// service/impl/ReportServiceImpl.java
package com.microfinance.reports.service;

import com.microfinance.audit.repository.AuditLogRepository;
import com.microfinance.audit.service.AuditService;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.borrower.repository.BorrowerDocumentRepository;
import com.microfinance.borrower.repository.BorrowerRepository;
import com.microfinance.loanapplications.repository.*;
import com.microfinance.reports.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final LoanRepository loanRepository;
    private final RecoveryCaseRepository recoveryCaseRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final BorrowerRepository borrowerRepository;
    private final BorrowerDocumentRepository borrowerDocumentRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final LegalNoticeRepository legalNoticeRepository;
    @Autowired
    private AuditService auditService;



    @Value("${app.config.compliance.interest-rate-cap:18}")
    private BigDecimal interestRateCap;

    @Value("${app.config.compliance.penalty-rate-cap:25}")
    private BigDecimal penaltyRateCap;

    @Value("${app.config.compliance.provisioning-requirement:100}")
    private BigDecimal provisioningRequirement;


    @Override
    @Transactional(readOnly = true)
    public FinancialReportDto generateFinancialReport(ReportFilterDto filter, User currentUser) {
        log.info("Generating financial report for period: {} to {}", filter.getStartDate(), filter.getEndDate());

        long startTime = System.currentTimeMillis();

        LocalDate startDate = filter.getStartDate() != null ? filter.getStartDate() : LocalDate.now().minusMonths(1);
        LocalDate endDate = filter.getEndDate() != null ? filter.getEndDate() : LocalDate.now();
        
        // Income calculations
        BigDecimal totalInterestIncome = calculateTotalInterestIncome(startDate, endDate);
        BigDecimal totalFeeIncome = calculateTotalFeeIncome(startDate, endDate);
        BigDecimal totalPenaltyIncome = calculateTotalPenaltyIncome(startDate, endDate);
        BigDecimal totalOtherIncome = calculateTotalOtherIncome(startDate, endDate);
        BigDecimal totalIncome = totalInterestIncome.add(totalFeeIncome).add(totalPenaltyIncome).add(totalOtherIncome);
        
        // Expense calculations
        BigDecimal totalInterestExpense = calculateTotalInterestExpense(startDate, endDate);
        BigDecimal totalOperatingExpense = calculateTotalOperatingExpense(startDate, endDate);
        BigDecimal totalProvisionExpense = calculateTotalProvisionExpense(startDate, endDate);
        BigDecimal totalExpenses = totalInterestExpense.add(totalOperatingExpense).add(totalProvisionExpense);
        
        // Profit/Loss
        BigDecimal netProfit = totalIncome.subtract(totalExpenses);
        BigDecimal netProfitMargin = totalIncome.compareTo(BigDecimal.ZERO) > 0 
            ? netProfit.divide(totalIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
        
        // Balance Sheet
        BigDecimal totalLoanPortfolio = getTotalOutstandingBalance();
        BigDecimal totalCashAndBank = getTotalCashAndBank();
        BigDecimal totalReceivables = getTotalReceivables();
        BigDecimal totalAssets = totalLoanPortfolio.add(totalCashAndBank).add(totalReceivables);
        BigDecimal totalLiabilities = getTotalLiabilities();
        BigDecimal totalEquity = totalAssets.subtract(totalLiabilities);
        
        // Ratios
        BigDecimal returnOnAssets = totalAssets.compareTo(BigDecimal.ZERO) > 0
            ? netProfit.divide(totalAssets, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
        
        BigDecimal returnOnEquity = totalEquity.compareTo(BigDecimal.ZERO) > 0
            ? netProfit.divide(totalEquity, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
        
        BigDecimal operatingEfficiency = totalIncome.compareTo(BigDecimal.ZERO) > 0
            ? totalOperatingExpense.divide(totalIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
        
        String periodLabel = getPeriodLabel(startDate, endDate);

        long duration = System.currentTimeMillis() - startTime;

        // Log the report generation
        auditService.logReportGeneration("FINANCIAL_REPORT", filter.getFormat(), duration);
        
        return FinancialReportDto.builder()
                .reportDate(LocalDate.now())
                .reportPeriod(periodLabel)
                .totalInterestIncome(totalInterestIncome)
                .totalFeeIncome(totalFeeIncome)
                .totalPenaltyIncome(totalPenaltyIncome)
                .totalOtherIncome(totalOtherIncome)
                .totalIncome(totalIncome)
                .totalInterestExpense(totalInterestExpense)
                .totalOperatingExpense(totalOperatingExpense)
                .totalProvisionExpense(totalProvisionExpense)
                .totalExpenses(totalExpenses)
                .netProfit(netProfit)
                .netProfitMargin(netProfitMargin)
                .totalLoanPortfolio(totalLoanPortfolio)
                .totalCashAndBank(totalCashAndBank)
                .totalReceivables(totalReceivables)
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .totalEquity(totalEquity)
                .returnOnAssets(returnOnAssets)
                .returnOnEquity(returnOnEquity)
                .operatingEfficiency(operatingEfficiency)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioReportDto generatePortfolioReport(ReportFilterDto filter, User currentUser) {
        log.info("Generating portfolio report for period: {} to {}", filter.getStartDate(), filter.getEndDate());

        LocalDate asOfDate = filter.getEndDate() != null ? filter.getEndDate() : LocalDate.now();

        // Portfolio Overview
        Integer totalActiveLoans = loanRepository.countActiveLoansForReport();
        Integer totalDisbursedLoans = loanRepository.countDisbursedLoansForReport();
        BigDecimal totalDisbursedAmount = loanRepository.sumDisbursedAmountForReport();
        BigDecimal totalWriteOff = loanRepository.sumWriteOffAmountForReport();

        // Recovery Summary - Handle Object[] correctly
        BigDecimal totalRecoveredAmount = BigDecimal.ZERO;
        BigDecimal totalOutstandingAmount = BigDecimal.ZERO;

        try {
            List<Object[]> recoverySummaryList = recoveryCaseRepository.getRecoverySummary();
            if (recoverySummaryList != null && !recoverySummaryList.isEmpty()) {
                Object[] recoverySummary = recoverySummaryList.get(0);
                totalRecoveredAmount = recoverySummary[0] != null ? (BigDecimal) recoverySummary[0] : BigDecimal.ZERO;
                totalOutstandingAmount = recoverySummary[1] != null ? (BigDecimal) recoverySummary[1] : BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.error("Error getting recovery summary: {}", e.getMessage());
            totalRecoveredAmount = recoveryCaseRepository.sumTotalRecoveredAmount();
            totalOutstandingAmount = loanRepository.sumOutstandingBalanceForReport();
        }

        // Calculate recovery rate
        BigDecimal totalPortfolio = totalOutstandingAmount.add(totalRecoveredAmount);
        BigDecimal recoveryRate = totalPortfolio.compareTo(BigDecimal.ZERO) > 0
                ? totalRecoveredAmount.divide(totalPortfolio, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Calculate average loan size
        BigDecimal averageLoanSize = totalActiveLoans > 0 && totalActiveLoans != null
                ? totalOutstandingAmount.divide(BigDecimal.valueOf(totalActiveLoans), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Portfolio at Risk
        BigDecimal par1Day = calculatePar(1, asOfDate);
        BigDecimal par30Days = calculatePar(30, asOfDate);
        BigDecimal par60Days = calculatePar(60, asOfDate);
        BigDecimal par90Days = calculatePar(90, asOfDate);
        BigDecimal par180Days = calculatePar(180, asOfDate);

        // Aging Analysis
        BigDecimal currentPortfolio = calculateAgingPortfolio(0, 0, asOfDate);
        BigDecimal overdue1To30Days = calculateAgingPortfolio(1, 30, asOfDate);
        BigDecimal overdue31To60Days = calculateAgingPortfolio(31, 60, asOfDate);
        BigDecimal overdue61To90Days = calculateAgingPortfolio(61, 90, asOfDate);
        BigDecimal overdue91To180Days = calculateAgingPortfolio(91, 180, asOfDate);
        BigDecimal overdue180PlusDays = calculateAgingPortfolio(181, 999, asOfDate);

        // Portfolio by Product and Branch (with error handling)
        Map<String, ProductPortfolioDto> portfolioByProduct = getPortfolioByProduct();
        Map<String, ProductPortfolioDto> portfolioByBranch = getPortfolioByBranch();

        return PortfolioReportDto.builder()
                .reportDate(asOfDate)
                .totalActiveLoans(totalActiveLoans != null ? totalActiveLoans : 0)
                .totalDisbursedLoans(totalDisbursedLoans != null ? totalDisbursedLoans : 0)
                .totalDisbursedAmount(totalDisbursedAmount != null ? totalDisbursedAmount : BigDecimal.ZERO)
                .totalOutstandingAmount(totalOutstandingAmount)
                .averageLoanSize(averageLoanSize)
                .portfolioAtRisk1Day(par1Day)
                .portfolioAtRisk30Days(par30Days)
                .portfolioAtRisk60Days(par60Days)
                .portfolioAtRisk90Days(par90Days)
                .portfolioAtRisk180Days(par180Days)
                .writeOffAmount(totalWriteOff != null ? totalWriteOff : BigDecimal.ZERO)
                .recoveredAmount(totalRecoveredAmount)
                .recoveryRate(recoveryRate)
                .currentPortfolio(currentPortfolio)
                .overdue1To30Days(overdue1To30Days)
                .overdue31To60Days(overdue31To60Days)
                .overdue61To90Days(overdue61To90Days)
                .overdue91To180Days(overdue91To180Days)
                .overdue180PlusDays(overdue180PlusDays)
                .portfolioByProduct(portfolioByProduct != null ? portfolioByProduct : new HashMap<>())
                .portfolioByBranch(portfolioByBranch != null ? portfolioByBranch : new HashMap<>())
                .build();
    }

    // Helper methods with error handling
    private BigDecimal calculatePar(int daysOverdue, LocalDate asOfDate) {
        try {
            BigDecimal parAmount = loanRepository.sumOutstandingForOverdueDaysForReport(daysOverdue, asOfDate);
            BigDecimal totalPortfolio = loanRepository.sumOutstandingBalanceForReport();
            if (totalPortfolio != null && totalPortfolio.compareTo(BigDecimal.ZERO) > 0 && parAmount != null) {
                return parAmount.divide(totalPortfolio, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Error calculating PAR for {} days: {}", daysOverdue, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateAgingPortfolio(int minDays, int maxDays, LocalDate asOfDate) {
        try {
            BigDecimal result = loanRepository.sumOutstandingForAgingRangeForReport(minDays, maxDays, asOfDate);
            return result != null ? result : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Error calculating aging portfolio for range {}-{}: {}", minDays, maxDays, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Map<String, ProductPortfolioDto> getPortfolioByProduct() {
        Map<String, ProductPortfolioDto> result = new HashMap<>();
        try {
            List<ProductPortfolioDto> portfolioList = loanRepository.getPortfolioByProductForReport();
            if (portfolioList != null) {
                for (ProductPortfolioDto dto : portfolioList) {
                    if (dto != null && dto.getName() != null) {
                        result.put(dto.getName(), dto);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting portfolio by product: {}", e.getMessage());
        }
        return result;
    }

    private Map<String, ProductPortfolioDto> getPortfolioByBranch() {
        Map<String, ProductPortfolioDto> result = new HashMap<>();
        try {
            List<ProductPortfolioDto> portfolioList = loanRepository.getPortfolioByBranchForReport();
            if (portfolioList != null) {
                for (ProductPortfolioDto dto : portfolioList) {
                    if (dto != null && dto.getName() != null) {
                        result.put(dto.getName(), dto);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting portfolio by branch: {}", e.getMessage());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceReportDto generateComplianceReport(ReportFilterDto filter, User currentUser) {
        log.info("Generating compliance report");
        
        LocalDate reportDate = LocalDate.now();
        
        // Regulatory Ratios
        BigDecimal capitalAdequacyRatio = calculateCapitalAdequacyRatio();
        BigDecimal liquidityRatio = calculateLiquidityRatio();
        BigDecimal nonPerformingLoanRatio = calculateNPLRatio();
        BigDecimal provisioningRatio = calculateProvisioningRatio();
        
        // KYC Compliance
        Integer totalBorrowers = borrowerRepository.countActiveBorrowers();
        Integer kycVerifiedCount = borrowerRepository.countKycVerified();
        Integer kycPendingCount = borrowerRepository.countKycPending();
        Integer kycExpiredCount = borrowerRepository.countKycExpired();
        Double kycComplianceRate = totalBorrowers > 0 
            ? (kycVerifiedCount.doubleValue() / totalBorrowers) * 100 
            : 0.0;
        
        // Interest Rate Compliance
        BigDecimal averageInterestRate = loanRepository.calculateAverageInterestRateForReport();
        BigDecimal maxInterestRate = loanRepository.findMaxInterestRateForReport();
        BigDecimal minInterestRate = loanRepository.findMinInterestRateForReport();

        BigDecimal ratecap= interestRateCap;
        Integer loansExceedingRateCap = loanRepository.countLoansExceedingRateCapForReport(ratecap);
        
        // Legal Actions
        Integer pendingLegalCases = (int) legalNoticeRepository.countByStatus("SENT");
        Integer resolvedLegalCases = (int) legalNoticeRepository.countByStatus("COMPLIED");
        BigDecimal amountUnderLitigation = recoveryCaseRepository.sumAmountUnderLitigation();
        
        return ComplianceReportDto.builder()
                .reportDate(reportDate)
                .reportingPeriod(getPeriodLabel(filter.getStartDate(), filter.getEndDate()))
                .capitalAdequacyRatio(capitalAdequacyRatio)
                .liquidityRatio(liquidityRatio)
                .nonPerformingLoanRatio(nonPerformingLoanRatio)
                .provisioningRatio(provisioningRatio)
                .totalBorrowers(totalBorrowers)
                .kycVerifiedCount(kycVerifiedCount)
                .kycPendingCount(kycPendingCount)
                .kycExpiredCount(kycExpiredCount)
                .kycComplianceRate(kycComplianceRate)
                .averageInterestRate(averageInterestRate)
                .maxInterestRate(maxInterestRate)
                .minInterestRate(minInterestRate)
                .loansExceedingRateCap(loansExceedingRateCap)
                .gdprCompliant(true)
                .lastAuditDate(reportDate.minusMonths(6))
                .dataBreachIncidents(0)
                .pendingLegalCases(pendingLegalCases)
                .resolvedLegalCases(resolvedLegalCases)
                .amountUnderLitigation(amountUnderLitigation)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuditReportDto generateAuditReport(ReportFilterDto filter, User currentUser) {
        log.info("Generating audit report");


        
        LocalDate startDate = filter.getStartDate() != null ? filter.getStartDate() : LocalDate.now().minusMonths(1);
        LocalDate endDate = filter.getEndDate() != null ? filter.getEndDate() : LocalDate.now();
        log.debug(">>>getUserActivityStats called with startDate: {}, endDate: {}", startDate, endDate);

        // Convert LocalDate to LocalDateTime
        LocalDateTime startDateTime = startDate.atStartOfDay(); // 00:00:00
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59); // 23:59:59

        log.debug(">>> Converted to LocalDateTime - startDateTime: {}, endDateTime: {}", startDateTime, endDateTime);
        
        // User Activity
        Integer totalActiveUsers = userRepository.countActiveUsers();
        log.debug(">>>Step 0: {}, endDate: {}", startDate, endDate);
        Integer totalLoginCount = auditLogRepository.countLoginsInPeriod(startDateTime, endDateTime);
        log.debug(">>>Step 1: {}, endDate: {}", startDate, endDate);
        Integer failedLoginAttempts = auditLogRepository.countFailedLoginsInPeriod(startDateTime, endDateTime);
        log.debug(">>>Step 2: {}, endDate: {}", startDate, endDate);
        List<UserActivityDto> topActiveUsers = getUserActivityStats(startDate, endDate);
        log.debug(">>>Step 3: {}, endDate: {}", startDate, endDate);
        // Get user statistics by role for additional insights
        List<Object[]> userStatsByRole = userRepository.getUserStatisticsByRole();
        // Count users by specific roles
        Integer collectionOfficersCount = userRepository.countUsersByRole(User.UserRole.COLLECTION_OFFICER);
        Integer legalOfficersCount = userRepository.countUsersByRole(User.UserRole.LEGAL_OFFICER);
        Integer branchManagersCount = userRepository.countUsersByRole(User.UserRole.BRANCH_MANAGER);
        // Count recently active users (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Integer recentlyActiveUsers = userRepository.countRecentlyActiveUsers(thirtyDaysAgo);
        
        // System Activity
        Integer totalTransactions = auditLogRepository.countTransactionsInPeriod(startDateTime, endDateTime);
        log.debug(">>>Step 4: {}, endDate: {}", startDate, endDate);

        Integer totalLoansCreated = loanRepository.countLoansCreatedInPeriodForReport(startDateTime, endDateTime);
        Integer totalRepaymentsProcessed = repaymentRepository.countProcessedInPeriod(startDate, endDate);
        Integer totalDisbursements = loanRepository.countLoansDisbursedInPeriodForReport(startDate, endDate);
        
        // Security Events
        Integer totalSecurityEvents = auditLogRepository.countSecurityEventsInPeriod(startDateTime, endDateTime);
        Integer criticalSecurityEvents = auditLogRepository.countCriticalSecurityEventsInPeriod(startDateTime, endDateTime);
        List<SecurityEventDto> recentSecurityEvents = getRecentSecurityEvents(startDate);
        
        // Data Changes
        Integer totalDataChanges = auditLogRepository.countDataChangesInPeriod(startDateTime, endDateTime);
        List<DataChangeDto> recentDataChanges = getRecentDataChanges(startDate);

        
        return AuditReportDto.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(currentUser.getUsername())
                .reportPeriod(getPeriodLabel(startDate, endDate))
                .totalActiveUsers(totalActiveUsers)
                .totalLoginCount(totalLoginCount)
                .failedLoginAttempts(failedLoginAttempts)
                .topActiveUsers(topActiveUsers)
                .totalTransactions(totalTransactions)
                .totalLoansCreated(totalLoansCreated)
                .totalRepaymentsProcessed(totalRepaymentsProcessed)
                .totalDisbursements(totalDisbursements)
                .totalSecurityEvents(totalSecurityEvents)
                .criticalSecurityEvents(criticalSecurityEvents)
                .recentSecurityEvents(recentSecurityEvents)
                .totalDataChanges(totalDataChanges)
                .recentDataChanges(recentDataChanges)
                .build();
    }

    @Override
    public byte[] exportReport(String reportType, ReportFilterDto filter, String format, User currentUser) {
        log.info("Exporting report: {} in format: {}", reportType, format);
        
        // This will generate PDF/Excel/CSV based on format
        // Implementation using JasperReports or Apache POI
        return new byte[0]; // Placeholder
    }

    @Override
    public Map<String, Object> getReportStatistics(User currentUser) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getReportStatistics(ReportFilterDto filter, User currentUser) {
        Map<String, Object> stats = new HashMap<>();
        LocalDate startDate = filter.getStartDate() != null ? filter.getStartDate() : LocalDate.now().minusMonths(1);
        LocalDate endDate = filter.getEndDate() != null ? filter.getEndDate() : LocalDate.now();
        // Convert LocalDate to LocalDateTime
        LocalDateTime startDateTime = startDate.atStartOfDay(); // 00:00:00
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59); // 23:59:59
        
        stats.put("totalReportsGenerated", auditLogRepository.countReportGenerations(startDateTime,endDateTime));
        stats.put("mostViewedReport", auditLogRepository.getMostViewedReport());
        stats.put("averageReportGenerationTime", auditLogRepository.getAverageReportGenerationTime(startDateTime,endDateTime));
        
        return stats;
    }

    // Helper methods for calculations
    private BigDecimal calculateTotalInterestIncome(LocalDate startDate, LocalDate endDate) {
        return repaymentRepository.sumInterestCollected(startDate, endDate);
    }
    
    private BigDecimal calculateTotalFeeIncome(LocalDate startDate, LocalDate endDate) {
        return repaymentRepository.sumFeesCollected(startDate, endDate);
    }
    
    private BigDecimal calculateTotalPenaltyIncome(LocalDate startDate, LocalDate endDate) {
        return repaymentRepository.sumPenaltiesCollected(startDate, endDate);
    }
    
    private BigDecimal calculateTotalOtherIncome(LocalDate startDate, LocalDate endDate) {
        return BigDecimal.ZERO; // Implement as needed
    }
    
    private BigDecimal calculateTotalInterestExpense(LocalDate startDate, LocalDate endDate) {
        return BigDecimal.ZERO; // Implement as needed
    }
    
    private BigDecimal calculateTotalOperatingExpense(LocalDate startDate, LocalDate endDate) {
        return BigDecimal.ZERO; // Implement as needed
    }
    
    private BigDecimal calculateTotalProvisionExpense(LocalDate startDate, LocalDate endDate) {
        return loanRepository.sumProvisionAmountForReport();
    }
    
    private BigDecimal getTotalOutstandingBalance() {
        return loanRepository.sumOutstandingBalanceForReport();
    }
    
    private BigDecimal getTotalCashAndBank() {
        return BigDecimal.ZERO; // Implement as needed
    }
    
    private BigDecimal getTotalReceivables() {
        return BigDecimal.ZERO; // Implement as needed
    }
    
    private BigDecimal getTotalLiabilities() {
        return BigDecimal.ZERO; // Implement as needed
    }

    
    private BigDecimal calculateCapitalAdequacyRatio() {
        return BigDecimal.valueOf(12.5); // Placeholder - implement actual calculation
    }
    
    private BigDecimal calculateLiquidityRatio() {
        return BigDecimal.valueOf(25.0); // Placeholder - implement actual calculation
    }
    
    private BigDecimal calculateNPLRatio() {
        BigDecimal nplAmount = loanRepository.sumNonPerformingLoansForReport();
        BigDecimal totalPortfolio = getTotalOutstandingBalance();
        return totalPortfolio.compareTo(BigDecimal.ZERO) > 0
            ? nplAmount.divide(totalPortfolio, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
    }
    
    private BigDecimal calculateProvisioningRatio() {
        BigDecimal totalProvision = loanRepository.sumProvisionAmountForReport();
        BigDecimal nplAmount = loanRepository.sumNonPerformingLoansForReport();
        return nplAmount.compareTo(BigDecimal.ZERO) > 0
            ? totalProvision.divide(nplAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
    }


    private List<UserActivityDto> getUserActivityStatsAll(LocalDate startDate, LocalDate endDate) {
        // Convert LocalDate to LocalDateTime
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Get all results without pagination
        List<Object[]> results = auditLogRepository.getUserActivityStatsAll(startDateTime, endDateTime);

        // Convert to DTOs
        return results.stream()
                .map(result -> UserActivityDto.builder()
                        .userId((Long) result[0])
                        .username((String) result[1])
                        .actionCount(((Long) result[2]).intValue())
                        .lastActive(getLastActiveForUser((Long) result[0], startDateTime, endDateTime))
                        .build())
                .collect(Collectors.toList());
    }

    // Helper method to get last active timestamp for a user
    private LocalDateTime getLastActiveForUser(Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return auditLogRepository.findMaxTimestampByUserAndPeriod(userId, startDateTime, endDateTime);
    }



    private List<UserActivityDto> getUserActivityStats(LocalDate startDate, LocalDate endDate) {
        // Convert LocalDate to LocalDateTime
        LocalDateTime startDateTime = startDate.atStartOfDay(); // 00:00:00
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59); // 23:59:59

        // Create Pageable with limit (e.g., top 10 users)
        Pageable pageable = PageRequest.of(0, 10); // Get top 10 most active users

        // Get results from repository
        List<Object[]> results = auditLogRepository.getUserActivityStats(startDateTime, endDateTime, pageable);

        // Convert to DTOs
        return results.stream()
                .map(result -> UserActivityDto.builder()
                        .userId((Long) result[0])
                        .username((String) result[1])
                        .actionCount(((Long) result[2]).intValue())
                        .build())
                .collect(Collectors.toList());
    }


    private List<SecurityEventDto> getRecentSecurityEvents(LocalDate since) {
        LocalDateTime startDateTime = since.atStartOfDay();
        return auditLogRepository.getRecentSecurityEvents(startDateTime);
    }
    
    private List<DataChangeDto> getRecentDataChanges(LocalDate since) {
        LocalDateTime startDateTime = since.atStartOfDay();
        return auditLogRepository.getRecentDataChanges(startDateTime);
    }


    
    private String getPeriodLabel(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return "Custom Period";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return startDate.format(formatter) + " - " + endDate.format(formatter);
    }
}