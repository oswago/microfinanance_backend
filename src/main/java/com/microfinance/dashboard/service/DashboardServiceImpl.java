// service/impl/DashboardServiceImpl.java
package com.microfinance.dashboard.service;

import com.microfinance.base.entity.User;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.base.service.UserService;
import com.microfinance.borrower.repository.BorrowerRepository;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.dashboard.dto.*;
import com.microfinance.loanapplications.entity.*;
import com.microfinance.loanapplications.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final BorrowerRepository borrowerRepository;
    private final LoanRepository loanRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final CollectionActionRepository collectionActionRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final ReschedulingRequestRepository reschedulingRequestRepository; // Add this

    @Autowired
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(User currentUser) {
        log.info("Fetching dashboard statistics for user: {}", currentUser.getUsername());
        
        LocalDate now = LocalDate.now();
        LocalDate oneMonthAgo = now.minusMonths(1);
        
        // Current period stats
        Integer totalUsers = userRepository.countActiveUsers();
        Integer totalBorrowers = borrowerRepository.countActiveBorrowers();
        Integer activeLoans = loanRepository.countActiveLoansForReport();
        BigDecimal totalPortfolio = loanRepository.sumOutstandingBalanceForReport();
        Integer pendingApplications = loanApplicationRepository.countByStatus(GeneralConfig.LoanApplicationStatus.PENDING_APPROVAL).intValue();
        
        // High risk loans (90+ days overdue)
        Integer highRiskLoans = loanRepository.findOverdueLoans(now).size();
        
        // Previous period stats for growth calculations
        LocalDateTime oneMonthAgoStart = oneMonthAgo.atStartOfDay();
        LocalDateTime nowEnd = LocalDateTime.now();
        
        Integer previousUsers = userRepository.countUsersCreatedInPeriod(oneMonthAgoStart.minusMonths(1), oneMonthAgoStart);
        Integer previousBorrowers = borrowerRepository.countBorrowersCreatedInPeriod(oneMonthAgoStart.minusMonths(1), oneMonthAgoStart);
        Integer previousActiveLoans = loanRepository.countActiveLoansForReport(); // Simplified
        BigDecimal previousPortfolio = loanRepository.sumOutstandingBalanceForReport(); // Simplified
        
        Double userGrowth = calculateGrowth(previousUsers, totalUsers);
        Double borrowerGrowth = calculateGrowth(previousBorrowers, totalBorrowers);
        Double loanGrowth = calculateGrowth(previousActiveLoans, activeLoans);
        Double portfolioGrowth = calculateGrowth(previousPortfolio, totalPortfolio);
        
        return DashboardStatsDto.builder()
                .totalUsers(totalUsers != null ? totalUsers : 0)
                .totalBorrowers(totalBorrowers != null ? totalBorrowers : 0)
                .activeLoans(activeLoans != null ? activeLoans : 0)
                .totalPortfolio(totalPortfolio != null ? totalPortfolio : BigDecimal.ZERO)
                .pendingApplications(pendingApplications != null ? pendingApplications : 0)
                .highRiskLoans(highRiskLoans != null ? highRiskLoans : 0)
                .userGrowth(userGrowth)
                .borrowerGrowth(borrowerGrowth)
                .loanGrowth(loanGrowth)
                .portfolioGrowth(portfolioGrowth)
                .build();
    }



    @Override
    @Transactional(readOnly = true)
    public PortfolioDistributionDto getPortfolioDistribution(User currentUser) {
        log.info("Fetching portfolio distribution");

        List<Object[]> results = loanRepository.getPortfolioByProductForReportDash();
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        if (results != null && !results.isEmpty()) {
            for (Object[] row : results) {
                if (row.length > 0 && row[0] != null) {
                    labels.add(row[0].toString());  // Product name
                    // row[2] is the outstanding amount (index 2)
                    BigDecimal amount = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
                    values.add(amount.doubleValue());
                }
            }
        }

        // If no data, return default sample data
        if (labels.isEmpty()) {
            labels = Arrays.asList("Personal", "Business", "Emergency", "Education", "Agriculture");
            values = Arrays.asList(65000.0, 89000.0, 45000.0, 32000.0, 28000.0);
        }

        return PortfolioDistributionDto.builder()
                .labels(labels)
                .values(values)
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public RepaymentPerformanceDto getRepaymentPerformance(User currentUser) {
        log.info("Fetching repayment performance");
        
        LocalDate now = LocalDate.now();
        LocalDate monthAgo = now.minusMonths(1);
        
        Object[] stats = loanRepaymentRepository.getOnTimeVsLateRepaymentStats(monthAgo, now);
        
        Long onTimeCount = 0L;
        Long lateCount = 0L;
        
        if (stats != null && stats.length >= 3) {
            onTimeCount = stats[0] != null ? ((Number) stats[0]).longValue() : 0L;
            lateCount = stats[2] != null ? ((Number) stats[2]).longValue() : 0L;
        }
        
        List<String> labels = Arrays.asList("On Time", "Late");
        List<Double> values = Arrays.asList(onTimeCount.doubleValue(), lateCount.doubleValue());
        
        // If no data, return sample
        if (onTimeCount == 0 && lateCount == 0) {
            values = Arrays.asList(75.0, 25.0);
        }
        
        return RepaymentPerformanceDto.builder()
                .labels(labels)
                .values(values)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentActivityDto> getRecentActivities(int limit, User currentUser) {
        log.info("Fetching recent activities for user: {}, limit: {}", currentUser.getUsername(), limit);

        Long branchId = getUserBranchId(currentUser);
        List<RecentActivityDto> activities = new ArrayList<>();

        // 1. Get recent loan applications
        List<LoanApplication> recentApplications;
        if (branchId != null) {
            recentApplications = loanApplicationRepository
                    .findTopByBranchIdOrderByCreatedAtDesc(branchId, PageRequest.of(0, limit));
        } else {
            recentApplications = loanApplicationRepository
                    .findTopByOrderByCreatedAtDesc(PageRequest.of(0, limit));
        }

        for (LoanApplication app : recentApplications) {
            String message;
            String severity = "info";
            String icon = "pi pi-file-edit";

            switch (app.getStatus()) {
                case PENDING_APPROVAL:
                    message = "New loan application pending approval: " + app.getApplicationNumber();
                    severity = "warning";
                    icon = "pi pi-clock";
                    break;
                case APPROVED:
                    message = "Loan application approved: " + app.getApplicationNumber();
                    severity = "success";
                    icon = "pi pi-check-circle";
                    break;
                case REJECTED:
                    message = "Loan application rejected: " + app.getApplicationNumber();
                    severity = "danger";
                    icon = "pi pi-times-circle";
                    break;
                default:
                    message = "Loan application submitted: " + app.getApplicationNumber();
                    severity = "info";
                    icon = "pi pi-file-edit";
                    break;
            }
            User appuser = userService.getUserById(app.getCreatedBy());
            String username=appuser.getUsername();

            activities.add(RecentActivityDto.builder()
                    .id(app.getId())
                    .type("APPLICATION")
                    .icon(icon)
                    .message(message)
                    .description(String.format("%s by %s",
                            app.getApplicationNumber(),
                            app.getBorrower() != null ? app.getBorrower().getFullName() : "Unknown"))
                    .timestamp(app.getCreatedAt())
                    .severity(severity)
                    .referenceNumber(app.getApplicationNumber())
                    .userName(app.getCreatedBy() != null ? username : "System")
                    .build());
        }

        // 2. Get recent loan disbursements
        List<Loan> recentDisbursements;
        if (branchId != null) {
            recentDisbursements = loanRepository
                    .findTopByBranchIdAndDisbursementDateNotNullOrderByDisbursementDateDesc(branchId, PageRequest.of(0, limit));
        } else {
            recentDisbursements = loanRepository
                    .findTopByDisbursementDateNotNullOrderByDisbursementDateDesc(PageRequest.of(0, limit));
        }

        for (Loan loan : recentDisbursements) {
            activities.add(RecentActivityDto.builder()
                    .id(loan.getId())
                    .type("DISBURSEMENT")
                    .icon("pi pi-money-bill")
                    .message("Loan disbursed: " + loan.getLoanAccountNumber())
                    .description(String.format("Amount %s disbursed to %s",
                            formatCurrency(loan.getNetDisbursementAmount()),
                            loan.getBorrower().getFullName()))
                    .timestamp(loan.getDisbursementDate() != null ?
                            loan.getDisbursementDate().atStartOfDay() : loan.getUpdatedAt())
                    .severity("success")
                    .amount(loan.getNetDisbursementAmount())
                    .referenceNumber(loan.getLoanAccountNumber())
                    .userName(loan.getDisbursedBy() != null ?
                            loan.getDisbursedBy().getUsername() : "System")
                    .build());
        }

        // 3. Get recent repayments
        List<LoanRepayment> recentRepayments;
        if (branchId != null) {
            recentRepayments = loanRepaymentRepository
                    .findTopByBranchIdOrderByCreatedAtDesc(branchId, PageRequest.of(0, limit));
        } else {
            recentRepayments = loanRepaymentRepository
                    .findTopByOrderByCreatedAtDesc(PageRequest.of(0, limit));
        }

        for (LoanRepayment repayment : recentRepayments) {
            activities.add(RecentActivityDto.builder()
                    .id(repayment.getId())
                    .type("REPAYMENT")
                    .icon("pi pi-dollar")
                    .message("Payment received: " + repayment.getReceiptNumber())
                    .description(String.format("Amount %s for loan %s",
                            formatCurrency(repayment.getAmountPaid()),
                            repayment.getLoan() != null ? repayment.getLoan().getLoanAccountNumber() : "Unknown"))
                    .timestamp(repayment.getCreatedAt())
                    .severity("success")
                    .amount(repayment.getAmountPaid())
                    .referenceNumber(repayment.getReceiptNumber())
                    .userName(repayment.getReceivedBy() != null ?
                            repayment.getReceivedBy().getUsername() : "System")
                    .build());
        }

        // 4. Get recent rescheduling requests
        List<ReschedulingRequest> recentReschedules;
        if (branchId != null) {
            recentReschedules = reschedulingRequestRepository
                    .findTopByBranchIdOrderByCreatedAtDesc(branchId, PageRequest.of(0, limit));
        } else {
            recentReschedules = reschedulingRequestRepository
                    .findTopByOrderByCreatedAtDesc(PageRequest.of(0, limit));
        }

        for (ReschedulingRequest request : recentReschedules) {
            String message;
            String severity;
            String icon = "pi pi-calendar-plus";

            switch (request.getStatus()) {
                case PENDING:
                    message = "Reschedule request pending: " + request.getRequestNumber();
                    severity = "warning";
                    icon = "pi pi-clock";
                    break;
                case APPROVED:
                    message = "Reschedule request approved: " + request.getRequestNumber();
                    severity = "success";
                    icon = "pi pi-check-circle";
                    break;
                case REJECTED:
                    message = "Reschedule request rejected: " + request.getRequestNumber();
                    severity = "danger";
                    icon = "pi pi-times-circle";
                    break;
                default:
                    message = "Reschedule request: " + request.getRequestNumber();
                    severity = "info";
                    icon = "pi pi-calendar-plus";
                    break;
            }

            String description = buildRescheduleDescription(request);

            activities.add(RecentActivityDto.builder()
                    .id(request.getId())
                    .type("RESCHEDULE")
                    .icon(icon)
                    .message(message)
                    .description(description)
                    .timestamp(request.getCreatedAt())
                    .severity(severity)
                    .referenceNumber(request.getRequestNumber())
                    .userName(request.getRequestedBy() != null ?
                            request.getRequestedBy().getUsername() : "System")
                    .build());
        }

        // 5. Get overdue loans with high delinquency
        List<Loan> overdueLoans;
        if (branchId != null) {
            overdueLoans = loanRepository
                    .findOverdueLoansByBranch( branchId,LocalDate.now(), PageRequest.of(0, limit));
        } else {
            overdueLoans = loanRepository
                    .findOverdueLoans(LocalDate.now(), PageRequest.of(0, limit));
        }

        for (Loan loan : overdueLoans) {
            String severity = loan.getDaysDelinquent() > 90 ? "danger" :
                    (loan.getDaysDelinquent() > 30 ? "warning" : "info");

            activities.add(RecentActivityDto.builder()
                    .id(loan.getId())
                    .type("OVERDUE")
                    .icon("pi pi-exclamation-triangle")
                    .message("Loan overdue: " + loan.getLoanAccountNumber())
                    .description(String.format("%d days overdue. Outstanding: %s",
                            loan.getDaysDelinquent(),
                            formatCurrency(loan.getOutstandingBalance())))
                    .timestamp(loan.getUpdatedAt())
                    .severity(severity)
                    .alert(loan.getDaysDelinquent() > 90 ? "Critical" :
                            (loan.getDaysDelinquent() > 30 ? "Warning" : "Due"))
                    .amount(loan.getOutstandingBalance())
                    .referenceNumber(loan.getLoanAccountNumber())
                    .userName(loan.getLoanOfficerName())
                    .build());
        }
        // Sort by timestamp descending and limit
        activities.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return activities.stream().limit(limit).collect(Collectors.toList());
    }


    /**
     * Helper method to build reschedule description
     */
    private String buildRescheduleDescription(ReschedulingRequest request) {
        switch (request.getRequestType()) {
            case TENURE_EXTENSION:
                return String.format("Tenure extended by %d months (from %d to %d months)",
                        request.getAdditionalMonths(),
                        request.getCurrentInstallments(),
                        request.getProposedInstallments());
            case PAYMENT_REDUCTION:
                return String.format("Monthly payment reduced from %s to %s",
                        formatCurrency(request.getCurrentMonthlyPayment()),
                        formatCurrency(request.getProposedMonthlyPayment()));
            case PAYMENT_HOLIDAY:
                return String.format("Payment holiday for %d months, resuming on %s",
                        request.getHolidayMonths(),
                        request.getResumeDate());
            case INTEREST_RATE_ADJUSTMENT:
                return String.format("Interest rate adjusted from %.2f%% to %.2f%%",
                        request.getCurrentInterestRate(),
                        request.getProposedInterestRate());
            default:
                return "Loan restructuring requested";
        }
    }

    /**
     * Helper method to format currency
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "KES 0";
        return String.format("KES %,.2f", amount);
    }


    @Override
    @Transactional(readOnly = true)
    public List<PendingTaskDto> getPendingTasks(User currentUser) {
        log.info("Fetching pending tasks for user: {}", currentUser.getUsername());
        
        List<PendingTaskDto> tasks = new ArrayList<>();
        long taskId = 1;
        
        // Check pending approvals based on user role
        if (currentUser.getRole() == User.UserRole.BRANCH_MANAGER || 
            currentUser.getRole() == User.UserRole.CREDIT_APPROVER) {
            
            List<LoanApplication> pendingApprovals = loanApplicationRepository
                    .findByStatus(GeneralConfig.LoanApplicationStatus.PENDING_APPROVAL)
                    .stream()
                    .limit(3)
                    .collect(Collectors.toList());
            
            for (LoanApplication app : pendingApprovals) {
                tasks.add(PendingTaskDto.builder()
                        .id(taskId++)
                        .description("Review loan application: " + 
                                (app.getApplicationNumber() != null ? app.getApplicationNumber() : "APP-" + app.getId()))
                        .completed(false)
                        .priority("High")
                        .build());
            }
        }
        
        // Check overdue loans for collection officers
        if (currentUser.getRole() == User.UserRole.COLLECTION_OFFICER) {
            long overdueCount = loanRepository.countOverdueLoans(null);
            if (overdueCount > 0) {
                tasks.add(PendingTaskDto.builder()
                        .id(taskId++)
                        .description("Contact " + overdueCount + " overdue borrowers")
                        .completed(false)
                        .priority("High")
                        .build());
            }
        }
        
        // Add KYC verification task
        long pendingKyc = borrowerRepository.countKycPending();
        if (pendingKyc > 0) {
            tasks.add(PendingTaskDto.builder()
                    .id(taskId++)
                    .description("Verify KYC for " + pendingKyc + " borrowers")
                    .completed(false)
                    .priority("Medium")
                    .build());
        }
        
        // Add system tasks
        tasks.add(PendingTaskDto.builder()
                .id(taskId++)
                .description("Generate monthly portfolio report")
                .completed(false)
                .priority("Medium")
                .build());
        
        // Mark some as completed for demo
        if (!tasks.isEmpty() && tasks.size() > 2) {
            tasks.get(1).setCompleted(true);
        }
        
        return tasks;
    }


    @Override
    @Transactional(readOnly = true)
    public List<SystemAlertDto> getSystemAlerts(User currentUser) {
        log.info("Fetching system alerts");

        List<SystemAlertDto> alerts = new ArrayList<>();
        long alertId = 1;

        // Check for severely overdue loans (90+ days)
        try {
            long severeOverdue = loanRepository.countOverdueLoansByDays(90);
            if (severeOverdue > 0) {
                alerts.add(SystemAlertDto.builder()
                        .id(alertId++)
                        .message(severeOverdue + " loan(s) are overdue by more than 90 days")
                        .severity("danger")
                        .icon("pi pi-exclamation-circle")
                        .build());
            }
        } catch (Exception e) {
            log.error("Error checking overdue loans: {}", e.getMessage());
        }

        // Check for KYC expiring soon
        try {
            long expiringKyc = borrowerRepository.countBorrowersWithKycExpiringInDays(30);
            if (expiringKyc > 0) {
                alerts.add(SystemAlertDto.builder()
                        .id(alertId++)
                        .message(expiringKyc + " borrower(s) have KYC documents expiring soon")
                        .severity("warning")
                        .icon("pi pi-clock")
                        .build());
            }
        } catch (Exception e) {
            log.error("Error checking expiring KYC: {}", e.getMessage());
        }

        // Check for pending approvals
        try {
            long pendingApprovals = loanApplicationRepository.countByStatus(GeneralConfig.LoanApplicationStatus.PENDING_APPROVAL);
            if (pendingApprovals > 5) {
                alerts.add(SystemAlertDto.builder()
                        .id(alertId++)
                        .message(pendingApprovals + " loan applications awaiting approval")
                        .severity("info")
                        .icon("pi pi-inbox")
                        .build());
            }
        } catch (Exception e) {
            log.error("Error checking pending approvals: {}", e.getMessage());
        }

        return alerts;
    }



    @Override
    @Transactional
    public void dismissAlert(Long alertId, User currentUser) {
        log.info("Dismissing alert: {} for user: {}", alertId, currentUser.getUsername());
        // In a real implementation, you would store dismissed alerts in a database
        // For now, this is a no-op since alerts are generated dynamically
    }
    
    // Helper methods
    private Double calculateGrowth(Number previous, Number current) {
        if (previous == null || previous.doubleValue() <= 0) {
            return 0.0;
        }
        double prev = previous.doubleValue();
        double curr = current != null ? current.doubleValue() : 0;
        return Math.round(((curr - prev) / prev) * 1000.0) / 10.0;
    }
    
    private Double calculateGrowth(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.doubleValue() <= 0) {
            return 0.0;
        }
        double prev = previous.doubleValue();
        double curr = current != null ? current.doubleValue() : 0;
        return Math.round(((curr - prev) / prev) * 1000.0) / 10.0;
    }


    @Transactional(readOnly = true)
    @Override
    public LoanStatsDTO getLoanStats(User currentUser) {
        log.info("Getting loan stats for user: {}", currentUser.getUsername());

        Long branchId = getUserBranchId(currentUser);
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

        LoanStatsDTO stats = new LoanStatsDTO();

        // Pending applications
        stats.setPendingApplications(countApplicationsByStatusAndBranch("PENDING_APPROVAL", branchId));
        stats.setDraftApplications(countApplicationsByStatusAndBranch("DRAFT", branchId));
        stats.setPendingApproval(countApplicationsByStatusAndBranch("PENDING_APPROVAL", branchId));

        // Approved today
        stats.setApprovedToday(countApprovedToday(startOfDay, endOfDay, branchId));

        // Pending disbursement
        stats.setPendingDisbursement(countLoansByStatusAndBranch("PENDING_DISBURSEMENT", branchId));
        stats.setDisbursedToday(countDisbursedToday(startOfDay, endOfDay, branchId));

        // Repayments
        stats.setDueToday(getDueTodayCount(branchId));
        stats.setOverdue(getOverdueCount(branchId));
        stats.setOverdue30Days(getOverdueCountByDays(30, branchId));

        // Rescheduling
        stats.setPendingReschedule(getPendingRescheduleCount(branchId));
        stats.setRescheduledThisMonth(getRescheduledCountThisMonth(startOfMonth, LocalDateTime.now(), branchId));

        // Collections
        stats.setCollectedToday(getCollectedTodayAmount(startOfDay, endOfDay, branchId));

        // High Risk Loans - ADD THIS
        stats.setHighRiskLoans((int) getHighRiskLoansCount(branchId));

        return stats;
    }

    @Transactional(readOnly = true)
    @Override
    public DashboardSummaryDTO getDashboardSummary(User currentUser) {
        log.info("Getting dashboard summary for user: {}", currentUser.getUsername());

        return DashboardSummaryDTO.builder()
                .stats(getLoanStats(currentUser))
                .recentActivities(getRecentActivities(10, currentUser))
                .upcomingPayments(getUpcomingPayments(7, currentUser))
                .topBorrowers(getTopBorrowers(5, currentUser))
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Map<String, Object>> getUpcomingPayments(int days, User currentUser) {
        log.info("Getting upcoming payments for next {} days for user: {}", days, currentUser.getUsername());

        Long branchId = getUserBranchId(currentUser);
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);

        List<RepaymentSchedule> schedules;
        if (branchId != null) {
            schedules = repaymentScheduleRepository.findDueBetweenAndBranch(today, endDate, branchId);
        } else {
            schedules = repaymentScheduleRepository.findDueBetween(today, endDate);
        }

        return schedules.stream()
                .limit(10)
                .map(schedule -> {
                    Map<String, Object> payment = new HashMap<>();
                    payment.put("id", schedule.getId());
                    payment.put("loanId", schedule.getLoan().getId());
                    payment.put("loanAccountNumber", schedule.getLoan().getLoanAccountNumber());
                    payment.put("borrowerName", schedule.getLoan().getBorrower().getFullName());
                    payment.put("dueDate", schedule.getDueDate().toString());
                    payment.put("amountDue", schedule.getOutstandingAmount());
                    payment.put("installmentNumber", schedule.getInstallmentNumber());
                    return payment;
                })
                .collect(Collectors.toList());
    }



    private Long getUserBranchId(User currentUser) {
        if (currentUser.getRole().equals("SUPER_ADMIN")) {
            return null;
        }
        return currentUser.getBranchId();
    }

    private BigDecimal getTotalPortfolio(Long branchId) {
        if (branchId != null) {
            return loanRepository.sumOutstandingBalanceByBranch(branchId);
        }
        return loanRepository.sumOutstandingBalance();
    }

    private BigDecimal getTotalPortfolioByDateRange(LocalDate start, LocalDate end, Long branchId) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atStartOfDay();

        if (branchId != null) {
            return loanRepository.sumOutstandingBalanceByBranchAndDateRange(branchId, startDateTime, endDateTime);
        }
        return loanRepository.sumOutstandingBalanceByDateRange(startDateTime, endDateTime);
    }

    /**
     * Get high risk loans count with fallback to delinquency calculation
     * Uses riskRating field if any loans have it set, otherwise uses delinquency
     */
    private long getHighRiskLoansCount(Long branchId) {
        long totalLoansWithRiskRating;

        // Check if any loans have risk ratings set
        if (branchId != null) {
            totalLoansWithRiskRating = loanRepository.countByRiskRatingNotNullAndBranch(branchId);
        } else {
            totalLoansWithRiskRating = loanRepository.countByRiskRatingNotNull();
        }

        // If there are loans with risk ratings, use that field
        if (totalLoansWithRiskRating > 0) {
            log.debug("Using riskRating field for high risk calculation ({} loans have ratings)",
                    totalLoansWithRiskRating);
            if (branchId != null) {
                return loanRepository.countHighRiskByBranch(branchId);
            }
            return loanRepository.countHighRisk();
        }
        // Otherwise, use delinquency-based calculation
        else {
            log.debug("No loans with risk ratings found, using delinquency-based calculation");
            if (branchId != null) {
                return loanRepository.countHighRiskByBranchAndDelinquency(branchId);
            }
            return loanRepository.countHighRiskByDelinquency();
        }
    }


    private double calculateGrowthRate(long previousPeriod, long currentPeriod) {
        if (previousPeriod == 0) return currentPeriod > 0 ? 100.0 : 0.0;
        return ((double) (currentPeriod - previousPeriod) / previousPeriod) * 100;
    }

    private int countApplicationsByStatusAndBranch(String status, Long branchId) {
        if (branchId != null) {
            return loanApplicationRepository.countByStatusAndBranchId(GeneralConfig.LoanApplicationStatus.valueOf(status), branchId);
        }
        return Math.toIntExact(loanApplicationRepository.countByStatus(GeneralConfig.LoanApplicationStatus.valueOf(status)));
    }

    private int countApprovedToday(LocalDateTime start, LocalDateTime end, Long branchId) {
        if (branchId != null) {
            return loanApplicationRepository.countByStatusAndApprovedDateBetweenAndBranchId(GeneralConfig.LoanApplicationStatus.APPROVED, start, end, branchId);
        }
        return loanApplicationRepository.countByStatusAndApprovedDateBetween(GeneralConfig.LoanApplicationStatus.APPROVED, start, end);
    }

    private int countLoansByStatusAndBranch(String status, Long branchId) {
        if (branchId != null) {
            return loanRepository.countByStatusAndBranchId(status, branchId);
        }
        return Math.toIntExact(loanRepository.countByStatus(GeneralConfig.LoanStatus.valueOf(status)));
    }

    private int countDisbursedToday(LocalDateTime start, LocalDateTime end, Long branchId) {
        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        if (branchId != null) {
            return loanRepository.countByStatusAndDisbursementDateBetweenAndBranchId(GeneralConfig.LoanStatus.ACTIVE, startDate, endDate, branchId);
        }
        return loanRepository.countByStatusAndDisbursementDateBetween(GeneralConfig.LoanStatus.ACTIVE, startDate, endDate);
    }

    private int getDueTodayCount(Long branchId) {
        LocalDate today = LocalDate.now();
        if (branchId != null) {
            return repaymentScheduleRepository.countDueTodayByBranch(today, branchId);
        }
        return Math.toIntExact(repaymentScheduleRepository.countDueToday(today));
    }

    private int getOverdueCount(Long branchId) {
        LocalDate today = LocalDate.now();
        if (branchId != null) {
            return repaymentScheduleRepository.countOverdueByBranch(today, branchId);
        }
        return repaymentScheduleRepository.countOverdue(today);
    }

    private int getOverdueCountByDays(int days, Long branchId) {
        LocalDate threshold = LocalDate.now().minusDays(days);
        if (branchId != null) {
            return repaymentScheduleRepository.countOverdueByDaysAndBranch(threshold, days, branchId);
        }
        return repaymentScheduleRepository.countOverdueByDays(threshold, days);
    }

    private int getPendingRescheduleCount(Long branchId) {
        if (branchId != null) {
            return reschedulingRequestRepository.countPendingRescheduleRequestsByBranch(branchId);
        }
        return reschedulingRequestRepository.countPendingRescheduleRequests();
    }

    private int getRescheduledCountThisMonth(LocalDateTime start, LocalDateTime end, Long branchId) {
        if (branchId != null) {
            return reschedulingRequestRepository.countApprovedBetweenAndBranch(start, end, branchId);
        }
        return reschedulingRequestRepository.countApprovedBetween(start, end);
    }

    private BigDecimal getCollectedTodayAmount(LocalDateTime start, LocalDateTime end, Long branchId) {
        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        if (branchId != null) {
            return loanRepaymentRepository.sumAmountByDateRangeAndBranch(startDate, endDate, branchId);
        }
        return loanRepaymentRepository.sumAmountByDateRange(startDate, endDate);
    }

    private BigDecimal getOnTimePaymentsForMonth(LocalDate monthStart, LocalDate monthEnd, Long branchId) {
        if (branchId != null) {
            return loanRepaymentRepository.sumOnTimePaymentsByBranch(monthStart, monthEnd, branchId);
        }
        return loanRepaymentRepository.sumOnTimePayments(monthStart, monthEnd);
    }

    private List<Map<String, Object>> getTopBorrowers(int limit, User currentUser) {
        Long branchId = getUserBranchId(currentUser);
        List<Object[]> results;

        if (branchId != null) {
            results = loanRepository.findTopBorrowersByBranch(branchId, PageRequest.of(0, limit));
        } else {
            results = loanRepository.findTopBorrowers(PageRequest.of(0, limit));
        }

        return results.stream()
                .map(row -> {
                    Map<String, Object> borrower = new HashMap<>();
                    borrower.put("borrowerId", row[0]);
                    borrower.put("borrowerName", row[1]);
                    borrower.put("totalBorrowed", row[2]);
                    borrower.put("activeLoans", row[3]);
                    return borrower;
                })
                .collect(Collectors.toList());
    }


    // In DashboardService.java - Add method to get rescheduling activities
    private RecentActivityDto convertReschedulingToActivityDTO(ReschedulingRequest request) {
        String description;
        String type = "RESCHEDULE";

        switch (request.getRequestType()) {
            case TENURE_EXTENSION:
                description = String.format("Loan %s rescheduled: Tenure extended by %d months",
                        request.getLoan().getLoanAccountNumber(),
                        request.getAdditionalMonths());
                break;
            case PAYMENT_REDUCTION:
                description = String.format("Loan %s rescheduled: Payment reduced from %s to %s",
                        request.getLoan().getLoanAccountNumber(),
                        formatCurrency(request.getCurrentMonthlyPayment()),
                        formatCurrency(request.getProposedMonthlyPayment()));
                break;
            case PAYMENT_HOLIDAY:
                description = String.format("Loan %s rescheduled: Payment holiday for %d months",
                        request.getLoan().getLoanAccountNumber(),
                        request.getHolidayMonths());
                break;
            case INTEREST_RATE_ADJUSTMENT:
                description = String.format("Loan %s rescheduled: Interest rate adjusted from %.2f%% to %.2f%%",
                        request.getLoan().getLoanAccountNumber(),
                        request.getCurrentInterestRate(),
                        request.getProposedInterestRate());
                break;
            default:
                description = String.format("Loan %s rescheduled", request.getLoan().getLoanAccountNumber());
                break;
        }

        if (request.getStatus() == ReschedulingRequest.RequestStatus.APPROVED) {
            description += " (Approved)";
        } else if (request.getStatus() == ReschedulingRequest.RequestStatus.PENDING) {
            description += " (Pending)";
        }

        return RecentActivityDto.builder()
                .id(request.getId())
                .type(type)
                .description(description)
                .timestamp(LocalDateTime.parse(request.getCreatedAt().toString()))
                .referenceNumber(request.getRequestNumber())
                .userName(request.getRequestedBy() != null ?
                        request.getRequestedBy().getUsername() : "System")
                .build();
    }



}