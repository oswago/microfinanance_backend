// src/main/java/com/microfinance/loanapplications/service/ClientReportsServiceImpl.java
package com.microfinance.reports.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.borrower.entity.BorrowerGroup;
import com.microfinance.borrower.repository.BorrowerDocumentRepository;
import com.microfinance.borrower.repository.BorrowerGroupRepository;
import com.microfinance.borrower.repository.BorrowerRepository;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.BusinessException;
import com.microfinance.loanapplications.dto.report.*;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.repository.LoanRepository;
import com.microfinance.loanapplications.repository.RepaymentScheduleRepository;
import com.microfinance.common.config.DocumentConfig;
import com.microfinance.reports.entity.ReportConfiguration;
import com.microfinance.reports.repository.ReportConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientReportsServiceImpl implements ClientReportsService {

    private final BorrowerRepository borrowerRepository;
    private final BorrowerGroupRepository borrowerGroupRepository;
    private final BorrowerDocumentRepository borrowerDocumentRepository;
    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final ReportConfigurationRepository reportConfigurationRepository;


    @Override
    public ClientDemographicsReport getClientDemographics(Long branchId) {
        log.info("Generating client demographics report for branch: {}", branchId);

        List<Borrower> borrowers = branchId != null ?
                borrowerRepository.findByBranchId(branchId) :
                borrowerRepository.findAll();

        ClientDemographicsReport report = new ClientDemographicsReport();
        report.setTotalClients(borrowers.size());

        // Calculate client growth (compare with last month)
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        long previousMonthCount = borrowers.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isBefore(oneMonthAgo))
                .count();

        double clientGrowth = 0;
        if (previousMonthCount > 0) {
            clientGrowth = ((borrowers.size() - previousMonthCount) * 100.0) / previousMonthCount;
        }
        report.setClientGrowth(Math.round(clientGrowth * 10) / 10.0); // Round to 1 decimal place

        // Gender distribution
        Map<String, Integer> genderMap = new HashMap<>();
        Map<String, Integer> maritalMap = new HashMap<>();
        Map<String, Integer> occupationMap = new HashMap<>();
        Map<String, Integer> locationMap = new HashMap<>();

        ClientDemographicsReport.AgeGroups ageGroups = ClientDemographicsReport.AgeGroups.builder()
                .under25(0)
                .age25_35(0)
                .age36_50(0)
                .over50(0)
                .build();

        for (Borrower borrower : borrowers) {
            // Gender
            String gender = borrower.getGender() != null ? borrower.getGender().name() : "OTHER";
            genderMap.merge(gender, 1, Integer::sum);

            // Marital status
            String marital = borrower.getMaritalStatus() != null ? borrower.getMaritalStatus().name() : "SINGLE";
            maritalMap.merge(marital, 1, Integer::sum);

            // Occupation
            if (borrower.getOccupation() != null && !borrower.getOccupation().isEmpty()) {
                occupationMap.merge(borrower.getOccupation(), 1, Integer::sum);
            }

            // Location/Branch
            if (borrower.getBranch() != null && borrower.getBranch().getName() != null) {
                locationMap.merge(borrower.getBranch().getName(), 1, Integer::sum);
            }

            // Age groups
            if (borrower.getDateOfBirth() != null) {
                int age = LocalDate.now().getYear() - borrower.getDateOfBirth().getYear();
                if (age < 25) ageGroups.setUnder25(ageGroups.getUnder25() + 1);
                else if (age <= 35) ageGroups.setAge25_35(ageGroups.getAge25_35() + 1);
                else if (age <= 50) ageGroups.setAge36_50(ageGroups.getAge36_50() + 1);
                else ageGroups.setOver50(ageGroups.getOver50() + 1);
            }
        }

        report.setGenderDistribution(ClientDemographicsReport.GenderDistribution.builder()
                .male(genderMap.getOrDefault("MALE", 0))
                .female(genderMap.getOrDefault("FEMALE", 0))
                .other(genderMap.getOrDefault("OTHER", 0))
                .build());

        report.setMaritalStatusDistribution(ClientDemographicsReport.MaritalStatusDistribution.builder()
                .single(maritalMap.getOrDefault("SINGLE", 0))
                .married(maritalMap.getOrDefault("MARRIED", 0))
                .divorced(maritalMap.getOrDefault("DIVORCED", 0))
                .widowed(maritalMap.getOrDefault("WIDOWED", 0))
                .build());

        report.setAgeGroups(ageGroups);

        report.setOccupationBreakdown(occupationMap.entrySet().stream()
                .map(e -> ClientDemographicsReport.OccupationBreakdown.builder()
                        .occupation(e.getKey())
                        .count(e.getValue())
                        .build())
                .sorted((a, b) -> b.getCount().compareTo(a.getCount()))
                .limit(10)
                .collect(Collectors.toList()));

        report.setLocationBreakdown(locationMap.entrySet().stream()
                .map(e -> ClientDemographicsReport.LocationBreakdown.builder()
                        .branch(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList()));

        return report;
    }


    @Override
    public KycStatusReport getKycStatusReport(Long branchId) {
        log.info("Generating KYC status report for branch: {}", branchId);
        
        List<Borrower> borrowers = branchId != null ? 
            borrowerRepository.findByBranchId(branchId) : 
            borrowerRepository.findAll();
        
        KycStatusReport report = new KycStatusReport();
        report.setTotalClients(borrowers.size());
        
        Map<GeneralConfig.KycStatus, Integer> statusCounts = new HashMap<>();
        for (Borrower borrower : borrowers) {
            statusCounts.merge(borrower.getKycStatus(), 1, Integer::sum);
        }
        
        int total = borrowers.size();
        
        report.setVerified(createKycStatusCount(statusCounts, GeneralConfig.KycStatus.VERIFIED, total));
        report.setPending(createKycStatusCount(statusCounts, GeneralConfig.KycStatus.PENDING, total));
        report.setRejected(createKycStatusCount(statusCounts, GeneralConfig.KycStatus.REJECTED, total));
        report.setExpired(createKycStatusCount(statusCounts, GeneralConfig.KycStatus.EXPIRED, total));
        
        int notStarted = total - statusCounts.values().stream().mapToInt(Integer::intValue).sum();
        report.setNotStarted(KycStatusReport.KycStatusCount.builder()
            .count(notStarted)
            .percentage(total > 0 ? (notStarted * 100.0 / total) : 0)
            .build());
        
        // Pending documents by type
        List<BorrowerDocument> pendingDocs = borrowerDocumentRepository.findByStatus(DocumentConfig.DocumentStatus.PENDING);
        Map<String, Integer> pendingDocsByType = new HashMap<>();
        for (BorrowerDocument doc : pendingDocs) {
            if (branchId == null || (doc.getBorrower().getBranch() != null && 
                doc.getBorrower().getBranch().getId().equals(branchId))) {
                pendingDocsByType.merge(String.valueOf(doc.getDocumentType()), 1, Integer::sum);
            }
        }
        
        report.setPendingDocumentsByType(pendingDocsByType.entrySet().stream()
            .map(e -> KycStatusReport.PendingDocumentByType.builder()
                .documentType(e.getKey())
                .count(e.getValue())
                .build())
            .collect(Collectors.toList()));
        
        // KYC completion trend (last 30 days)
        List<KycStatusReport.KycTrend> trends = new ArrayList<>();
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        for (int i = 0; i < 30; i++) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            
            long verifiedCount = borrowers.stream()
                .filter(b -> b.getKycVerifiedAt() != null && 
                    b.getKycVerifiedAt().isAfter(dayStart) && 
                    b.getKycVerifiedAt().isBefore(dayEnd))
                .count();
            
            trends.add(KycStatusReport.KycTrend.builder()
                .date(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .verified((int) verifiedCount)
                .pending(0)
                .build());
        }
        report.setKycCompletionTrend(trends);
        
        return report;
    }

    @Override
    public PortfolioSummaryReport getPortfolioSummaryReport(Long branchId) {
        log.info("Generating portfolio summary report for branch: {}", branchId);
        
        List<Loan> activeLoans = branchId != null ?
            loanRepository.findByBranchIdAndStatus(branchId, GeneralConfig.LoanStatus.ACTIVE) :
            loanRepository.findByStatus(GeneralConfig.LoanStatus.ACTIVE);
        
        List<Loan> allLoans = branchId != null ?
            loanRepository.findByBranchId(branchId) :
            loanRepository.findAll();
        
        PortfolioSummaryReport report = new PortfolioSummaryReport();
        report.setActiveLoans(activeLoans.size());
        
        BigDecimal totalOutstanding = activeLoans.stream()
            .map(Loan::getOutstandingBalance)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setOutstandingBalance(totalOutstanding);
        
        BigDecimal totalPortfolio = allLoans.stream()
            .map(Loan::getPrincipalAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setTotalPortfolioValue(totalPortfolio);
        
        BigDecimal avgLoanSize = activeLoans.stream()
            .map(Loan::getPrincipalAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setAverageLoanSize(activeLoans.isEmpty() ? BigDecimal.ZERO : 
            avgLoanSize.divide(BigDecimal.valueOf(activeLoans.size()), 2, RoundingMode.HALF_UP));
        
        // Overdue loans
        List<Loan> overdueLoans = activeLoans.stream()
            .filter(l -> l.getDaysDelinquent() != null && l.getDaysDelinquent() > 0)
            .collect(Collectors.toList());
        report.setOverdueLoans(overdueLoans.size());
        
        BigDecimal overdueAmount = overdueLoans.stream()
            .map(Loan::getOutstandingBalance)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setOverdueAmount(overdueAmount);
        
        // PAR calculations
        PortfolioSummaryReport.ParDays parDays = PortfolioSummaryReport.ParDays.builder().build();
        
        BigDecimal totalOutstandingForPar = totalOutstanding.compareTo(BigDecimal.ZERO) > 0 ? totalOutstanding : BigDecimal.ONE;
        
        long par30Count = activeLoans.stream()
            .filter(l -> l.getDaysDelinquent() != null && l.getDaysDelinquent() >= 30)
            .count();
        BigDecimal par30Amount = activeLoans.stream()
            .filter(l -> l.getDaysDelinquent() != null && l.getDaysDelinquent() >= 30)
            .map(Loan::getOutstandingBalance)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        parDays.setPar30(par30Amount.multiply(BigDecimal.valueOf(100))
            .divide(totalOutstandingForPar, 2, RoundingMode.HALF_UP));
        
        long par60Count = activeLoans.stream()
            .filter(l -> l.getDaysDelinquent() != null && l.getDaysDelinquent() >= 60)
            .count();
        BigDecimal par60Amount = activeLoans.stream()
            .filter(l -> l.getDaysDelinquent() != null && l.getDaysDelinquent() >= 60)
            .map(Loan::getOutstandingBalance)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        parDays.setPar60(par60Amount.multiply(BigDecimal.valueOf(100))
            .divide(totalOutstandingForPar, 2, RoundingMode.HALF_UP));
        
        long par90Count = activeLoans.stream()
            .filter(l -> l.getDaysDelinquent() != null && l.getDaysDelinquent() >= 90)
            .count();
        BigDecimal par90Amount = activeLoans.stream()
            .filter(l -> l.getDaysDelinquent() != null && l.getDaysDelinquent() >= 90)
            .map(Loan::getOutstandingBalance)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        parDays.setPar90(par90Amount.multiply(BigDecimal.valueOf(100))
            .divide(totalOutstandingForPar, 2, RoundingMode.HALF_UP));
        
        report.setParDays(parDays);
        
        // Loan product distribution
        Map<String, Long> productCounts = new HashMap<>();
        Map<String, BigDecimal> productAmounts = new HashMap<>();
        
        for (Loan loan : allLoans) {
            String productName = loan.getLoanProduct() != null ? loan.getLoanProduct().getName() : "Unknown";
            productCounts.merge(productName, 1L, Long::sum);
            productAmounts.merge(productName, loan.getPrincipalAmount(), BigDecimal::add);
        }
        
        report.setLoanProductDistribution(productCounts.keySet().stream()
            .map(name -> PortfolioSummaryReport.LoanProductDistribution.builder()
                .productName(name)
                .loanCount(productCounts.get(name).intValue())
                .totalAmount(productAmounts.get(name))
                .build())
            .collect(Collectors.toList()));
        
        return report;
    }

    @Override
    public GroupPerformanceReport getGroupPerformanceReport(Long branchId) {
        log.info("Generating group performance report for branch: {}", branchId);
        
        List<BorrowerGroup> groups = branchId != null ?
            borrowerGroupRepository.findByBranchId(branchId) :
            borrowerGroupRepository.findAll();
        
        GroupPerformanceReport report = new GroupPerformanceReport();
        report.setTotalGroups(groups.size());
        
        long activeGroups = groups.stream()
            .filter(g -> "ACTIVE".equalsIgnoreCase(String.valueOf(g.getStatus())))
            .count();
        report.setActiveGroups((int) activeGroups);
        
        int totalMembers = groups.stream().mapToInt(g -> g.getMembers() != null ? g.getMembers().size() : 0).sum();
        report.setTotalMembers(totalMembers);
        report.setAverageGroupSize(groups.isEmpty() ? 0.0 : (double) totalMembers / groups.size());
        
        GroupPerformanceReport.GroupsByStatus groupsByStatus = GroupPerformanceReport.GroupsByStatus.builder()
            .active((int) groups.stream().filter(g -> "ACTIVE".equalsIgnoreCase(String.valueOf(g.getStatus()))).count())
            .inactive((int) groups.stream().filter(g -> "INACTIVE".equalsIgnoreCase(String.valueOf(g.getStatus()))).count())
            .suspended((int) groups.stream().filter(g -> "SUSPENDED".equalsIgnoreCase(String.valueOf(g.getStatus()))).count())
            .build();
        report.setGroupsByStatus(groupsByStatus);
        
        // Group loan performance (simplified - would need more complex queries in production)
        List<GroupPerformanceReport.GroupLoanPerformance> performances = new ArrayList<>();
        for (BorrowerGroup group : groups.stream().limit(10).collect(Collectors.toList())) {
            performances.add(GroupPerformanceReport.GroupLoanPerformance.builder()
                .groupName(group.getGroupName())
                .totalLoans(0)
                .activeLoans(0)
                .outstandingAmount(BigDecimal.ZERO)
                .repaymentRate(BigDecimal.valueOf(95.0))
                .build());
        }
        report.setGroupLoanPerformance(performances);
        
        return report;
    }

    @Override
    public ActivityReport getActivityReport(Long branchId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating activity report for branch: {}, period: {} to {}", branchId, startDate, endDate);
        
        ActivityReport report = new ActivityReport();
        report.setTotalActivities(0);
        report.setActivitiesByType(new ArrayList<>());
        report.setDailyActivityTrend(new ArrayList<>());
        report.setRecentActivities(new ArrayList<>());
        
        return report;
    }

    @Override
    public RiskAssessmentReport getRiskAssessmentReport(Long branchId) {
        log.info("Generating risk assessment report for branch: {}", branchId);
        
        List<Loan> activeLoans = branchId != null ?
            loanRepository.findByBranchIdAndStatus(branchId, GeneralConfig.LoanStatus.ACTIVE) :
            loanRepository.findByStatus(GeneralConfig.LoanStatus.ACTIVE);
        
        RiskAssessmentReport report = new RiskAssessmentReport();
        
        RiskAssessmentReport.RiskDistribution distribution = RiskAssessmentReport.RiskDistribution.builder()
            .low(0)
            .medium(0)
            .high(0)
            .critical(0)
            .build();
        
        for (Loan loan : activeLoans) {
            if (loan.getDaysDelinquent() == null || loan.getDaysDelinquent() == 0) {
                distribution.setLow(distribution.getLow() + 1);
            } else if (loan.getDaysDelinquent() <= 30) {
                distribution.setMedium(distribution.getMedium() + 1);
            } else if (loan.getDaysDelinquent() <= 90) {
                distribution.setHigh(distribution.getHigh() + 1);
            } else {
                distribution.setCritical(distribution.getCritical() + 1);
            }
        }
        
        report.setRiskDistribution(distribution);
        
        // High risk clients
        List<RiskAssessmentReport.HighRiskClient> highRiskClients = activeLoans.stream()
            .filter(l -> l.getDaysDelinquent() != null && l.getDaysDelinquent() > 90)
            .map(l -> RiskAssessmentReport.HighRiskClient.builder()
                .id(l.getBorrower().getId())
                .name(l.getBorrower().getFullName())
                .borrowerNumber(l.getBorrower().getBorrowerNumber())
                .riskRating("HIGH")
                .outstandingBalance(l.getOutstandingBalance())
                .daysOverdue(l.getDaysDelinquent())
                .build())
            .collect(Collectors.toList());
        report.setHighRiskClients(highRiskClients);
        
        report.setRiskFactors(new ArrayList<>());
        
        return report;
    }



        @Override
        public byte[] exportReport(String reportType, String format, Map<String, Object> params) {
            log.info("Exporting report: {} in format: {}", reportType, format);

            try {
                Long branchId = params.containsKey("branchId") ? (Long) params.get("branchId") : null;

                switch (reportType) {
                    case "demographics":
                        ClientDemographicsReport demographics = getClientDemographics(branchId);
                        if ("pdf".equalsIgnoreCase(format)) {
                            return generateDemographicsPDF(demographics);
                        } else if ("csv".equalsIgnoreCase(format)) {
                            return generateDemographicsCSV(demographics);
                        } else {
                            return generateDemographicsExcel(demographics);
                        }
                    case "kyc-status":
                        KycStatusReport kycStatus = getKycStatusReport(branchId);
                        if ("pdf".equalsIgnoreCase(format)) {
                            return generateKycStatusPDF(kycStatus);
                        } else if ("csv".equalsIgnoreCase(format)) {
                            return generateKycStatusCSV(kycStatus);
                        } else {
                            return generateKycStatusExcel(kycStatus);
                        }
                    case "portfolio":
                        PortfolioSummaryReport portfolio = getPortfolioSummaryReport(branchId);
                        if ("pdf".equalsIgnoreCase(format)) {
                            return generatePortfolioPDF(portfolio);
                        } else if ("csv".equalsIgnoreCase(format)) {
                            return generatePortfolioCSV(portfolio);
                        } else {
                            return generatePortfolioExcel(portfolio);
                        }
                    case "group-performance":
                        GroupPerformanceReport group = getGroupPerformanceReport(branchId);
                        if ("pdf".equalsIgnoreCase(format)) {
                            return generateGroupPerformancePDF(group);
                        } else if ("csv".equalsIgnoreCase(format)) {
                            return generateGroupPerformanceCSV(group);
                        } else {
                            return generateGroupPerformanceExcel(group);
                        }
                    case "activity":
                        LocalDate startDate = params.containsKey("startDate") ? (LocalDate) params.get("startDate") : LocalDate.now().minusDays(30);
                        LocalDate endDate = params.containsKey("endDate") ? (LocalDate) params.get("endDate") : LocalDate.now();
                        ActivityReport activity = getActivityReport(branchId, startDate, endDate);
                        if ("pdf".equalsIgnoreCase(format)) {
                            return generateActivityPDF(activity);
                        } else if ("csv".equalsIgnoreCase(format)) {
                            return generateActivityCSV(activity);
                        } else {
                            return generateActivityExcel(activity);
                        }
                    case "risk":
                        RiskAssessmentReport risk = getRiskAssessmentReport(branchId);
                        if ("pdf".equalsIgnoreCase(format)) {
                            return generateRiskAssessmentPDF(risk);
                        } else if ("csv".equalsIgnoreCase(format)) {
                            return generateRiskAssessmentCSV(risk);
                        } else {
                            return generateRiskAssessmentExcel(risk);
                        }
                    default:
                        throw new IllegalArgumentException("Unknown report type: " + reportType);
                }
            } catch (Exception e) {
                log.error("Error generating report: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to generate report: " + e.getMessage());
            }
        }

        // ==================== PDF GENERATION USING iTEXT 7 ====================

        private byte[] generateDemographicsPDF(ClientDemographicsReport report) throws Exception {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc, PageSize.A4);
                document.setMargins(50, 50, 50, 50);

                // Colors
                DeviceRgb primaryColor = new DeviceRgb(102, 126, 234);
                DeviceRgb secondaryColor = new DeviceRgb(118, 75, 162);
                DeviceRgb headerBgColor = new DeviceRgb(248, 249, 250);

                // Fonts
                PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");
                PdfFont regularFont = PdfFontFactory.createFont("Helvetica");

                // Title
                Paragraph title = new Paragraph("CLIENT DEMOGRAPHICS REPORT")
                        .setFont(boldFont)
                        .setFontSize(20)
                        .setFontColor(primaryColor)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(10);
                document.add(title);

                // Subtitle
                Paragraph subtitle = new Paragraph("Generated on: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")))
                        .setFont(regularFont)
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(30);
                document.add(subtitle);

                // Summary Section
                Paragraph summaryHeader = new Paragraph("SUMMARY")
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setFontColor(primaryColor)
                        .setMarginBottom(10);
                document.add(summaryHeader);

                Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}));
                summaryTable.setWidth(UnitValue.createPercentValue(100));
                summaryTable.setMarginBottom(20);

                addTableRow(summaryTable, "Total Clients", String.valueOf(report.getTotalClients()), boldFont, regularFont);
                addTableRow(summaryTable, "Client Growth", report.getClientGrowth() + "%", boldFont, regularFont);
                document.add(summaryTable);

                // Gender Distribution
                Paragraph genderHeader = new Paragraph("GENDER DISTRIBUTION")
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setFontColor(primaryColor)
                        .setMarginBottom(10);
                document.add(genderHeader);

                Table genderTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}));
                genderTable.setWidth(UnitValue.createPercentValue(100));
                genderTable.setMarginBottom(20);

                addTableRow(genderTable, "Male", String.valueOf(report.getGenderDistribution().getMale()), boldFont, regularFont);
                addTableRow(genderTable, "Female", String.valueOf(report.getGenderDistribution().getFemale()), boldFont, regularFont);
                document.add(genderTable);

                // Age Distribution
                Paragraph ageHeader = new Paragraph("AGE DISTRIBUTION")
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setFontColor(primaryColor)
                        .setMarginBottom(10);
                document.add(ageHeader);

                Table ageTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}));
                ageTable.setWidth(UnitValue.createPercentValue(100));
                ageTable.setMarginBottom(20);

                addTableRow(ageTable, "Under 25", String.valueOf(report.getAgeGroups().getUnder25()), boldFont, regularFont);
                addTableRow(ageTable, "25-35", String.valueOf(report.getAgeGroups().getAge25_35()), boldFont, regularFont);
                addTableRow(ageTable, "36-50", String.valueOf(report.getAgeGroups().getAge36_50()), boldFont, regularFont);
                addTableRow(ageTable, "Over 50", String.valueOf(report.getAgeGroups().getOver50()), boldFont, regularFont);
                document.add(ageTable);

                // Marital Status
                Paragraph maritalHeader = new Paragraph("MARITAL STATUS")
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setFontColor(primaryColor)
                        .setMarginBottom(10);
                document.add(maritalHeader);

                Table maritalTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}));
                maritalTable.setWidth(UnitValue.createPercentValue(100));
                maritalTable.setMarginBottom(20);

                addTableRow(maritalTable, "Single", String.valueOf(report.getMaritalStatusDistribution().getSingle()), boldFont, regularFont);
                addTableRow(maritalTable, "Married", String.valueOf(report.getMaritalStatusDistribution().getMarried()), boldFont, regularFont);
                addTableRow(maritalTable, "Divorced", String.valueOf(report.getMaritalStatusDistribution().getDivorced()), boldFont, regularFont);
                addTableRow(maritalTable, "Widowed", String.valueOf(report.getMaritalStatusDistribution().getWidowed()), boldFont, regularFont);
                document.add(maritalTable);

                // Footer
                Paragraph footer = new Paragraph("This report was generated automatically by the Microfinance System")
                        .setFont(regularFont)
                        .setFontSize(8)
                        .setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(30);
                document.add(footer);

                document.close();
                return baos.toByteArray();
            }
        }

        private byte[] generateKycStatusPDF(KycStatusReport report) throws Exception {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc, PageSize.A4);
                document.setMargins(50, 50, 50, 50);

                PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");
                PdfFont regularFont = PdfFontFactory.createFont("Helvetica");
                DeviceRgb primaryColor = new DeviceRgb(102, 126, 234);

                // Title
                Paragraph title = new Paragraph("KYC STATUS REPORT")
                        .setFont(boldFont)
                        .setFontSize(20)
                        .setFontColor(primaryColor)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(10);
                document.add(title);

                Paragraph subtitle = new Paragraph("Generated on: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")))
                        .setFont(regularFont)
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(30);
                document.add(subtitle);

                // KYC Status Summary
                Paragraph statusHeader = new Paragraph("KYC STATUS SUMMARY")
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setFontColor(primaryColor)
                        .setMarginBottom(10);
                document.add(statusHeader);

                Table statusTable = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34}));
                statusTable.setWidth(UnitValue.createPercentValue(100));
                statusTable.setMarginBottom(20);

                // Header row
                addHeaderRow(statusTable, new String[]{"Status", "Count", "Percentage"}, boldFont);

                // Data rows
                addTableRow(statusTable, "Verified", String.valueOf(report.getVerified().getCount()), report.getVerified().getPercentage() + "%", regularFont);
                addTableRow(statusTable, "Pending", String.valueOf(report.getPending().getCount()), report.getPending().getPercentage() + "%", regularFont);
                addTableRow(statusTable, "Rejected", String.valueOf(report.getRejected().getCount()), report.getRejected().getPercentage() + "%", regularFont);
                addTableRow(statusTable, "Expired", String.valueOf(report.getExpired().getCount()), report.getExpired().getPercentage() + "%", regularFont);
                document.add(statusTable);

                // Pending Documents
                if (report.getPendingDocumentsByType() != null && !report.getPendingDocumentsByType().isEmpty()) {
                    Paragraph pendingHeader = new Paragraph("PENDING DOCUMENTS BY TYPE")
                            .setFont(boldFont)
                            .setFontSize(14)
                            .setFontColor(primaryColor)
                            .setMarginBottom(10);
                    document.add(pendingHeader);

                    Table pendingTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
                    pendingTable.setWidth(UnitValue.createPercentValue(100));
                    pendingTable.setMarginBottom(20);

                    addHeaderRow(pendingTable, new String[]{"Document Type", "Count"}, boldFont);

                    for (KycStatusReport.PendingDocumentByType doc : report.getPendingDocumentsByType()) {
                        addTableRow(pendingTable, doc.getDocumentType(), String.valueOf(doc.getCount()), regularFont);

                    }
                    document.add(pendingTable);
                }

                document.close();
                return baos.toByteArray();
            }
        }

        private byte[] generatePortfolioPDF(PortfolioSummaryReport report) throws Exception {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc, PageSize.A4);
                document.setMargins(50, 50, 50, 50);

                PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");
                PdfFont regularFont = PdfFontFactory.createFont("Helvetica");
                DeviceRgb primaryColor = new DeviceRgb(30, 60, 114);

                // Title
                Paragraph title = new Paragraph("PORTFOLIO SUMMARY REPORT")
                        .setFont(boldFont)
                        .setFontSize(20)
                        .setFontColor(primaryColor)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(10);
                document.add(title);

                Paragraph subtitle = new Paragraph("Generated on: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")))
                        .setFont(regularFont)
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(30);
                document.add(subtitle);

                // Portfolio Summary
                Paragraph portfolioHeader = new Paragraph("PORTFOLIO SUMMARY")
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setFontColor(primaryColor)
                        .setMarginBottom(10);
                document.add(portfolioHeader);

                Table portfolioTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}));
                portfolioTable.setWidth(UnitValue.createPercentValue(100));
                portfolioTable.setMarginBottom(20);

                addTableRow(portfolioTable, "Total Portfolio Value", formatCurrency(report.getTotalPortfolioValue()), boldFont, regularFont);
                addTableRow(portfolioTable, "Active Loans", String.valueOf(report.getActiveLoans()), boldFont, regularFont);
                addTableRow(portfolioTable, "Average Loan Size", formatCurrency(report.getAverageLoanSize()), boldFont, regularFont);
                addTableRow(portfolioTable, "Outstanding Balance", formatCurrency(report.getOutstandingBalance()), boldFont, regularFont);
                addTableRow(portfolioTable, "Overdue Loans", String.valueOf(report.getOverdueLoans()), boldFont, regularFont);
                document.add(portfolioTable);

                // Portfolio at Risk
                Paragraph parHeader = new Paragraph("PORTFOLIO AT RISK (PAR)")
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setFontColor(primaryColor)
                        .setMarginBottom(10);
                document.add(parHeader);

                Table parTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
                parTable.setWidth(UnitValue.createPercentValue(100));
                parTable.setMarginBottom(20);

                addTableRow(parTable, "PAR 30", report.getParDays().getPar30() + "%", boldFont, regularFont);
                addTableRow(parTable, "PAR 60", report.getParDays().getPar60() + "%", boldFont, regularFont);
                addTableRow(parTable, "PAR 90", report.getParDays().getPar90() + "%", boldFont, regularFont);
                document.add(parTable);

                // Loan Product Distribution
                if (report.getLoanProductDistribution() != null && !report.getLoanProductDistribution().isEmpty()) {
                    Paragraph productHeader = new Paragraph("LOAN PRODUCT DISTRIBUTION")
                            .setFont(boldFont)
                            .setFontSize(14)
                            .setFontColor(primaryColor)
                            .setMarginBottom(10);
                    document.add(productHeader);

                    Table productTable = new Table(UnitValue.createPercentArray(new float[]{40, 30, 30}));
                    productTable.setWidth(UnitValue.createPercentValue(100));
                    productTable.setMarginBottom(20);

                    addHeaderRow(productTable, new String[]{"Product Name", "Loan Count", "Total Amount"}, boldFont);

                    for (PortfolioSummaryReport.LoanProductDistribution product : report.getLoanProductDistribution()) {
                        addTableRow(productTable, product.getProductName(),
                                String.valueOf(product.getLoanCount()),
                                formatCurrency(product.getTotalAmount()), regularFont);
                    }
                    document.add(productTable);
                }

                document.close();
                return baos.toByteArray();
            }
        }

        private byte[] generateRiskAssessmentPDF(RiskAssessmentReport report) throws Exception {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc, PageSize.A4);
                document.setMargins(50, 50, 50, 50);

                PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");
                PdfFont regularFont = PdfFontFactory.createFont("Helvetica");
                DeviceRgb primaryColor = new DeviceRgb(203, 45, 62);

                // Title
                Paragraph title = new Paragraph("RISK ASSESSMENT REPORT")
                        .setFont(boldFont)
                        .setFontSize(20)
                        .setFontColor(primaryColor)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(10);
                document.add(title);

                Paragraph subtitle = new Paragraph("Generated on: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")))
                        .setFont(regularFont)
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(30);
                document.add(subtitle);

                // Risk Distribution
                Paragraph riskHeader = new Paragraph("RISK DISTRIBUTION")
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setFontColor(primaryColor)
                        .setMarginBottom(10);
                document.add(riskHeader);

                Table riskTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}));
                riskTable.setWidth(UnitValue.createPercentValue(100));
                riskTable.setMarginBottom(20);

                addTableRow(riskTable, "Low Risk", String.valueOf(report.getRiskDistribution().getLow()), boldFont, regularFont);
                addTableRow(riskTable, "Medium Risk", String.valueOf(report.getRiskDistribution().getMedium()), boldFont, regularFont);
                addTableRow(riskTable, "High Risk", String.valueOf(report.getRiskDistribution().getHigh()), boldFont, regularFont);
                addTableRow(riskTable, "Critical Risk", String.valueOf(report.getRiskDistribution().getCritical()), boldFont, regularFont);
                document.add(riskTable);

                // High Risk Clients
                if (report.getHighRiskClients() != null && !report.getHighRiskClients().isEmpty()) {
                    Paragraph highRiskHeader = new Paragraph("HIGH RISK CLIENTS")
                            .setFont(boldFont)
                            .setFontSize(14)
                            .setFontColor(primaryColor)
                            .setMarginBottom(10);
                    document.add(highRiskHeader);

                    Table clientTable = new Table(UnitValue.createPercentArray(new float[]{35, 20, 25, 20}));
                    clientTable.setWidth(UnitValue.createPercentValue(100));
                    clientTable.setMarginBottom(20);

                    addHeaderRow(clientTable, new String[]{"Client Name", "Client Number", "Outstanding Balance", "Days Overdue"}, boldFont);

                    for (RiskAssessmentReport.HighRiskClient client : report.getHighRiskClients()) {
                        addTableRow(clientTable, client.getName(), client.getBorrowerNumber(),
                                formatCurrency(client.getOutstandingBalance()), client.getDaysOverdue() + " days", regularFont);
                    }
                    document.add(clientTable);
                }

                document.close();
                return baos.toByteArray();
            }
        }

        private byte[] generateGroupPerformancePDF(GroupPerformanceReport report) throws Exception {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc, PageSize.A4);
                document.setMargins(50, 50, 50, 50);

                PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");
                PdfFont regularFont = PdfFontFactory.createFont("Helvetica");
                DeviceRgb primaryColor = new DeviceRgb(17, 153, 142);

                // Title
                Paragraph title = new Paragraph("GROUP PERFORMANCE REPORT")
                        .setFont(boldFont)
                        .setFontSize(20)
                        .setFontColor(primaryColor)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(10);
                document.add(title);

                Paragraph subtitle = new Paragraph("Generated on: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")))
                        .setFont(regularFont)
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(30);
                document.add(subtitle);

                // Group Summary
                Table groupTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}));
                groupTable.setWidth(UnitValue.createPercentValue(100));
                groupTable.setMarginBottom(20);

                addTableRow(groupTable, "Total Groups", String.valueOf(report.getTotalGroups()), boldFont, regularFont);
                addTableRow(groupTable, "Active Groups", String.valueOf(report.getActiveGroups()), boldFont, regularFont);
                addTableRow(groupTable, "Total Members", String.valueOf(report.getTotalMembers()), boldFont, regularFont);
                addTableRow(groupTable, "Average Group Size", String.valueOf(report.getAverageGroupSize()), boldFont, regularFont);
                document.add(groupTable);

                document.close();
                return baos.toByteArray();
            }
        }

        private byte[] generateActivityPDF(ActivityReport report) throws Exception {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc, PageSize.A4);
                document.setMargins(50, 50, 50, 50);

                PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");
                PdfFont regularFont = PdfFontFactory.createFont("Helvetica");
                DeviceRgb primaryColor = new DeviceRgb(240, 147, 251);

                // Title
                Paragraph title = new Paragraph("ACTIVITY REPORT")
                        .setFont(boldFont)
                        .setFontSize(20)
                        .setFontColor(primaryColor)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(10);
                document.add(title);

                Paragraph subtitle = new Paragraph("Generated on: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")))
                        .setFont(regularFont)
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(30);
                document.add(subtitle);

                // Activity Summary
                Table activityTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}));
                activityTable.setWidth(UnitValue.createPercentValue(100));
                activityTable.setMarginBottom(20);

                addTableRow(activityTable, "Total Activities", String.valueOf(report.getTotalActivities()), boldFont, regularFont);
                document.add(activityTable);

                // Recent Activities
                if (report.getRecentActivities() != null && !report.getRecentActivities().isEmpty()) {
                    Paragraph recentHeader = new Paragraph("RECENT ACTIVITIES")
                            .setFont(boldFont)
                            .setFontSize(14)
                            .setFontColor(primaryColor)
                            .setMarginBottom(10);
                    document.add(recentHeader);

                    Table recentTable = new Table(UnitValue.createPercentArray(new float[]{25, 50, 25}));
                    recentTable.setWidth(UnitValue.createPercentValue(100));
                    recentTable.setMarginBottom(20);

                    addHeaderRow(recentTable, new String[]{"Type", "Description", "Date"}, boldFont);

                    for (ActivityReport.RecentActivity activity : report.getRecentActivities()) {
                        addTableRow(recentTable, activity.getType(), activity.getDescription(),
                                formatDate(activity.getTimestamp()), regularFont);
                    }
                    document.add(recentTable);
                }

                document.close();
                return baos.toByteArray();
            }
        }

        // ==================== CSV GENERATION ====================

        private byte[] generateDemographicsCSV(ClientDemographicsReport report) throws Exception {
            StringBuilder csv = new StringBuilder();
            csv.append("Metric,Value\n");
            csv.append("Total Clients,").append(report.getTotalClients()).append("\n");
            csv.append("Client Growth,").append(report.getClientGrowth()).append("%\n");
            csv.append("\nGender Distribution,\n");
            csv.append("Male,").append(report.getGenderDistribution().getMale()).append("\n");
            csv.append("Female,").append(report.getGenderDistribution().getFemale()).append("\n");
            csv.append("\nAge Distribution,\n");
            csv.append("Under 25,").append(report.getAgeGroups().getUnder25()).append("\n");
            csv.append("25-35,").append(report.getAgeGroups().getAge25_35()).append("\n");
            csv.append("36-50,").append(report.getAgeGroups().getAge36_50()).append("\n");
            csv.append("Over 50,").append(report.getAgeGroups().getOver50()).append("\n");
            csv.append("\nMarital Status,\n");
            csv.append("Single,").append(report.getMaritalStatusDistribution().getSingle()).append("\n");
            csv.append("Married,").append(report.getMaritalStatusDistribution().getMarried()).append("\n");
            csv.append("Divorced,").append(report.getMaritalStatusDistribution().getDivorced()).append("\n");
            csv.append("Widowed,").append(report.getMaritalStatusDistribution().getWidowed()).append("\n");

            return csv.toString().getBytes();
        }

        private byte[] generateKycStatusCSV(KycStatusReport report) throws Exception {
            StringBuilder csv = new StringBuilder();
            csv.append("Status,Count,Percentage\n");
            csv.append("Verified,").append(report.getVerified().getCount()).append(",").append(report.getVerified().getPercentage()).append("%\n");
            csv.append("Pending,").append(report.getPending().getCount()).append(",").append(report.getPending().getPercentage()).append("%\n");
            csv.append("Rejected,").append(report.getRejected().getCount()).append(",").append(report.getRejected().getPercentage()).append("%\n");
            csv.append("Expired,").append(report.getExpired().getCount()).append(",").append(report.getExpired().getPercentage()).append("%\n");

            if (report.getPendingDocumentsByType() != null && !report.getPendingDocumentsByType().isEmpty()) {
                csv.append("\nPending Documents by Type,\n");
                csv.append("Document Type,Count\n");
                for (KycStatusReport.PendingDocumentByType doc : report.getPendingDocumentsByType()) {
                    csv.append(doc.getDocumentType()).append(",").append(doc.getCount()).append("\n");
                }
            }

            return csv.toString().getBytes();
        }

        private byte[] generatePortfolioCSV(PortfolioSummaryReport report) throws Exception {
            StringBuilder csv = new StringBuilder();
            csv.append("Metric,Value\n");
            csv.append("Total Portfolio Value,").append(formatCurrency(report.getTotalPortfolioValue())).append("\n");
            csv.append("Active Loans,").append(report.getActiveLoans()).append("\n");
            csv.append("Average Loan Size,").append(formatCurrency(report.getAverageLoanSize())).append("\n");
            csv.append("Outstanding Balance,").append(formatCurrency(report.getOutstandingBalance())).append("\n");
            csv.append("Overdue Loans,").append(report.getOverdueLoans()).append("\n");
            csv.append("\nPortfolio at Risk (PAR),\n");
            csv.append("PAR 30,").append(report.getParDays().getPar30()).append("%\n");
            csv.append("PAR 60,").append(report.getParDays().getPar60()).append("%\n");
            csv.append("PAR 90,").append(report.getParDays().getPar90()).append("%\n");

            if (report.getLoanProductDistribution() != null && !report.getLoanProductDistribution().isEmpty()) {
                csv.append("\nLoan Product Distribution,\n");
                csv.append("Product Name,Loan Count,Total Amount\n");
                for (PortfolioSummaryReport.LoanProductDistribution product : report.getLoanProductDistribution()) {
                    csv.append(product.getProductName()).append(",")
                            .append(product.getLoanCount()).append(",")
                            .append(formatCurrency(product.getTotalAmount())).append("\n");
                }
            }

            return csv.toString().getBytes();
        }

        private byte[] generateRiskAssessmentCSV(RiskAssessmentReport report) throws Exception {
            StringBuilder csv = new StringBuilder();
            csv.append("Risk Level,Count\n");
            csv.append("Low Risk,").append(report.getRiskDistribution().getLow()).append("\n");
            csv.append("Medium Risk,").append(report.getRiskDistribution().getMedium()).append("\n");
            csv.append("High Risk,").append(report.getRiskDistribution().getHigh()).append("\n");
            csv.append("Critical Risk,").append(report.getRiskDistribution().getCritical()).append("\n");

            if (report.getHighRiskClients() != null && !report.getHighRiskClients().isEmpty()) {
                csv.append("\nHigh Risk Clients,\n");
                csv.append("Client Name,Client Number,Outstanding Balance,Days Overdue\n");
                for (RiskAssessmentReport.HighRiskClient client : report.getHighRiskClients()) {
                    csv.append(client.getName()).append(",")
                            .append(client.getBorrowerNumber()).append(",")
                            .append(formatCurrency(client.getOutstandingBalance())).append(",")
                            .append(client.getDaysOverdue()).append("\n");
                }
            }

            return csv.toString().getBytes();
        }

        private byte[] generateGroupPerformanceCSV(GroupPerformanceReport report) throws Exception {
            StringBuilder csv = new StringBuilder();
            csv.append("Metric,Value\n");
            csv.append("Total Groups,").append(report.getTotalGroups()).append("\n");
            csv.append("Active Groups,").append(report.getActiveGroups()).append("\n");
            csv.append("Total Members,").append(report.getTotalMembers()).append("\n");
            csv.append("Average Group Size,").append(report.getAverageGroupSize()).append("\n");

            return csv.toString().getBytes();
        }

        private byte[] generateActivityCSV(ActivityReport report) throws Exception {
            StringBuilder csv = new StringBuilder();
            csv.append("Metric,Value\n");
            csv.append("Total Activities,").append(report.getTotalActivities()).append("\n");

            if (report.getRecentActivities() != null && !report.getRecentActivities().isEmpty()) {
                csv.append("\nRecent Activities,\n");
                csv.append("Type,Description,Date\n");
                for (ActivityReport.RecentActivity activity : report.getRecentActivities()) {
                    csv.append(activity.getType()).append(",")
                            .append(activity.getDescription()).append(",")
                            .append(formatDate(activity.getTimestamp())).append("\n");
                }
            }

            return csv.toString().getBytes();
        }

        // ==================== EXCEL GENERATION ====================

        private byte[] generateDemographicsExcel(ClientDemographicsReport report) throws Exception {
            try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("Demographics Report");
                int rowNum = 0;

                // Title
                Row titleRow = sheet.createRow(rowNum++);
                org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("CLIENT DEMOGRAPHICS REPORT");
                CellStyle titleStyle = workbook.createCellStyle();
                Font titleFont = workbook.createFont();
                titleFont.setBold(true);
                titleFont.setFontHeightInPoints((short) 16);
                titleStyle.setFont(titleFont);
                titleCell.setCellStyle(titleStyle);

                rowNum++; // Empty row

                // Summary
                rowNum = addExcelRow(sheet, rowNum, "Total Clients", String.valueOf(report.getTotalClients()));
                rowNum = addExcelRow(sheet, rowNum, "Client Growth", report.getClientGrowth() + "%");
                rowNum++; // Empty row

                // Gender Distribution
                rowNum = addExcelHeader(sheet, rowNum, "GENDER DISTRIBUTION");
                rowNum = addExcelRow(sheet, rowNum, "Male", String.valueOf(report.getGenderDistribution().getMale()));
                rowNum = addExcelRow(sheet, rowNum, "Female", String.valueOf(report.getGenderDistribution().getFemale()));
                rowNum++; // Empty row

                // Age Distribution
                rowNum = addExcelHeader(sheet, rowNum, "AGE DISTRIBUTION");
                rowNum = addExcelRow(sheet, rowNum, "Under 25", String.valueOf(report.getAgeGroups().getUnder25()));
                rowNum = addExcelRow(sheet, rowNum, "25-35", String.valueOf(report.getAgeGroups().getAge25_35()));
                rowNum = addExcelRow(sheet, rowNum, "36-50", String.valueOf(report.getAgeGroups().getAge36_50()));
                rowNum = addExcelRow(sheet, rowNum, "Over 50", String.valueOf(report.getAgeGroups().getOver50()));

                // Auto-size columns
                for (int i = 0; i < 2; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(baos);
                return baos.toByteArray();
            }
        }

        private byte[] generateKycStatusExcel(KycStatusReport report) throws Exception {
            try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("KYC Status Report");
                int rowNum = 0;

                // Title
                Row titleRow = sheet.createRow(rowNum++);
                titleRow.createCell(0).setCellValue("KYC STATUS REPORT");

                rowNum++; // Empty row

                // Status Summary
                rowNum = addExcelHeader(sheet, rowNum, "STATUS SUMMARY");
                rowNum = addExcelRow(sheet, rowNum, "Verified", report.getVerified().getCount() + " (" + report.getVerified().getPercentage() + "%)");
                rowNum = addExcelRow(sheet, rowNum, "Pending", report.getPending().getCount() + " (" + report.getPending().getPercentage() + "%)");
                rowNum = addExcelRow(sheet, rowNum, "Rejected", report.getRejected().getCount() + " (" + report.getRejected().getPercentage() + "%)");
                rowNum = addExcelRow(sheet, rowNum, "Expired", report.getExpired().getCount() + " (" + report.getExpired().getPercentage() + "%)");

                // Pending Documents
                if (report.getPendingDocumentsByType() != null && !report.getPendingDocumentsByType().isEmpty()) {
                    rowNum++; // Empty row
                    rowNum = addExcelHeader(sheet, rowNum, "PENDING DOCUMENTS BY TYPE");
                    for (KycStatusReport.PendingDocumentByType doc : report.getPendingDocumentsByType()) {
                        rowNum = addExcelRow(sheet, rowNum, doc.getDocumentType(), String.valueOf(doc.getCount()));
                    }
                }

                // Auto-size columns
                for (int i = 0; i < 2; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(baos);
                return baos.toByteArray();
            }
        }

        private byte[] generatePortfolioExcel(PortfolioSummaryReport report) throws Exception {
            try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("Portfolio Report");
                int rowNum = 0;

                // Title
                Row titleRow = sheet.createRow(rowNum++);
                titleRow.createCell(0).setCellValue("PORTFOLIO SUMMARY REPORT");

                rowNum++; // Empty row

                // Portfolio Summary
                rowNum = addExcelHeader(sheet, rowNum, "PORTFOLIO SUMMARY");
                rowNum = addExcelRow(sheet, rowNum, "Total Portfolio Value", formatCurrency(report.getTotalPortfolioValue()));
                rowNum = addExcelRow(sheet, rowNum, "Active Loans", String.valueOf(report.getActiveLoans()));
                rowNum = addExcelRow(sheet, rowNum, "Average Loan Size", formatCurrency(report.getAverageLoanSize()));
                rowNum = addExcelRow(sheet, rowNum, "Outstanding Balance", formatCurrency(report.getOutstandingBalance()));
                rowNum = addExcelRow(sheet, rowNum, "Overdue Loans", String.valueOf(report.getOverdueLoans()));

                rowNum++; // Empty row
                rowNum = addExcelHeader(sheet, rowNum, "PORTFOLIO AT RISK (PAR)");
                rowNum = addExcelRow(sheet, rowNum, "PAR 30", report.getParDays().getPar30() + "%");
                rowNum = addExcelRow(sheet, rowNum, "PAR 60", report.getParDays().getPar60() + "%");
                rowNum = addExcelRow(sheet, rowNum, "PAR 90", report.getParDays().getPar90() + "%");

                // Auto-size columns
                for (int i = 0; i < 2; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(baos);
                return baos.toByteArray();
            }
        }

        private byte[] generateRiskAssessmentExcel(RiskAssessmentReport report) throws Exception {
            try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("Risk Assessment");
                int rowNum = 0;

                // Title
                Row titleRow = sheet.createRow(rowNum++);
                titleRow.createCell(0).setCellValue("RISK ASSESSMENT REPORT");

                rowNum++; // Empty row

                // Risk Distribution
                rowNum = addExcelHeader(sheet, rowNum, "RISK DISTRIBUTION");
                rowNum = addExcelRow(sheet, rowNum, "Low Risk", String.valueOf(report.getRiskDistribution().getLow()));
                rowNum = addExcelRow(sheet, rowNum, "Medium Risk", String.valueOf(report.getRiskDistribution().getMedium()));
                rowNum = addExcelRow(sheet, rowNum, "High Risk", String.valueOf(report.getRiskDistribution().getHigh()));
                rowNum = addExcelRow(sheet, rowNum, "Critical Risk", String.valueOf(report.getRiskDistribution().getCritical()));

                // Auto-size columns
                for (int i = 0; i < 2; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(baos);
                return baos.toByteArray();
            }
        }

        private byte[] generateGroupPerformanceExcel(GroupPerformanceReport report) throws Exception {
            try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("Group Performance");
                int rowNum = 0;

                Row titleRow = sheet.createRow(rowNum++);
                titleRow.createCell(0).setCellValue("GROUP PERFORMANCE REPORT");

                rowNum++; // Empty row

                rowNum = addExcelHeader(sheet, rowNum, "GROUP SUMMARY");
                rowNum = addExcelRow(sheet, rowNum, "Total Groups", String.valueOf(report.getTotalGroups()));
                rowNum = addExcelRow(sheet, rowNum, "Active Groups", String.valueOf(report.getActiveGroups()));
                rowNum = addExcelRow(sheet, rowNum, "Total Members", String.valueOf(report.getTotalMembers()));
                rowNum = addExcelRow(sheet, rowNum, "Average Group Size", String.valueOf(report.getAverageGroupSize()));

                for (int i = 0; i < 2; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(baos);
                return baos.toByteArray();
            }
        }

        private byte[] generateActivityExcel(ActivityReport report) throws Exception {
            try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("Activity Report");
                int rowNum = 0;

                Row titleRow = sheet.createRow(rowNum++);
                titleRow.createCell(0).setCellValue("ACTIVITY REPORT");

                rowNum++; // Empty row

                rowNum = addExcelRow(sheet, rowNum, "Total Activities", String.valueOf(report.getTotalActivities()));

                if (report.getRecentActivities() != null && !report.getRecentActivities().isEmpty()) {
                    rowNum++; // Empty row
                    rowNum = addExcelHeader(sheet, rowNum, "RECENT ACTIVITIES");
                    for (ActivityReport.RecentActivity activity : report.getRecentActivities()) {
                        rowNum = addExcelRow(sheet, rowNum, activity.getType(), activity.getDescription() + " (" + formatDate(activity.getTimestamp()) + ")");
                    }
                }

                for (int i = 0; i < 2; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(baos);
                return baos.toByteArray();
            }
        }

        // ==================== HELPER METHODS ====================

        private void addTableRow(Table table, String label, String value, PdfFont boldFont, PdfFont regularFont) {
            Cell labelCell = new Cell().add(new Paragraph(label).setFont(boldFont).setFontSize(10));
            Cell valueCell = new Cell().add(new Paragraph(value).setFont(regularFont).setFontSize(10));
            table.addCell(labelCell);
            table.addCell(valueCell);
        }

    // Add this missing method for 2-column tables (like pending documents)
    private void addTableRow(Table table, String col1, String col2, PdfFont regularFont) {
        Cell cell1 = new Cell().add(new Paragraph(col1).setFont(regularFont).setFontSize(10));
        Cell cell2 = new Cell().add(new Paragraph(col2).setFont(regularFont).setFontSize(10));
        table.addCell(cell1);
        table.addCell(cell2);
    }

        private void addTableRow(Table table, String col1, String col2, String col3, PdfFont regularFont) {
            table.addCell(new Cell().add(new Paragraph(col1).setFont(regularFont).setFontSize(10)));
            table.addCell(new Cell().add(new Paragraph(col2).setFont(regularFont).setFontSize(10)));
            table.addCell(new Cell().add(new Paragraph(col3).setFont(regularFont).setFontSize(10)));
        }

        private void addTableRow(Table table, String col1, String col2, String col3, String col4, PdfFont regularFont) {
            table.addCell(new Cell().add(new Paragraph(col1).setFont(regularFont).setFontSize(10)));
            table.addCell(new Cell().add(new Paragraph(col2).setFont(regularFont).setFontSize(10)));
            table.addCell(new Cell().add(new Paragraph(col3).setFont(regularFont).setFontSize(10)));
            table.addCell(new Cell().add(new Paragraph(col4).setFont(regularFont).setFontSize(10)));
        }

        private void addHeaderRow(Table table, String[] headers, PdfFont boldFont) {
            for (String header : headers) {
                Cell headerCell = new Cell().add(new Paragraph(header).setFont(boldFont).setFontSize(10))
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(5);
                table.addCell(headerCell);
            }
        }

        private int addExcelRow(Sheet sheet, int rowNum, String label, String value) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(label);
            row.createCell(1).setCellValue(value);
            return rowNum;
        }

        private int addExcelHeader(Sheet sheet, int rowNum, String header) {
            Row headerRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell headerCell = headerRow.createCell(0);
            headerCell.setCellValue(header);

            CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
            Font headerFont = sheet.getWorkbook().createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerCell.setCellStyle(headerStyle);

            return rowNum;
        }

        private String formatCurrency(BigDecimal amount) {
            if (amount == null) return "KES 0";
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "KE"));
            return formatter.format(amount);
        }

        private String formatDate(String dateString) {
            if (dateString == null) return "N/A";
            try {
                LocalDate date = LocalDate.parse(dateString);
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                return dateString;
            }
        }


    @Override
    public Page<ReportHistoryDto> getRecentReports(Pageable pageable) {
        // In production, fetch from a report history table
        List<ReportHistoryDto> reports = Arrays.asList(
            ReportHistoryDto.builder()
                .id(1L)
                .name("Monthly Client Demographics")
                .type("Demographic")
                .generatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .fileSize("2.4 MB")
                .status("COMPLETED")
                .build(),
            ReportHistoryDto.builder()
                .id(2L)
                .name("KYC Status Quarterly")
                .type("KYC")
                .generatedAt(LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .fileSize("1.8 MB")
                .status("COMPLETED")
                .build()
        );
        
        return new PageImpl<>(reports, pageable, reports.size());
    }
    
    private KycStatusReport.KycStatusCount createKycStatusCount(
            Map<GeneralConfig.KycStatus, Integer> counts, 
            GeneralConfig.KycStatus status, 
            int total) {
        int count = counts.getOrDefault(status, 0);
        return KycStatusReport.KycStatusCount.builder()
            .count(count)
            .percentage(total > 0 ? (count * 100.0 / total) : 0)
            .build();
    }


    @Override
    @Transactional
    public ReportConfigurationDto saveReportConfiguration(SaveReportRequestDto request, Long userId) {
        log.info("Saving report configuration: {} for user: {}", request.getName(), userId);

        try {
            // Build parameters JSON
            Map<String, Object> params = new HashMap<>();
            params.put("branchId", request.getBranchId());
            params.put("startDate", request.getStartDate());
            params.put("endDate", request.getEndDate());
            params.put("clientStatus", request.getClientStatus());
            params.put("dataFields", request.getDataFields());
            if (request.getAdditionalParams() != null) {
                params.putAll(request.getAdditionalParams());
            }

            ObjectMapper mapper = new ObjectMapper();
            String parametersJson = mapper.writeValueAsString(params);
            String dataFieldsJson = request.getDataFields() != null ?
                    mapper.writeValueAsString(request.getDataFields()) : null;

            // Create report configuration
            ReportConfiguration config = ReportConfiguration.builder()
                    .name(request.getName())
                    .reportType(request.getReportType())
                    .format(request.getFormat())
                    .createdBy(userId)
                    .createdAt(LocalDateTime.now())
                    .parameters(parametersJson)
                    .branchId(request.getBranchId())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .clientStatus(request.getClientStatus())
                    .dataFields(dataFieldsJson)
                    .status("ACTIVE")
                    .build();

            ReportConfiguration savedConfig = reportConfigurationRepository.save(config);

            return ReportConfigurationDto.builder()
                    .id(savedConfig.getId())
                    .name(savedConfig.getName())
                    .reportType(savedConfig.getReportType())
                    .format(savedConfig.getFormat())
                    .createdAt(savedConfig.getCreatedAt())
                    .branchId(savedConfig.getBranchId())
                    .startDate(savedConfig.getStartDate())
                    .endDate(savedConfig.getEndDate())
                    .clientStatus(savedConfig.getClientStatus())
                    .build();

        } catch (Exception e) {
            log.error("Error saving report configuration: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save report configuration: " + e.getMessage());
        }
    }

    @Override
    public Page<ReportConfigurationDto> getSavedReports(Long userId, Pageable pageable) {
        log.info("Fetching saved reports for user: {}", userId);

        Page<ReportConfiguration> configs = reportConfigurationRepository
                .findByCreatedByOrderByCreatedAtDesc(userId, pageable);

        return configs.map(config -> ReportConfigurationDto.builder()
                .id(config.getId())
                .name(config.getName())
                .reportType(config.getReportType())
                .format(config.getFormat())
                .createdAt(config.getCreatedAt())
                .branchId(config.getBranchId())
                .startDate(config.getStartDate())
                .endDate(config.getEndDate())
                .clientStatus(config.getClientStatus())
                .build());
    }

    @Override
    public byte[] generateReportFromConfiguration(Long configId, Long userId, String format) {
        log.info("Generating report from configuration: {} for user: {}", configId, userId);

        ReportConfiguration config = reportConfigurationRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("Report configuration not found"));

        // Verify ownership
        if (!config.getCreatedBy().equals(userId)) {
            throw new RuntimeException("You don't have permission to access this report");
        }

        try {
            // Parse parameters
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> params = mapper.readValue(config.getParameters(),
                    new TypeReference<Map<String, Object>>() {});

            // Add parameters to map
            params.put("branchId", config.getBranchId());
            if (config.getStartDate() != null) params.put("startDate", LocalDate.parse(config.getStartDate()));
            if (config.getEndDate() != null) params.put("endDate", LocalDate.parse(config.getEndDate()));
            params.put("clientStatus", config.getClientStatus());
            if (config.getDataFields() != null) {
                List<String> fields = mapper.readValue(config.getDataFields(),
                        new TypeReference<List<String>>() {});
                params.put("dataFields", fields);
            }

            // Generate report using existing export method
            return exportReport(config.getReportType(), format, params);

        } catch (Exception e) {
            log.error("Error generating report from configuration: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate report: " + e.getMessage());
        }
    }

    @Override
    public void deleteReportConfiguration(Long configId, Long userId) {
        log.info("Deleting report configuration: {} for user: {}", configId, userId);

        ReportConfiguration config = reportConfigurationRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("Report configuration not found"));

        if (!config.getCreatedBy().equals(userId)) {
            throw new RuntimeException("You don't have permission to delete this report");
        }

        reportConfigurationRepository.delete(config);
    }



}