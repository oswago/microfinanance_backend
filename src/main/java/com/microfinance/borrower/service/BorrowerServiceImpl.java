package com.microfinance.borrower.service;

import com.microfinance.audit.service.AuditService;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.base.service.UserService;
import com.microfinance.borrower.dto.*;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.entity.BorrowerActivity;
import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.borrower.entity.DocumentVerification;
import com.microfinance.borrower.enums.KycWorkflowState;
import com.microfinance.borrower.enums.KycWorkflowStep;
import com.microfinance.borrower.repository.BorrowerActivityRepository;
import com.microfinance.borrower.repository.BorrowerDocumentRepository;
import com.microfinance.borrower.repository.BorrowerRepository;
import com.microfinance.borrower.repository.DocumentVerificationRepository;
import com.microfinance.common.config.DocumentConfig;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.entity.LoanRepayment;
import com.microfinance.loanapplications.entity.RepaymentSchedule;
import com.microfinance.loanapplications.repository.LoanRepaymentRepository;
import com.microfinance.loanapplications.repository.LoanRepository;
import com.microfinance.loanapplications.repository.RepaymentScheduleRepository;
import com.microfinance.loanapplications.service.LoanProductDocumentService;
import com.microfinance.loanapplications.service.WorkflowStepService;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.loanproducts.repository.LoanProductRepository;
import com.microfinance.system.entity.Branch;
import com.microfinance.system.repository.BranchRepository;
import com.microfinance.system.service.ActivityLogService;
import com.microfinance.system.service.SystemService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.microfinance.base.utils.SecurityUtils;


import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BorrowerServiceImpl implements BorrowerService {

    private final BorrowerActivityService borrowerActivityService;
    private final SecurityUtils securityUtils; // Inject SecurityUtils
    private final ActivityLogService activityLogService;
    private final AuditService auditService;

    private final BorrowerRepository borrowerRepository;
    private final BorrowerDocumentRepository borrowerDocumentRepository;
    private final DocumentVerificationRepository documentVerificationRepository;
    private final LoanProductRepository loanProductRepository;
    private final UserRepository userRepository;
    private final BorrowerActivityRepository borrowerActivityRepository;
    private final UserService userService; // Assuming you have a user service
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    @Autowired
    private LoanProductDocumentService loanProductDocumentService;

    @Autowired
    private WorkflowStepService workflowStepService;

    private final BranchRepository branchRepository;
    private final SystemService systemService;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.file.max-size:5242880}") // 5MB default
    private long maxFileSize;

    @Value("${app.file.allowed-types:image/jpeg,image/png,image/jpg,application/pdf}")
    private String allowedFileTypes;

    @Autowired
    private LoanRepository loanRepository;

   // @Autowired
    //private SavingsAccountRepository savingsAccountRepository;

    @Autowired
    private LoanRepaymentRepository loanRepaymentRepository;


    @Override
    public BorrowerCreditAssessmentDto assessCreditworthiness(Long borrowerId) {
        return null;
    }

    @Override
    public List<BorrowerDto> getBorrowersEligibleForLoan(Long loanProductId) {
        return List.of();
    }

    @Override
    public Boolean isBorrowerEligibleForLoan(Long borrowerId, Long loanProductId) {
        return null;
    }

    /*
    @Override
    public BorrowerPortfolioSummaryDto getPortfolioSummary(Long borrowerId) {
        return null;
    }
     */


        @Override
        public BorrowerPortfolioSummaryDto getPortfolioSummary(Long borrowerId) {
            // Verify borrower exists
            Borrower borrower = borrowerRepository.findById(borrowerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Borrower not found with id: " + borrowerId));

            BorrowerPortfolioSummaryDto summary = new BorrowerPortfolioSummaryDto();
            summary.setBorrowerId(borrowerId);
            summary.setBorrowerName(borrower.getFirstName() + " " + borrower.getLastName());

            // Get loan statistics
            List<Loan> borrowerLoans = loanRepository.findByBorrowerId(borrowerId);

            // Active loans count
            long activeLoansCount = borrowerLoans.stream()
                    .filter(loan -> loan.getStatus() == GeneralConfig.LoanStatus.ACTIVE || loan.getStatus() == GeneralConfig.LoanStatus.APPROVED)
                    .count();
            summary.setActiveLoans((int) activeLoansCount);

            // Completed loans count
            long completedLoansCount = borrowerLoans.stream()
                    .filter(loan -> loan.getStatus() == GeneralConfig.LoanStatus.COMPLETED || loan.getStatus() == GeneralConfig.LoanStatus.CLOSED)
                    .count();
            summary.setCompletedLoans((int) completedLoansCount);

            // Total borrowed amount
            BigDecimal totalBorrowed = borrowerLoans.stream()
                    .map(Loan::getNetDisbursementAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setTotalBorrowed(totalBorrowed);

            // Total repaid and interest paid
            BigDecimal totalRepaid = BigDecimal.ZERO;
            BigDecimal totalInterestPaid = BigDecimal.ZERO;

            for (Loan loan : borrowerLoans) {
                List<LoanRepayment> repayments = loanRepaymentRepository.findByLoanId(loan.getId());
                BigDecimal loanTotalRepaid = repayments.stream()
                        .map(LoanRepayment::getAmountPaid)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                totalRepaid = totalRepaid.add(loanTotalRepaid);

                // Calculate interest paid (this might need adjustment based on your business logic)
                BigDecimal loanInterestPaid = repayments.stream()
                        .map(LoanRepayment::getInterestAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                totalInterestPaid = totalInterestPaid.add(loanInterestPaid);
            }
            summary.setTotalRepaid(totalRepaid);
            summary.setTotalInterestPaid(totalInterestPaid);

            // Outstanding balance
            BigDecimal outstandingBalance = BigDecimal.ZERO;
            for (Loan loan : borrowerLoans) {
                if (loan.getStatus() == GeneralConfig.LoanStatus.ACTIVE || loan.getStatus() == GeneralConfig.LoanStatus.APPROVED) {
                    BigDecimal totalDue = loan.getTotalDue(); // Assuming you have this field
                    BigDecimal paidAmount = loanRepaymentRepository.findByLoanId(loan.getId()).stream()
                            .map(LoanRepayment::getAmountPaid)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    outstandingBalance = outstandingBalance.add(totalDue.subtract(paidAmount));
                }
            }
            summary.setOutstandingBalance(outstandingBalance);

            // Total savings
           /* BigDecimal totalSavings = savingsAccountRepository.findByBorrowerId(borrowerId).stream()
                    .map(SavingsAccount::getCurrentBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            */
            summary.setTotalSavings(BigDecimal.ZERO);

            // Last loan date
            Optional<Loan> lastLoan = borrowerLoans.stream()
                    .max(Comparator.comparing(Loan::getCreatedAt));
            summary.setLastLoanDate(lastLoan.map(Loan::getCreatedAt).orElse(null));

            // Repayment behavior
            summary.setRepaymentBehavior(calculateRepaymentBehavior(borrowerId, borrowerLoans));

            return summary;
        }

        private String calculateRepaymentBehavior(Long borrowerId, List<Loan> loans) {
            if (loans.isEmpty()) {
                return "NO_HISTORY";
            }

            long totalLoans = loans.size();
            long onTimeLoans = 0;

            for (Loan loan : loans) {
                if (isLoanRepaidOnTime(loan)) {
                    onTimeLoans++;
                }
            }

            double onTimePercentage = (double) onTimeLoans / totalLoans * 100;

            if (onTimePercentage >= 90) return "GOOD";
            if (onTimePercentage >= 70) return "AVERAGE";
            return "POOR";
        }

        private boolean isLoanRepaidOnTime(Loan loan) {
            // Implement your logic to determine if loan was repaid on time
            // This might check for late payments, default history, etc.
            if (loan.getStatus() != GeneralConfig.LoanStatus.COMPLETED) {
                return false; // Only consider completed loans
            }
            // Example logic - adjust based on your business rules
            List<LoanRepayment> repayments = loanRepaymentRepository.findByLoanId(loan.getId());
            long latePayments = repayments.stream()
                    .filter(repayment -> repayment.getPaymentDate().isAfter(loan.getNextPaymentDueDate()))
                    .count();

            return latePayments == 0;
        }

    @Override
    public List<BorrowerActivityDto> getRecentActivities(Long borrowerId) {
        return List.of();
    }

    @Override
    @Transactional
    public BorrowerActivityDto logActivity(BorrowerActivityDto activityDto) {
        log.info("Logging activity for borrower: {}, type: {}",
                activityDto.getBorrowerId(), activityDto.getActivityType());

        try {
            Borrower borrower = borrowerRepository.findById(activityDto.getBorrowerId())
                    .orElseThrow(() -> new RuntimeException("Borrower not found with id: " + activityDto.getBorrowerId()));

            BorrowerActivity activity = new BorrowerActivity();
            activity.setBorrower(borrower);
            activity.setActivityType(activityDto.getActivityType());
            activity.setDescription(activityDto.getDescription());
            activity.setDetails(activityDto.getDetails());
            activity.setActivityDate(activityDto.getActivityDate() != null ?
                    activityDto.getActivityDate() : LocalDateTime.now());
            activity.setPerformedBy(activityDto.getPerformedBy());

            // Get performer name if performedBy is provided but performedByName is not
            if (activityDto.getPerformedBy() != null && activityDto.getPerformedByName() == null) {
                try {
                    User performer = userRepository.findById(activityDto.getPerformedBy()).orElse(null);
                    if (performer != null) {
                        activity.setPerformedByName(performer.getFirstName() + " " +
                                (performer.getLastName() != null ? performer.getLastName() : ""));
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch performer name for user ID: {}", activityDto.getPerformedBy());
                }
            } else {
                activity.setPerformedByName(activityDto.getPerformedByName());
            }

            activity.setReferenceType(activityDto.getReferenceType());
            activity.setReferenceId(activityDto.getReferenceId());
            activity.setReferenceNumber(activityDto.getReferenceNumber());
            activity.setBranchName(activityDto.getBranchName());
            activity.setIpAddress(activityDto.getIpAddress());
            activity.setUserAgent(activityDto.getUserAgent());
            activity.setSessionId(activityDto.getSessionId());

            BorrowerActivity savedActivity = borrowerActivityRepository.save(activity);
            log.info("Activity logged successfully with id: {}", savedActivity.getId());

            return BorrowerActivityDto.fromEntity(savedActivity);

        } catch (Exception e) {
            log.error("Error logging activity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to log activity", e);
        }
    }



    @Override
    public Page<BorrowerActivityDto> getBorrowerActivities(Long borrowerId, Pageable pageable) {
        log.info("Fetching activities for borrower: {}, page: {}, size: {}",
                borrowerId, pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<BorrowerActivity> activities = borrowerActivityRepository.findByBorrowerId(borrowerId, pageable);
            return activities.map(BorrowerActivityDto::fromEntity);

        } catch (Exception e) {
            log.error("Error fetching activities for borrower {}: {}", borrowerId, e.getMessage(), e);
            return Page.empty(pageable);
        }
    }



    @Override
    public Page<BorrowerActivityDto> searchActivities(ActivitySearchCriteria criteria, Pageable pageable) {
        log.info("Searching activities with criteria: {}", criteria);
        try {
            Page<BorrowerActivity> activities;

            if (criteria.getActivityType() != null) {
                activities = borrowerActivityRepository.findByBorrowerIdAndActivityType(
                        criteria.getBorrowerId(),
                        GeneralConfig.BorrowerActivityType.valueOf(criteria.getActivityType().name()),
                        pageable);
            } else if (criteria.getStartDate() != null && criteria.getEndDate() != null) {
                activities = borrowerActivityRepository.findByBorrowerIdAndActivityDateBetween(
                        criteria.getBorrowerId(),
                        criteria.getStartDate().atStartOfDay(),
                        criteria.getEndDate().atTime(23, 59, 59),
                        pageable);
            } else {
                activities = borrowerActivityRepository.findByBorrowerId(criteria.getBorrowerId(), pageable);
            }

            return activities.map(BorrowerActivityDto::fromEntity);

        } catch (Exception e) {
            log.error("Error searching activities: {}", e.getMessage(), e);
            return Page.empty(pageable);
        }
    }


    @Override
    public BorrowerActivitySummaryDto getActivitySummary(Long borrowerId, LocalDate startDate, LocalDate endDate) {
        log.info("Getting activity summary for borrower {} from {} to {}", borrowerId, startDate, endDate);

        try {
            // Set default dates if not provided (last 30 days)
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(30);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);

            // Fetch borrower
            Optional<Borrower> borrowerOpt = borrowerRepository.findById(borrowerId);
            if (borrowerOpt.isEmpty()) {
                log.info("Borrower not found with id: {}", borrowerId);
                return createEmptySummary(borrowerId, startDate, endDate);
            }

            Borrower borrower = borrowerOpt.get();
            String fullName = borrower.getFullName();
            String firstName= borrower.getFirstName();
            String lastName= borrower.getLastName();


            // ===== 1. Get counts by activity type =====
            List<Object[]> typeCounts = borrowerActivityRepository.countByBorrowerIdAndActivityTypeBetween(
                    borrowerId, start, end);

            Map<String, Integer> activityCounts = new HashMap<>();
            Map<BorrowerActivityDto.ActivityType, Integer> activityCountByType = new HashMap<>();

            int totalLoanActivitiesCount = 0;
            int totalRepaymentActivitiesCount = 0;
            int totalKycActivitiesCount = 0;
            int totalSavingsActivitiesCount = 0;

            String mostFrequentActivity = null;
            int maxCount = 0;

            for (Object[] row : typeCounts) {
                String activityType = row[0].toString();
                int count = ((Long) row[1]).intValue();

                activityCounts.put(activityType, count);

                // Map to enum if possible
                try {
                    BorrowerActivityDto.ActivityType enumType = BorrowerActivityDto.ActivityType.valueOf(activityType);
                    activityCountByType.put(enumType, count);
                } catch (IllegalArgumentException e) {
                    log.debug("Activity type {} not in enum, skipping", activityType);
                }

                // Categorize activities
                if (activityType.contains("LOAN") || activityType.contains("APPLICATION")) {
                    totalLoanActivitiesCount += count;
                } else if (activityType.contains("REPAYMENT")) {
                    totalRepaymentActivitiesCount += count;
                } else if (activityType.contains("KYC") || activityType.contains("VERIFIED")) {
                    totalKycActivitiesCount += count;
                } else if (activityType.contains("SAVING")) {
                    totalSavingsActivitiesCount += count;
                }

                // Track most frequent activity
                if (count > maxCount) {
                    maxCount = count;
                    mostFrequentActivity = activityType;
                }
            }

            // ===== 2. Get total activities =====
            long totalActivities = borrowerActivityRepository.countByBorrowerIdAndActivityDateBetween(
                    borrowerId, start, end);

            // ===== 2.5 Calculate activities for this week and this month =====
            LocalDate today = LocalDate.now();
            LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY);
            LocalDate startOfMonth = today.withDayOfMonth(1);

            LocalDateTime startOfWeekDateTime = startOfWeek.atStartOfDay();
            LocalDateTime startOfMonthDateTime = startOfMonth.atStartOfDay();
            LocalDateTime nowDateTime = LocalDateTime.now();

            long activitiesThisWeek = borrowerActivityRepository.countByBorrowerIdAndActivityDateBetween(
                    borrowerId, startOfWeekDateTime, nowDateTime);
            long activitiesThisMonth = borrowerActivityRepository.countByBorrowerIdAndActivityDateBetween(
                    borrowerId, startOfMonthDateTime, nowDateTime);


            // ===== 3. Get recent activities (last 5) =====
            List<BorrowerActivity> recent = borrowerActivityRepository.findByBorrowerIdAndActivityDateBetweenOrderByActivityDateDesc(
                    borrowerId, start, end, PageRequest.of(0, 5));

            List<BorrowerActivityDto> recentActivities = recent.stream()
                    .map(this::convertToActivityDto)
                    .collect(Collectors.toList());

            // ===== 4. Get detailed activity lists =====
            List<BorrowerActivityDto> loanActivitiesList = getLoanActivities(borrowerId, start, end);
            List<BorrowerActivityDto> repaymentActivitiesList = getRepaymentActivities(borrowerId, start, end);
            List<BorrowerActivityDto> kycActivitiesList = getKycActivities(borrowerId, start, end);
            List<BorrowerActivityDto> savingsActivitiesList = getSavingsActivities(borrowerId, start, end);

            // ===== 5. Get last activity info =====
            LocalDate lastActivityDate = null;
            String lastActivityType = null;

            if (!recent.isEmpty()) {
                BorrowerActivity lastActivity = recent.get(0);
                lastActivityDate = lastActivity.getActivityDate().toLocalDate();
                lastActivityType = String.valueOf(lastActivity.getActivityType());
            }

            // ===== 6. Get loan performance metrics =====
            Double onTimeRepaymentRate = calculateOnTimeRepaymentRate(borrowerId);

            // ===== 7. Get meeting attendance (if applicable for group borrowers) =====
            int meetingsAttended = 0;
            int meetingsMissed = 0;

            if (borrower.getGroup()!= null) {
                Object[] meetingStats = getMeetingAttendanceStats(borrowerId);
                if (meetingStats != null) {
                    meetingsAttended = ((Number) meetingStats[0]).intValue();
                    meetingsMissed = ((Number) meetingStats[1]).intValue();
                }
            }

            // ===== 8. Calculate activity trend =====
            String activityTrend = calculateActivityTrend(borrowerId, startDate, endDate);

            return BorrowerActivitySummaryDto.builder()
                    .borrowerId(borrowerId)
                    .borrowerName(fullName)
                    .borrowerLastName(lastName)
                    .borrowerFirstName(firstName)
                    .summaryDate(LocalDate.now())
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalActivities((int) totalActivities)

                    // Count fields
                    .loanActivitiesCount(totalLoanActivitiesCount)
                    .repaymentActivitiesCount(totalRepaymentActivitiesCount)
                    .kycActivitiesCount(totalKycActivitiesCount)
                    .savingsActivitiesCount(totalSavingsActivitiesCount)
                    .activitiesThisMonth((int) activitiesThisMonth)
                    .activitiesThisWeek((int) activitiesThisWeek)

                    // List fields
                    .loanActivitiesList(loanActivitiesList)
                    .repaymentActivitiesList(repaymentActivitiesList)
                    .kycActivitiesList(kycActivitiesList)
                    .savingsActivitiesList(savingsActivitiesList)

                    // For backward compatibility (set same values)
                    .loanActivities(totalLoanActivitiesCount)
                    .repaymentActivities(totalRepaymentActivitiesCount)
                    .kycActivities(totalKycActivitiesCount)
                    .savingsActivities(totalSavingsActivitiesCount)

                    .activityCounts(activityCounts)
                    .activityCountByType(activityCountByType)
                    .mostFrequentActivity(mostFrequentActivity)
                    .lastActivityDate(lastActivityDate)
                    .lastActivityType(lastActivityType)
                    .onTimeRepaymentRate(onTimeRepaymentRate)
                    .meetingsAttended(meetingsAttended)
                    .meetingsMissed(meetingsMissed)
                    .activityTrend(activityTrend)
                    .recentActivities(recentActivities)
                    .build();

        } catch (Exception e) {
            log.error("Error getting activity summary: {}", e.getMessage(), e);
            return createEmptySummary(borrowerId, startDate, endDate);
        }
    }

    private BorrowerActivitySummaryDto createEmptySummary(Long borrowerId, LocalDate startDate, LocalDate endDate) {
        return BorrowerActivitySummaryDto.builder()
                .borrowerId(borrowerId)
                .borrowerName("Unknown")
                .summaryDate(LocalDate.now())
                .startDate(startDate)
                .endDate(endDate)
                .totalActivities(0)
                .activitiesThisWeek(0)
                .activitiesThisMonth(0)
                .loanActivitiesCount(0)
                .repaymentActivitiesCount(0)
                .kycActivitiesCount(0)
                .savingsActivitiesCount(0)
                .loanActivities(0)
                .repaymentActivities(0)
                .kycActivities(0)
                .savingsActivities(0)
                .loanActivitiesList(new ArrayList<>())
                .repaymentActivitiesList(new ArrayList<>())
                .kycActivitiesList(new ArrayList<>())
                .savingsActivitiesList(new ArrayList<>())
                .activityCounts(new HashMap<>())
                .activityCountByType(new HashMap<>())
                .recentActivities(new ArrayList<>())
                .build();
    }

    private BorrowerActivityDto convertToActivityDto(BorrowerActivity activity) {
        BorrowerActivityDto dto = BorrowerActivityDto.fromEntity(activity);

        // Fetch performer name if performer ID exists
        if (dto.getPerformedBy() != null) {
            try {
                User performer = userRepository.findById(dto.getPerformedBy()).orElse(null);
                if (performer != null) {
                    dto.setPerformedByName(performer.getFirstName() + " " +
                            (performer.getLastName() != null ? performer.getLastName() : ""));
                }
            } catch (Exception e) {
                log.warn("Could not fetch performer name for user ID: {}", dto.getPerformedBy());
            }
        }

        return dto;
    }

    private Double calculateOnTimeRepaymentRate(Long borrowerId) {
        try {
            // Get all loans for this borrower
            List<Loan> loans = loanRepository.findByBorrowerId(borrowerId);
            if (loans == null || loans.isEmpty()) {
                return null;
            }

            int totalInstallments = 0;
            int onTimeInstallments = 0;

            for (Loan loan : loans) {
                List<RepaymentSchedule> schedules = repaymentScheduleRepository.findByLoanId(loan.getId());
                for (RepaymentSchedule schedule : schedules) {
                    totalInstallments++;
                    if (schedule.getDueDate() != null && schedule.getPaidDate() != null) {
                        if (!schedule.getPaidDate().isAfter(schedule.getDueDate())) {
                            onTimeInstallments++;
                        }
                    }
                }
            }

            if (totalInstallments == 0) return null;
            return (double) onTimeInstallments / totalInstallments * 100;

        } catch (Exception e) {
            log.warn("Error calculating on-time repayment rate: {}", e.getMessage());
            return null;
        }
    }

    private Object[] getMeetingAttendanceStats(Long borrowerId) {
        // Implement based on your meeting attendance tracking
        // This is a placeholder - query your meeting attendance repository
        try {
            // Example query:
            // return groupMeetingRepository.getAttendanceStats(borrowerId);
            return null;
        } catch (Exception e) {
            log.warn("Error getting meeting attendance: {}", e.getMessage());
            return null;
        }
    }

    private String calculateActivityTrend(Long borrowerId, LocalDate startDate, LocalDate endDate) {
        try {
            // Calculate period length in days
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            long previousStartDays = daysBetween;
            long previousEndDays = 0;

            LocalDate previousStartDate = startDate.minusDays(previousStartDays);
            LocalDate previousEndDate = startDate.minusDays(1);

            LocalDateTime previousStart = previousStartDate.atStartOfDay();
            LocalDateTime previousEnd = previousEndDate.atTime(23, 59, 59);

            long currentCount = borrowerActivityRepository.countByBorrowerIdAndActivityDateBetween(
                    borrowerId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
            long previousCount = borrowerActivityRepository.countByBorrowerIdAndActivityDateBetween(
                    borrowerId, previousStart, previousEnd);

            if (previousCount == 0) {
                return currentCount > 0 ? "NEW" : "NO_ACTIVITY";
            }

            double percentChange = ((double) currentCount - previousCount) / previousCount * 100;

            if (percentChange > 20) return "INCREASING";
            if (percentChange < -20) return "DECREASING";
            return "STABLE";

        } catch (Exception e) {
            log.warn("Error calculating activity trend: {}", e.getMessage());
            return "UNKNOWN";
        }
    }

    private List<BorrowerActivityDto> getLoanActivities(Long borrowerId, LocalDateTime start, LocalDateTime end) {
        try {
            List<BorrowerActivity> activities = borrowerActivityRepository
                    .findByBorrowerIdAndActivityTypeInAndActivityDateBetween(
                            borrowerId,
                            List.of("LOAN_APPLICATION_SUBMITTED", "LOAN_APPLICATION_APPROVED",
                                    "LOAN_DISBURSED", "LOAN_CREATED"),
                            start, end, PageRequest.of(0, 20));

            return activities.stream()
                    .map(this::convertToActivityDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error getting loan activities: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<BorrowerActivityDto> getRepaymentActivities(Long borrowerId, LocalDateTime start, LocalDateTime end) {
        try {
            List<BorrowerActivity> activities = borrowerActivityRepository
                    .findByBorrowerIdAndActivityTypeInAndActivityDateBetween(
                            borrowerId,
                            List.of("REPAYMENT_MADE", "REPAYMENT_RECEIVED", "INSTALLMENT_PAID"),
                            start, end, PageRequest.of(0, 20));

            return activities.stream()
                    .map(this::convertToActivityDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error getting repayment activities: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<BorrowerActivityDto> getKycActivities(Long borrowerId, LocalDateTime start, LocalDateTime end) {
        try {
            List<BorrowerActivity> activities = borrowerActivityRepository
                    .findByBorrowerIdAndActivityTypeInAndActivityDateBetween(
                            borrowerId,
                            List.of("BORROWER_KYC_VERIFIED", "KYC_UPDATED", "KYC_EXPIRED"),
                            start, end, PageRequest.of(0, 20));

            return activities.stream()
                    .map(this::convertToActivityDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error getting KYC activities: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<BorrowerActivityDto> getSavingsActivities(Long borrowerId, LocalDateTime start, LocalDateTime end) {
        try {
            List<BorrowerActivity> activities = borrowerActivityRepository
                    .findByBorrowerIdAndActivityTypeInAndActivityDateBetween(
                            borrowerId,
                            List.of("SAVINGS_DEPOSIT", "SAVINGS_WITHDRAWAL", "SAVINGS_CREATED"),
                            start, end, PageRequest.of(0, 20));

            return activities.stream()
                    .map(this::convertToActivityDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error getting savings activities: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<BorrowerActivityDto.TimelineGroup> getActivityTimeline(Long borrowerId, int days) {
        log.info("Getting activity timeline for borrower {} for last {} days", borrowerId, days);

        try {
            LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
            LocalDateTime toDate = LocalDateTime.now();

            List<BorrowerActivity> activities = borrowerActivityRepository.findByBorrowerIdAndActivityDateBetweenOrderByActivityDateDesc(
                    borrowerId, fromDate, toDate, PageRequest.of(0, 100));


            // Group by date
            Map<String, List<BorrowerActivityDto>> groupedByDate = activities.stream()
                    .map(BorrowerActivityDto::fromEntity)
                    .collect(Collectors.groupingBy(
                            activity -> activity.getActivityDate().toLocalDate().toString(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            // Create timeline groups
            List<BorrowerActivityDto.TimelineGroup> timeline = new ArrayList<>();
            for (Map.Entry<String, List<BorrowerActivityDto>> entry : groupedByDate.entrySet()) {
                timeline.add(new BorrowerActivityDto.TimelineGroup(entry.getKey(), entry.getValue()));

            }

            return timeline;

        } catch (Exception e) {
            log.error("Error getting activity timeline: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    @Override
    public List<BorrowerActivityDto> getRecentActivities(Long borrowerId, int limit) {
        log.info("Fetching recent activities for borrower ID: {}, limit: {}", borrowerId, limit);

        List<BorrowerActivityDto> activities = new ArrayList<>();

        try {
            // Get borrower to check if exists
            Borrower borrower = borrowerRepository.findById(borrowerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));

            // 1. Get recent loans (applications)
            List<Loan> recentLoans = loanRepository.findByBorrowerIdOrderByCreatedAtDesc(borrowerId, PageRequest.of(0, limit));
            for (Loan loan : recentLoans) {
                BorrowerActivityDto activity = BorrowerActivityDto.builder()
                        .id(loan.getId())
                        .borrowerId(borrowerId)
                        .borrowerName(borrower.getFullName())
                        .activityType(GeneralConfig.BorrowerActivityType.LOAN_APPLICATION_SUBMITTED)
                        .description(String.format("Loan application submitted - %s for KES %,.2f",
                                loan.getLoanAccountNumber(), loan.getPrincipalAmount()))
                        .activityDate(loan.getCreatedAt())
                        .performedBy(loan.getCreatedBy())
                        .referenceType("LOAN")
                        .referenceId(loan.getId())
                        .referenceNumber(loan.getLoanAccountNumber())
                        .build();
                activities.add(activity);
            }

            // 2. Get recent loan disbursements
            List<Loan> recentDisbursements = loanRepository.findByBorrowerIdAndDisbursementDateNotNullOrderByDisbursementDateDesc(
                    borrowerId, PageRequest.of(0, limit));
            for (Loan loan : recentDisbursements) {
                BorrowerActivityDto activity = BorrowerActivityDto.builder()
                        .id(loan.getId())
                        .borrowerId(borrowerId)
                        .borrowerName(borrower.getFullName())
                        .activityType(GeneralConfig.BorrowerActivityType.LOAN_DISBURSED)
                        .description(String.format("Loan disbursed - %s amount KES %,.2f",
                                loan.getLoanAccountNumber(), loan.getNetDisbursementAmount()))
                        .activityDate(loan.getDisbursementDate().atStartOfDay())
                        .performedBy(loan.getDisbursedBy() != null ? loan.getDisbursedBy().getId() : null)
                        .referenceType("LOAN")
                        .referenceId(loan.getId())
                        .referenceNumber(loan.getLoanAccountNumber())
                        .build();
                activities.add(activity);
            }

            // 3. Get recent repayments
            List<LoanRepayment> recentRepayments = loanRepaymentRepository.findByLoanBorrowerIdOrderByCreatedAtDesc(
                    borrowerId, PageRequest.of(0, limit));
            for (LoanRepayment repayment : recentRepayments) {
                BorrowerActivityDto activity = BorrowerActivityDto.builder()
                        .id(repayment.getId())
                        .borrowerId(borrowerId)
                        .borrowerName(borrower.getFullName())
                        .activityType(GeneralConfig.BorrowerActivityType.REPAYMENT_MADE)
                        .description(String.format("Loan repayment received - Receipt %s amount KES %,.2f",
                                repayment.getReceiptNumber(), repayment.getAmountPaid()))
                        .activityDate(repayment.getCreatedAt())
                        .performedBy(repayment.getReceivedBy() != null ? repayment.getReceivedBy().getId() : null)
                        .referenceType("REPAYMENT")
                        .referenceId(repayment.getId())
                        .referenceNumber(repayment.getReceiptNumber())
                        .build();
                activities.add(activity);
            }

            // 4. Get recent document uploads
            List<BorrowerDocument> recentDocuments = borrowerDocumentRepository.findByBorrowerIdOrderByCreatedAtDesc(
                    borrowerId, PageRequest.of(0, limit));
            for (BorrowerDocument doc : recentDocuments) {
                BorrowerActivityDto activity = BorrowerActivityDto.builder()
                        .id(doc.getId())
                        .borrowerId(borrowerId)
                        .borrowerName(borrower.getFullName())
                        .activityType(GeneralConfig.BorrowerActivityType.DOCUMENT_UPLOADED)
                        .description(String.format("Document uploaded - %s (%s)",
                                doc.getDocumentName(), doc.getDocumentType()))
                        .activityDate(doc.getCreatedAt())
                        .performedBy(doc.getCreatedBy())
                        .referenceType("DOCUMENT")
                        .referenceId(doc.getId())
                        .build();
                activities.add(activity);
            }

            // 5. Add KYC verification activity if verified
            if (borrower.getKycVerifiedAt() != null) {
                BorrowerActivityDto activity = BorrowerActivityDto.builder()
                        .id(borrowerId)
                        .borrowerId(borrowerId)
                        .borrowerName(borrower.getFullName())
                        .activityType(GeneralConfig.BorrowerActivityType.BORROWER_KYC_VERIFIED)
                        .description("KYC verification completed successfully")
                        .activityDate(borrower.getKycVerifiedAt())
                        .performedBy(borrower.getKycVerifiedBy())
                        .build();
                activities.add(activity);
            }

            // Sort by activity date descending
            activities.sort((a, b) -> b.getActivityDate().compareTo(a.getActivityDate()));

            // Fetch performer names
            for (BorrowerActivityDto activity : activities) {
                if (activity.getPerformedBy() != null) {
                    try {
                        User performer = userRepository.findById(activity.getPerformedBy()).orElse(null);
                        if (performer != null) {
                            activity.setPerformedByName(performer.getFirstName() + " " +
                                    (performer.getLastName() != null ? performer.getLastName() : ""));
                        }
                    } catch (Exception e) {
                        log.warn("Could not fetch performer name for user ID: {}", activity.getPerformedBy());
                    }
                }
            }

            return activities.stream().limit(limit).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching recent activities for borrower {}: {}", borrowerId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "KES 0";
        return String.format("KES %,.2f", amount);
    }

    @Override
    public BorrowerActivityDto logStandardActivity(Long borrowerId, GeneralConfig.BorrowerActivityType activityType, String description, Long performedBy, String referenceType, Long referenceId) {
        return BorrowerService.super.logStandardActivity(borrowerId, activityType, description, performedBy, referenceType, referenceId);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<BorrowerDto> getAllBorrowers(Pageable pageable) {
        return borrowerRepository.findAll(pageable).map(this::convertToDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<BorrowerDto> getBorrowersByBranch(Long branchId, Pageable pageable) {
        return borrowerRepository.findByBranchId(branchId, pageable).map(this::convertToDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<BorrowerDto> searchBorrowers(String search, Pageable pageable) {
        return borrowerRepository.searchBorrowers(search, pageable).map(this::convertToDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BorrowerDto getBorrowerById(Long id) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + id));
        return convertToDto(borrower);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BorrowerDto getBorrowerByNumber(String borrowerNumber) {
        Borrower borrower = borrowerRepository.findByBorrowerNumber(borrowerNumber)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with number: " + borrowerNumber));
        return convertToDto(borrower);
    }
    
    @Override
    @Transactional
    public BorrowerDto createBorrower(BorrowerDto borrowerDto, Long createdBy) {
        log.info("Created  new borrower Details: {} ", borrowerDto);

        // Validate unique constraints
        if (borrowerRepository.existsByPhoneNumber(borrowerDto.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists: " + borrowerDto.getPhoneNumber());
        }
        
        if (borrowerDto.getIdentificationNumber() != null && 
            borrowerRepository.existsByIdentificationNumber(borrowerDto.getIdentificationNumber())) {
            throw new IllegalArgumentException("Identification number already exists: " + borrowerDto.getIdentificationNumber());
        }
        
        Borrower borrower = convertToEntity(borrowerDto);
        
        // Generate borrower number
        String borrowerNumber = systemService.getNextNumber("BORROWER");
        borrower.setBorrowerNumber(borrowerNumber);
        
        // Set branch
        if (borrowerDto.getBranchId() != null) {
            Branch branch = branchRepository.findById(borrowerDto.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Branch not found with id: " + borrowerDto.getBranchId()));
            borrower.setBranch(branch);
        }

        // === FIX: Set loan product using loanProductId ===
        if (borrowerDto.getLoanProductId() != null) {
            LoanProduct loanProduct = loanProductRepository.findById(borrowerDto.getLoanProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Loan product not found with id: " + borrowerDto.getLoanProductId()));
            borrower.setLoanProduct(loanProduct);
            log.info("Set loan product: {} for borrower", loanProduct.getName());
        }
        borrower.setCreatedBy(createdBy);
        borrower.setStatus(GeneralConfig.BorrowerStatus.ACTIVE);
        borrower.setKycStatus(GeneralConfig.KycStatus.PENDING);
        
        Borrower savedBorrower = borrowerRepository.save(borrower);
        log.info("Created new borrower: {} with number: {}", savedBorrower.getFullName(), borrowerNumber);


        Optional<User> currentUser = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser.isPresent()){
            createdByName=currentUser.get().getFullName();
            createdById=currentUser.get().getId();
        }

        if (Objects.nonNull(savedBorrower.getId())) {
                auditLogs(
                        savedBorrower.getId(),
                        GeneralConfig.BorrowerActivityType.BORROWER_CREATED,
                        "BORROWER",
                        "Borrower with Id : "+savedBorrower.getId()+ " has been CREATED By : "+createdByName
                );
        }



        return convertToDto(savedBorrower);
    }



    
    @Override
    @Transactional
    public BorrowerDto updateBorrower(Long id, BorrowerDto borrowerDto) {
        Borrower existingBorrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + id));

        Optional<User> currentUser = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;

        if(currentUser.isPresent()){
            createdByName=currentUser.get().getFullName();
             createdById=currentUser.get().getId();
        }

        // Update fields
        existingBorrower.setFirstName(borrowerDto.getFirstName());
        existingBorrower.setLastName(borrowerDto.getLastName());
        existingBorrower.setMiddleName(borrowerDto.getMiddleName());
        existingBorrower.setGender(borrowerDto.getGender());
        existingBorrower.setDateOfBirth(borrowerDto.getDateOfBirth());
        existingBorrower.setPhoneNumber(borrowerDto.getPhoneNumber());
        existingBorrower.setEmail(borrowerDto.getEmail());
        existingBorrower.setAddress(borrowerDto.getAddress());
        existingBorrower.setCity(borrowerDto.getCity());
        existingBorrower.setState(borrowerDto.getState());
        existingBorrower.setCountry(borrowerDto.getCountry());
        existingBorrower.setPostalCode(borrowerDto.getPostalCode());
        existingBorrower.setMaritalStatus(borrowerDto.getMaritalStatus());
        existingBorrower.setOccupation(borrowerDto.getOccupation());
        existingBorrower.setEmployer(borrowerDto.getEmployer());
        existingBorrower.setMonthlyIncome(borrowerDto.getMonthlyIncome());
        existingBorrower.setEmergencyContactName(borrowerDto.getEmergencyContactName());
        existingBorrower.setEmergencyContactPhone(borrowerDto.getEmergencyContactPhone());
        existingBorrower.setEmergencyContactRelationship(borrowerDto.getEmergencyContactRelationship());
        existingBorrower.setNationality(borrowerDto.getNationality());
        existingBorrower.setIdentificationType(borrowerDto.getIdentificationType());
        existingBorrower.setIdentificationNumber(borrowerDto.getIdentificationNumber());
        existingBorrower.setNotes(borrowerDto.getNotes());
        
        // Update branch if changed
        if (borrowerDto.getBranchId() != null && 
            (existingBorrower.getBranch() == null || !existingBorrower.getBranch().getId().equals(borrowerDto.getBranchId()))) {
            Branch branch = branchRepository.findById(borrowerDto.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Branch not found with id: " + borrowerDto.getBranchId()));
            existingBorrower.setBranch(branch);
        }
        
        Borrower updatedBorrower = borrowerRepository.save(existingBorrower);
        log.info("Updated borrower: {} with id: {}", updatedBorrower.getFullName(), id);

        if (Objects.nonNull(updatedBorrower.getId())) {
                 auditLogs(
                         updatedBorrower.getId(),
                          GeneralConfig.BorrowerActivityType.BORROWER_UPDATED,
                    "BORROWER",
                     "Borrower with Id : "+updatedBorrower.getId()+ "has been UPDATED By: "+createdByName
                 );
        }

        return convertToDto(updatedBorrower);
    }
    
    @Override
    @Transactional
    public void deleteBorrower(Long id) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + id));
        
        // Soft delete - set status to INACTIVE
        borrower.setStatus(GeneralConfig.BorrowerStatus.INACTIVE);
       Borrower borrowerDelete= borrowerRepository.save(borrower);


        Optional<User> currentUser = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser.isPresent()){
            createdByName=currentUser.get().getFullName();
            createdById=currentUser.get().getId();
        }

        if (Objects.nonNull(borrowerDelete.getId())) {
            auditLogs(
                    borrowerDelete.getId(),
                    GeneralConfig.BorrowerActivityType.BORROWER_UPDATED,
                    "BORROWER",
                    "Borrower with Id :"+borrowerDelete.getId()+ "Has been DEACTIVATED By: "+createdByName
            );
        }

        
        log.info("Soft deleted borrower: {} with id: {}", borrower.getFullName(), id);
    }
    
    @Override
    @Transactional
    public BorrowerDto updateBorrowerStatus(Long id, GeneralConfig.BorrowerStatus status) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + id));
        
        borrower.setStatus(status);
        Borrower updatedBorrower = borrowerRepository.save(borrower);

        //Audit Section
        Optional<User> currentUser = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser.isPresent()){
            createdByName=currentUser.get().getFullName();
            createdById=currentUser.get().getId();
        }
        if (Objects.nonNull(updatedBorrower.getId())) {
            auditLogs(
                    updatedBorrower.getId(),
                    GeneralConfig.BorrowerActivityType.BORROWER_UPDATED,
                    "BORROWER",
                    "Borrower with Id : "+updatedBorrower.getId()+ " Status has been UPDATED to: "+status+ " By user:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section
        
        log.info("Updated borrower status to {} for borrower: {} with id: {}", status, borrower.getFullName(), id);
        
        return convertToDto(updatedBorrower);
    }
    
    @Override
    @Transactional
    public BorrowerDto updateKycStatus(Long id, GeneralConfig.KycStatus kycStatus, Long verifiedBy, String notes) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + id));
        
        borrower.setKycStatus(kycStatus);
        borrower.setKycVerifiedBy(verifiedBy);
        borrower.setKycVerifiedAt(LocalDateTime.now());
        
        Borrower updatedBorrower = borrowerRepository.save(borrower);



        //Audit Section
        Optional<User> currentUser = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser.isPresent()){
            createdByName=currentUser.get().getFullName();
            createdById=currentUser.get().getId();
        }
        if (Objects.nonNull(updatedBorrower.getId())) {
            auditLogs(
                    updatedBorrower.getId(),
                    GeneralConfig.BorrowerActivityType.BORROWER_UPDATED,
                    "BORROWER",
                    "Borrower with Id : "+updatedBorrower.getId()+ " kycStatus has been UPDATED  to: "+kycStatus+ " by user:"+createdByName+"-"+verifiedBy
            );
        }
        //End Audit Section

        
        log.info("Updated KYC status to {} for borrower: {} with id: {}", kycStatus, borrower.getFullName(), id);
        
        return convertToDto(updatedBorrower);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BorrowerDto> getBorrowersByGroup(Long groupId) {
        return borrowerRepository.findByGroupId(groupId, Pageable.unpaged())
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long getBorrowerCountByBranch(Long branchId) {
        return borrowerRepository.countActiveBorrowersByBranch(branchId);
    }

    public void auditLogs(
                           Long entityId,
                          GeneralConfig.BorrowerActivityType borrowerActivityType,
                           String  entityType,
                           String details
    ){
        Optional<User> currentUser = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName;
        Long createdById=null;

        if(currentUser.isPresent()){
            createdByName=currentUser.get().getFullName();
            createdById=currentUser.get().getId();
        }

        activityLogService.logBorrowerActivity(
                entityId,// updatedBorrower.getId()
                borrowerActivityType,//GeneralConfig.BorrowerActivityType.BORROWER_UPDATED,
                details ,//"Borrower Created by name: " + updatedBorrower.getFullName(),
                createdById
        );

        logStandardActivity(
                entityId,
                borrowerActivityType, //GeneralConfig.BorrowerActivityType.BORROWER_UPDATED,
                details ,//"Borrower Created by name: " + updatedBorrower.getFullName(),
                createdById,
                entityType,//"BORROWER",
                entityId //updatedBorrower.getId()
        );
        //audit log as well
        auditService.logEntityAction(
                entityId,//updatedBorrower.getId(),
                createdById,
                entityType,//"BORROWER",
                String.valueOf(borrowerActivityType),//"BORROWER UPDATED",
                details//"Borrower with ID: "+updatedBorrower.getFullName()+" Created"
        );
    }

    
    // Helper methods for entity-DTO conversion
    private BorrowerDto convertToDto(Borrower borrower) {
        BorrowerDto dto = new BorrowerDto();
        dto.setId(borrower.getId());
        dto.setBorrowerNumber(borrower.getBorrowerNumber());
        dto.setFirstName(borrower.getFirstName());
        dto.setLastName(borrower.getLastName());
        dto.setMiddleName(borrower.getMiddleName());
        dto.setGender(borrower.getGender());
        dto.setDateOfBirth(borrower.getDateOfBirth());
        dto.setPhoneNumber(borrower.getPhoneNumber());
        dto.setEmail(borrower.getEmail());
        dto.setAddress(borrower.getAddress());
        dto.setCity(borrower.getCity());
        dto.setState(borrower.getState());
        dto.setCountry(borrower.getCountry());
        dto.setPostalCode(borrower.getPostalCode());
        dto.setMaritalStatus(borrower.getMaritalStatus());
        dto.setOccupation(borrower.getOccupation());
        dto.setEmployer(borrower.getEmployer());
        dto.setMonthlyIncome(borrower.getMonthlyIncome());
        dto.setStatus(borrower.getStatus());
        dto.setKycStatus(borrower.getKycStatus());
        dto.setKycVerifiedAt(borrower.getKycVerifiedAt());
        dto.setEmergencyContactName(borrower.getEmergencyContactName());
        dto.setEmergencyContactPhone(borrower.getEmergencyContactPhone());
        dto.setEmergencyContactRelationship(borrower.getEmergencyContactRelationship());
        dto.setNationality(borrower.getNationality());
        dto.setIdentificationType(borrower.getIdentificationType());
        dto.setIdentificationNumber(borrower.getIdentificationNumber());
        dto.setNotes(borrower.getNotes());
        dto.setFullName(borrower.getFullName());
        
        if (borrower.getBranch() != null) {
            dto.setBranchId(borrower.getBranch().getId());
            dto.setBranchName(borrower.getBranch().getName());
        }
        
        if (borrower.getGroup() != null) {
            dto.setGroupId(borrower.getGroup().getId());
            dto.setGroupName(borrower.getGroup().getGroupName());
        }

        // === MAP PRODUCT TYPE ===
        if (borrower.getLoanProduct() != null) {
            dto.setLoanProductId(borrower.getLoanProduct().getId());
            // Map product type details
            LoanProduct product = new LoanProduct();
            product.setId(borrower.getLoanProduct().getId());
            product.setProductCode(borrower.getLoanProduct().getProductCode());
            product.setName(borrower.getLoanProduct().getName());
            product.setDescription(borrower.getLoanProduct().getDescription());
            product.setActive(borrower.getLoanProduct().getActive());
            dto.setLoanProduct(product);
        }
        
        return dto;
    }
    
    private Borrower convertToEntity(BorrowerDto dto) {
        Borrower borrower = new Borrower();
        borrower.setFirstName(dto.getFirstName());
        borrower.setLastName(dto.getLastName());
        borrower.setMiddleName(dto.getMiddleName());
        borrower.setGender(dto.getGender());
        borrower.setDateOfBirth(dto.getDateOfBirth());
        borrower.setPhoneNumber(dto.getPhoneNumber());
        borrower.setEmail(dto.getEmail());
        borrower.setAddress(dto.getAddress());
        borrower.setCity(dto.getCity());
        borrower.setState(dto.getState());
        borrower.setCountry(dto.getCountry());
        borrower.setPostalCode(dto.getPostalCode());
        borrower.setMaritalStatus(dto.getMaritalStatus());
        borrower.setOccupation(dto.getOccupation());
        borrower.setEmployer(dto.getEmployer());
        borrower.setMonthlyIncome(dto.getMonthlyIncome());
        borrower.setEmergencyContactName(dto.getEmergencyContactName());
        borrower.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        borrower.setEmergencyContactRelationship(dto.getEmergencyContactRelationship());
        borrower.setNationality(dto.getNationality());
        borrower.setIdentificationType(dto.getIdentificationType());
        borrower.setIdentificationNumber(dto.getIdentificationNumber());
        borrower.setNotes(dto.getNotes());
        
        return borrower;
    }

    public BorrowerSummaryDto convertToSummaryDto(Borrower borrower) {
        BorrowerSummaryDto dto = new BorrowerSummaryDto();
        dto.setId(borrower.getId());
        dto.setBorrowerNumber(borrower.getBorrowerNumber());
        dto.setFirstName(borrower.getFirstName());
        dto.setLastName(borrower.getLastName());
        dto.setMiddleName(borrower.getMiddleName());
        dto.setFullName(borrower.getFullName());
        dto.setPhoneNumber(borrower.getPhoneNumber());
        dto.setEmail(borrower.getEmail());
        dto.setStatus(borrower.getStatus());
        dto.setKycStatus(borrower.getKycStatus());
        dto.setKycVerifiedAt(borrower.getKycVerifiedAt());
        dto.setDateOfBirth(borrower.getDateOfBirth());
        dto.setOccupation(borrower.getOccupation());
        dto.setMonthlyIncome(borrower.getMonthlyIncome());
        dto.setIdentificationNumber(borrower.getIdentificationNumber());
        dto.setCreatedAt(borrower.getCreatedAt());

        if (borrower.getBranch() != null) {
            dto.setBranchName(borrower.getBranch().getName());
        }

        if (borrower.getGroup() != null) {
            dto.setGroupName(borrower.getGroup().getGroupName());
        }

        return dto;
    }

    public List<BorrowerSummaryDto> convertToSummaryDtoList(List<Borrower> borrowers) {
        return borrowers.stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BorrowerSummaryDto getBorrowerSummaryById(Long id) {
        Borrower borrower = borrowerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + id));
        return convertToSummaryDto(borrower);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BorrowerSummaryDto> getAllBorrowerSummaries(Pageable pageable) {
        return borrowerRepository.findAll(pageable).map(this::convertToSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowerSummaryDto> getBorrowerSummariesByGroup(Long groupId) {
        return borrowerRepository.findByGroupId(groupId, Pageable.unpaged())
                .stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }


    //************************DocumentUploads related methods**********************************************************/
    @Transactional
    @Override
    public BorrowerDocumentDto uploadDocument(Long borrowerId, DocumentConfig.DocumentType documentType,
                                              String documentName, MultipartFile file, String description) {
        // Validate borrower exists
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + borrowerId));
        // Validate file
        validateFile(file);
        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            // Generate unique filename
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String uniqueFileName = generateUniqueFileName(documentType, fileExtension);
            Path filePath = uploadPath.resolve(uniqueFileName);

            // Save file to filesystem
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Create and save document record
            BorrowerDocument document = createBorrowerDocument(borrower, documentType, documentName,
                    description, file, uniqueFileName, filePath.toString());

            BorrowerDocument savedDocument = borrowerDocumentRepository.save(document);

            log.info("Document uploaded successfully for borrower {}: {}",
                    borrower.getFullName(), savedDocument.getDocumentName());

            return convertToDocumentDto(savedDocument);

        } catch (IOException ex) {
            log.error("Failed to upload document for borrower {}: {}", borrowerId, ex.getMessage());
            throw new RuntimeException("Failed to upload document: " + ex.getMessage(), ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isAllowedFileType(contentType)) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: " + allowedFileTypes);
        }

        // Additional validation for specific file types
        if (contentType.startsWith("image/")) {
            validateImageFile(file);
        }
    }

    private boolean isAllowedFileType(String contentType) {
        String[] allowedTypes = allowedFileTypes.split(",");
        for (String allowedType : allowedTypes) {
            if (contentType.equals(allowedType.trim())) {
                return true;
            }
        }
        return false;
    }

    private void validateImageFile(MultipartFile file) {
        try {
            // Basic image validation - you could add more sophisticated checks
            if (file.getSize() == 0) {
                throw new IllegalArgumentException("Image file appears to be corrupted");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid image file: " + e.getMessage());
        }
    }

    private String generateUniqueFileName(DocumentConfig.DocumentType documentType, String fileExtension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomId = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s_%s%s",
                documentType.name().toLowerCase(),
                timestamp,
                randomId,
                fileExtension);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".dat";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private BorrowerDocument createBorrowerDocument(Borrower borrower, DocumentConfig.DocumentType documentType,
                                                    String documentName, String description, MultipartFile file,
                                                    String fileName, String filePath) {
        BorrowerDocument document = new BorrowerDocument();
        document.setBorrower(borrower);
        document.setDocumentType(DocumentConfig.DocumentType.valueOf(String.valueOf(documentType)));
        document.setDocumentName(documentName);
        document.setDescription(description);
        document.setFilePath(filePath);
        document.setFileName(fileName);
        document.setFileType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setStatus(DocumentConfig.DocumentStatus.PENDING);
        document.setCreatedAt(LocalDateTime.now());
        // Set expiry date for certain document types
        if (documentType == DocumentConfig.DocumentType.PASSPORT ||
                documentType == DocumentConfig.DocumentType.NATIONAL_ID) {
            document.setExpiryDate(LocalDateTime.now().plusYears(5).toLocalDate());
        }

        return document;
    }

    @Override
    @Transactional
    public void removeDocument(Long documentId) {
        BorrowerDocument document = borrowerDocumentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + documentId));

        try {
            // Delete file from filesystem
            Path filePath = Paths.get(document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted file: {}", document.getFilePath());
            }

            // Delete record from database
            borrowerDocumentRepository.delete(document);
            log.info("Removed document record: {}", document.getDocumentName());

        } catch (IOException ex) {
            log.error("Failed to delete file: {}", document.getFilePath(), ex);
            throw new RuntimeException("Failed to delete document file", ex);
        }
    }

    // Additional document-related methods
    @Override
    @Transactional(readOnly = true)
    public List<BorrowerDocumentDto> getBorrowerDocuments(Long borrowerId) {
        return borrowerDocumentRepository.findByBorrowerId(borrowerId)
                .stream()
                .map(this::convertToDocumentDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public BorrowerDocumentDto getDocumentById(Long documentId) {
        BorrowerDocument document = borrowerDocumentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + documentId));
        return convertToDocumentDto(document);
    }


    @Transactional
    @Override
    public BorrowerDocumentDto updateDocumentStatus(Long documentId, DocumentConfig.DocumentStatus status,
                                                    Long verifiedBy, String verificationNotes) {
        BorrowerDocument document = borrowerDocumentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + documentId));

        document.setStatus(status);
        document.setVerifiedBy(verifiedBy);
        document.setVerificationNotes(verificationNotes);

        if (status == DocumentConfig.DocumentStatus.VERIFIED) {
            document.setVerifiedAt(LocalDateTime.now());
        }

        BorrowerDocument updatedDocument = borrowerDocumentRepository.save(document);
        log.info("Updated document status to {} for document: {}", status, document.getDocumentName());

        return convertToDocumentDto(updatedDocument);
    }

    @Transactional(readOnly = true)
    @Override
    public List<BorrowerDocumentDto> getDocumentsByType(Long borrowerId, DocumentConfig.DocumentType documentType) {
        return borrowerDocumentRepository.findByBorrowerIdAndDocumentType(borrowerId, documentType)
                .stream()
                .map(this::convertToDocumentDto)
                .collect(Collectors.toList());
    }

    // Helper method for DTO conversion
    private BorrowerDocumentDto convertToDocumentDto(BorrowerDocument document) {
        BorrowerDocumentDto dto = new BorrowerDocumentDto();
        dto.setId(document.getId());

       // dto.setBorrowerId(document.getBorrower().getId());
        if (document.getBorrower() != null) {
            dto.setBorrowerId(document.getBorrower().getId());
            // SAFE: Use direct field access if possible, or use a separate query
            // Option A: If Borrower entity has getFullName() that doesn't trigger toString()
            dto.setBorrowerName(document.getBorrower().getFullName());
        }

        dto.setDocumentType(String.valueOf(document.getDocumentType()));
        dto.setDocumentName(document.getDocumentName());
        dto.setDescription(document.getDescription());
        dto.setFilePath(document.getFilePath());
        dto.setFileName(document.getFileName());
        dto.setFileType(document.getFileType());
        dto.setFileSize(document.getFileSize());
        dto.setStatus(document.getStatus());
        dto.setVerifiedAt(document.getVerifiedAt());
        dto.setVerifiedBy(document.getVerifiedBy());
        dto.setVerificationNotes(document.getVerificationNotes());
        dto.setExpiryDate(document.getExpiryDate());
        //dto.setBorrowerName(document.getBorrower().getFullName());
        dto.setCreatedAt(document.getCreatedAt());
        return dto;
    }

    @Override
    @Transactional
    public BulkKycVerificationResponse bulkUpdateKycStatus(BulkKycVerificationRequest request) {
        Long performedBy =securityUtils.getCurrentUserId();
        BulkKycVerificationResponse response = new BulkKycVerificationResponse();
        response.setPerformedBy(performedBy);
        response.setPerformedByName(securityUtils.getCurrentUsername()); // You'll need to implement this

        // Validate request
        if (!request.isValid()) {
            throw new IllegalArgumentException("Invalid bulk KYC verification request");
        }
        // Process each borrower
        for (Long borrowerId : request.getBorrowerIds()) {
            try {
                Borrower borrower = borrowerRepository.findById(borrowerId)
                        .orElseThrow(() -> new RuntimeException("Borrower not found with id: " + borrowerId));

                String previousStatus = borrower.getKycStatus().name();

                // Get specific notes for this borrower if provided
                String specificNotes = request.getVerificationNotes();
                if (request.getBorrowerDetails() != null) {
                    specificNotes = request.getBorrowerDetails().stream()
                            .filter(detail -> detail.getBorrowerId().equals(borrowerId))
                            .map(BulkKycVerificationRequest.BorrowerVerificationDetail::getSpecificNotes)
                            .findFirst()
                            .orElse(request.getVerificationNotes());
                }

                // Update KYC status
                borrower.setKycStatus(request.getKycStatus());
                borrower.setKycVerifiedBy(performedBy);
                borrower.setKycVerifiedAt(LocalDateTime.now());

                Borrower updatedBorrower = borrowerRepository.save(borrower);

                // Log activity
                borrowerActivityService.logQuickActivity(
                        borrowerId,
                        getKycActivityType(request.getKycStatus()),
                        String.format("KYC status updated to %s. Notes: %s",
                                request.getKycStatus().name(),
                                specificNotes != null ? specificNotes : "No notes provided"),
                        performedBy,
                        "KYC_BULK_UPDATE",
                        null
                );

                // Add to response
                BulkKycVerificationResponse.BorrowerUpdateResult result =
                        BulkKycVerificationResponse.BorrowerUpdateResult.success(
                                borrowerId,
                                borrower.getFullName(),
                                borrower.getBorrowerNumber(),
                                previousStatus,
                                request.getKycStatus().name()
                        );
                response.addSuccessResult(result);

                log.info("Bulk KYC update: Borrower {} status changed from {} to {}",
                        borrowerId, previousStatus, request.getKycStatus().name());

            } catch (Exception e) {
                log.error("Failed to update KYC status for borrower {}: {}", borrowerId, e.getMessage());

                BulkKycVerificationResponse.BorrowerUpdateResult result =
                        BulkKycVerificationResponse.BorrowerUpdateResult.failure(
                                borrowerId,
                                "Unknown", // We don't have borrower name due to exception
                                "Unknown",
                                e.getMessage()
                        );
                response.addFailureResult(result);
            }
        }

        response.generateSummary();

        // Send notifications if requested
        if (request.getSendNotification() && request.isVerificationAction()) {
            sendBulkKycNotifications(request.getBorrowerIds(), request.getNotificationTemplate());
        }

        log.info("Bulk KYC verification completed: {}", response.getSummary());
        return response;
    }

    @Override
    @Transactional
    public List<BorrowerDto> bulkUpdateKycStatus(List<Long> borrowerIds, GeneralConfig.KycStatus kycStatus,
                                                 Long verifiedBy, String notes) {
        List<BorrowerDto> updatedBorrowers = new ArrayList<>();

        for (Long borrowerId : borrowerIds) {
            try {
                BorrowerDto updatedBorrower = updateKycStatus(borrowerId, kycStatus, verifiedBy, notes);
                updatedBorrowers.add(updatedBorrower);
            } catch (Exception e) {
                log.warn("Failed to update KYC status for borrower {}: {}", borrowerId, e.getMessage());
                // Continue with other borrowers
            }
        }

        return updatedBorrowers;
    }

    @Override
    @Transactional
    public BulkKycVerificationResponse bulkKycRejection(List<Long> borrowerIds, String rejectionReason, Long rejectedBy) {
        BulkKycVerificationRequest request = new BulkKycVerificationRequest();
        request.setBorrowerIds(borrowerIds);
        request.setKycStatus(GeneralConfig.KycStatus.REJECTED);
        request.setVerificationNotes(rejectionReason);

        return bulkUpdateKycStatus(request);
    }

    @Override
    @Transactional
    public BulkKycVerificationResponse bulkKycVerification(List<Long> borrowerIds, Long verifiedBy) {
        BulkKycVerificationRequest request = new BulkKycVerificationRequest();
        request.setBorrowerIds(borrowerIds);
        request.setKycStatus(GeneralConfig.KycStatus.VERIFIED);
        request.setVerificationNotes("Bulk verification completed");
        request.setSendNotification(true);
        request.setNotificationTemplate("KYC_VERIFIED");

        return bulkUpdateKycStatus(request);
    }

    // Helper method to determine activity type based on KYC status
    private GeneralConfig.BorrowerActivityType getKycActivityType(GeneralConfig.KycStatus kycStatus) {
        switch (kycStatus) {
            case VERIFIED:
                return GeneralConfig.BorrowerActivityType.BORROWER_KYC_VERIFIED;
            case REJECTED:
                return GeneralConfig.BorrowerActivityType.BORROWER_KYC_REJECTED;
            case PENDING:
                return GeneralConfig.BorrowerActivityType.BORROWER_KYC_INITIATED;
            case EXPIRED:
                return GeneralConfig.BorrowerActivityType.BORROWER_KYC_EXPIRED;
            default:
                return GeneralConfig.BorrowerActivityType.BORROWER_UPDATED;
        }
    }

    // Helper method to send bulk notifications
    private void sendBulkKycNotifications(List<Long> borrowerIds, String template) {
        try {
            // Implementation depends on your notification service
            // This could send SMS, email, or push notifications
            log.info("Sending KYC notifications to {} borrowers with template: {}",
                    borrowerIds.size(), template);
            // Example implementation:
            // notificationService.sendBulkNotification(borrowerIds, template, "KYC_VERIFICATION");
        } catch (Exception e) {
            log.error("Failed to send bulk KYC notifications: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowerDto> getBorrowersEligibleForKycUpdate(GeneralConfig.KycStatus currentStatus, Boolean documentsUploaded) {
        // Typically, borrowers with pending KYC and all documents uploaded are eligible
        List<Borrower> borrowers;

        if (currentStatus != null) {
            borrowers = borrowerRepository.findByKycStatus(currentStatus);
        } else {
            // Default: get borrowers with pending KYC
            borrowers = borrowerRepository.findByKycStatus(GeneralConfig.KycStatus.PENDING);
        }

        // Filter by documents uploaded if requested
        if (documentsUploaded != null && documentsUploaded) {
            borrowers = borrowers.stream()
                    .filter(borrower -> hasAllRequiredDocuments(borrower.getId()))
                    .collect(Collectors.toList());
        }

        return borrowers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private boolean hasAllRequiredDocuments(Long borrowerId) {
        try {
            // Get required documents from loan products
            Set<DocumentConfig.DocumentType> requiredTypes = loanProductDocumentService.getRequiredDocumentTypes(borrowerId);

            if (requiredTypes.isEmpty()) {
                log.debug("No required documents found for borrower: {}", borrowerId);
                return true;
            }

            // Get all uploaded documents for the borrower
            List<BorrowerDocument> uploadedDocuments = borrowerDocumentRepository.findByBorrowerId(borrowerId);

            // Filter for valid documents (not rejected and not expired)
            Set<DocumentConfig.DocumentType> validUploadedTypes = uploadedDocuments.stream()
                    .filter(document -> document.getStatus() != DocumentConfig.DocumentStatus.REJECTED)
                    .filter(document -> {
                        if (document.getExpiryDate() != null) {
                            return !document.getExpiryDate().isBefore(LocalDate.now());
                        }
                        return true; // No expiry date means it's valid
                    })
                    .map(BorrowerDocument::getDocumentType)
                    .collect(Collectors.toSet());

            // Check if all required types are present in valid uploaded types
            boolean hasAllRequired = validUploadedTypes.containsAll(requiredTypes);

            if (!hasAllRequired) {
                // Log which documents are missing for debugging
                Set<DocumentConfig.DocumentType> missingTypes = new HashSet<>(requiredTypes);
                missingTypes.removeAll(validUploadedTypes);
                log.debug("Borrower {} missing documents: {}", borrowerId,
                        missingTypes.stream().map(Enum::name).collect(Collectors.joining(", ")));
            }

            log.debug("Borrower {} has all required documents: {}. Required: {}, Valid Uploaded: {}",
                    borrowerId, hasAllRequired, requiredTypes.size(), validUploadedTypes.size());

            return hasAllRequired;

        } catch (Exception e) {
            log.error("Error checking required documents for borrower {}: {}", borrowerId, e.getMessage(), e);
            return false;
        }
    }

/*Bororwer KYC Realated Methods

 */
    @Override
    @Transactional(readOnly = true)
    public BorrowerKycSummaryDto getBorrowerKycSummary(Long borrowerId) {
        log.info("Fetching KYC summary for borrower: {}", borrowerId);

        Borrower borrower = getBorrowerOrThrow(borrowerId);
        List<BorrowerDocument> documents = borrowerDocumentRepository.findByBorrowerId(borrowerId);
        List<DocumentVerification> verifications = documentVerificationRepository.findByBorrowerIdAndIsActiveTrue(borrowerId);

        List<KycWorkflowStepDto> workflowSteps = buildWorkflowSteps(borrowerId);

        return buildKycSummary(borrower, documents, verifications, workflowSteps);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowerKycSummaryDto> getBorrowerKycSummaries(List<Long> borrowerIds) {
        log.info("Fetching KYC summaries for {} borrowers", borrowerIds.size());
        return borrowerIds.stream()
                .map(this::getBorrowerKycSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean isBorrowerKycComplete(Long borrowerId) {
        BorrowerKycSummaryDto kycSummary = getBorrowerKycSummary(borrowerId);
        return kycSummary.getKycComplete();
    }

    /*public EnhancedKycSummaryDto getEnhancedKycSummary(Long borrowerId) {
        BorrowerKycSummaryDto basicSummary = getBorrowerKycSummary(borrowerId);
        List<KycWorkflowStepDto> workflowSteps = buildWorkflowSteps(borrowerId);

        return EnhancedKycSummaryDto.builder()
                .basicSummary(basicSummary)
                .workflowSteps(workflowSteps)
                .workflowProgress(calculateWorkflowProgress(workflowSteps))
                .currentStep(getCurrentStep(workflowSteps))
                .nextAction(getNextAction(workflowSteps))
                .build();
    }*/

    // Core Private Methods
    private Borrower getBorrowerOrThrow(Long borrowerId) {
        return borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found with id: " + borrowerId));
    }



    private BorrowerKycSummaryDto buildKycSummary(Borrower borrower,
                                                  List<BorrowerDocument> documents,
                                                  List<DocumentVerification> verifications,
                                                  List<KycWorkflowStepDto> workflowSteps
    ) {

        log.debug("START buildKycSummary for borrower: {}", borrower.getId());

        try {
            log.info("1. Calculating document stats...");
            DocumentStats documentStats = calculateDocumentStats(borrower.getId(), documents);

            log.info("2. Determining KYC workflow state...");
            KycWorkflowState currentState = determineKycWorkflowState(borrower, documentStats, workflowSteps);

            log.info("3. Getting assigned officer name...");
            String assignedOfficer = getAssignedOfficerName(borrower); // ← TEST THIS

            log.info("4. Getting verified by name...");
            String verifiedBy = getVerifiedByName(verifications); // ← TEST THIS

            log.info("5. Building final DTO...");
            return BorrowerKycSummaryDto.builder()
                    .borrowerId(borrower.getId())
                    .borrowerName(borrower.getFirstName() + " " + borrower.getLastName())
                    .borrowerOccupation(borrower.getOccupation())
                    .borrowerPhoneNumber(borrower.getPhoneNumber())
                    .borrowerEmail(borrower.getEmail())
                    .borrowerMonthlyIncome(borrower.getMonthlyIncome())
                    .documentsUploaded(documentStats.getTotalDocuments())
                    .documentsVerified(documentStats.getVerifiedDocuments())
                    .documentsPending(documentStats.getPendingDocuments())
                    .documentsRequired(documentStats.getRequiredDocuments())
                    .documentsRequiredType(documentStats.getRequiredDocumentsType())
                    .currentState(currentState)
                    .currentStep(getCurrentStepDisplay(workflowSteps))
                    .kycCompletionPercentage(calculateCompletionPercentage(documentStats, currentState, workflowSteps))
                    .pendingStepsCount(calculatePendingSteps(workflowSteps))
                    .startedAt(borrower.getCreatedAt())
                    .completedAt(getKycCompletionDate(borrower, currentState))
                    .estimatedCompletionDate(calculateEstimatedCompletionDate(currentState, workflowSteps))
                    .verifiedByName(getVerifiedByName(verifications))
                    .lastVerificationDate(getLastVerificationDate(verifications))
                    .overallStatus(determineOverallKycStatus(currentState))
                    .kycComplete(currentState == KycWorkflowState.VERIFIED)
                    .isKycComplete(currentState == KycWorkflowState.VERIFIED)
                    .documentStatuses(buildDocumentStatuses(documents))
                    .missingDocuments(identifyMissingDocuments(borrower.getId(), documents))
                    .assignedOfficerName(assignedOfficer)
                    .verifiedByName(verifiedBy)
                    .build();
        } catch (StackOverflowError e) {
            log.error("StackOverflow in buildKycSummary at step X", e);
            throw e;
        }

    }


    // Workflow Steps Management
    private List<KycWorkflowStepDto> buildWorkflowSteps(Long borrowerId) {
        log.info("BuildWorkflowSteps Start for borrower: {}", borrowerId);

        List<KycWorkflowStepDto> steps = Arrays.stream(KycWorkflowStep.values())
                .sorted(Comparator.comparing(KycWorkflowStep::getOrder))
                .map(step -> {
                    try {
                        return buildWorkflowStepDto(step, borrowerId, false);
                    } catch (Exception e) {
                        log.error("Failed to build step {} for borrower {}: {}", step, borrowerId, e.getMessage());
                        // Return a minimal DTO with error status instead of failing the whole stream
                        return KycWorkflowStepDto.builder()
                                .step(step)
                                .status("ERROR")
                                .isOverdue(false)
                                .retryCount(0)
                                .build();
                    }
                })
                .filter(step -> step != null) // Filter out any null results
                .collect(Collectors.toList());

        log.info("BuildWorkflowSteps Medium for borrower: {} (built {} steps)", borrowerId, steps.size());
        markCurrentStep(steps);
        log.info("BuildWorkflowSteps End for borrower: {}", borrowerId);
        return steps;
    }



    private KycWorkflowStepDto buildWorkflowStepDto(KycWorkflowStep step, Long borrowerId, boolean isCurrent) {
        boolean completed = workflowStepService.isStepCompleted(step, borrowerId);
        String status = determineStepStatus(completed, isCurrent);
        log.info("buildWorkflowStepDto Start for borrower: {}, Steps: {} ", borrowerId,step);

        KycWorkflowStepDto kycWorkflowStepDto= KycWorkflowStepDto.builder()
                .step(step)
                .status(status)
                .completedAt(completed ? getStepCompletionDate(step, borrowerId) : null)
                .isOverdue(isStepOverdue(step, status))
                .retryCount(0)
                .build();

        log.info("kycWorkflowStepDto results for borrower: {}, Results: {} ", borrowerId,kycWorkflowStepDto);

        return kycWorkflowStepDto;
    }



    // Helper method to mark the current step
    private void markCurrentStep(List<KycWorkflowStepDto> steps) {
        // Find the first incomplete step
        Optional<KycWorkflowStepDto> currentStep = steps.stream()
                .filter(step -> !"COMPLETED".equals(step.getStatus()))
                .findFirst();
        // Mark it as IN_PROGRESS
        currentStep.ifPresent(step -> step.setStatus("IN_PROGRESS"));
    }

    // Alternative approach for isCurrentStep if you still need it elsewhere
    private boolean isCurrentStep(KycWorkflowStep step, Long borrowerId) {
        // Get ordered steps
        List<KycWorkflowStep> orderedSteps = Arrays.stream(KycWorkflowStep.values())
                .sorted(Comparator.comparing(KycWorkflowStep::getOrder))
                .collect(Collectors.toList());

        // Find the first incomplete step
        for (KycWorkflowStep orderedStep : orderedSteps) {
            boolean completed = workflowStepService.isStepCompleted(orderedStep, borrowerId);
            if (!completed) {
                // Return true if this is the step we're checking
                return orderedStep == step;
            }
        }
        return false; // All steps are completed
    }

    private String determineStepStatus(boolean completed, boolean isCurrent) {
        if (completed) return "COMPLETED";
        if (isCurrent) return "IN_PROGRESS";
        return "PENDING";
    }

    private boolean isStepOverdue(KycWorkflowStep step, String status) {
        log.info("isStepOverdue Start for status: {}, Steps: {} ", status,step);
        // Implement based on your due date logic
        return false;
    }

    // Document Statistics
    private DocumentStats calculateDocumentStats(Long borrowerId, List<BorrowerDocument> documents) {
        log.info("calculateDocumentStats START for borrowerId: {}, {} ", borrowerId);
        Set<DocumentConfig.DocumentType> requiredTypes = loanProductDocumentService.getRequiredDocumentTypes(borrowerId);

        log.info("calculateDocumentStats for borrowerId: {}, requiredTypes: {} ", borrowerId,requiredTypes);

        int totalDocuments = documents.size();
        int verifiedDocuments = 0;
        int pendingDocuments = 0;
        int expiredDocuments = 0;
        int requiredDocuments = requiredTypes.size();
        List<String> requiredDocumentsType = convertDocumentTypesToStrings(requiredTypes);

        log.info("calculateDocumentStats for requiredDocumentsType: {}, requiredDocumentsType: {} ", borrowerId,requiredDocumentsType);

        for (BorrowerDocument document : documents) {
            if (DocumentConfig.DocumentStatus.VERIFIED.equals(document.getStatus())) {
                if (document.getExpiryDate() != null && document.getExpiryDate().isBefore(LocalDate.now())) {
                    expiredDocuments++;
                } else {
                    verifiedDocuments++;
                }
            } else if (DocumentConfig.DocumentStatus.PENDING.equals(document.getStatus())) {
                pendingDocuments++;
            }
        }

        return new DocumentStats(totalDocuments, verifiedDocuments, pendingDocuments,
                expiredDocuments, requiredDocuments,requiredDocumentsType);
    }


    // Method 2: Set<DocumentType> -> List<String>
    private List<String> convertDocumentTypesToStrings(Set<DocumentConfig.DocumentType> documentTypes) {
        if (documentTypes == null || documentTypes.isEmpty()) {
            return new ArrayList<>();
        }

        return documentTypes.stream()
                .map(DocumentConfig.DocumentType::name)
                .collect(Collectors.toList());
    }


    // Workflow State Determination (Simplified)
    private KycWorkflowState determineKycWorkflowState(Borrower borrower,
                                                       DocumentStats documentStats,
                                                       List<KycWorkflowStepDto> workflowSteps) {
        // Check terminal states first
        if (GeneralConfig.KycStatus.VERIFIED.equals(borrower.getKycStatus())) {
            return KycWorkflowState.VERIFIED;
        }
        if (GeneralConfig.KycStatus.REJECTED.equals(borrower.getKycStatus())) {
            return KycWorkflowState.REJECTED;
        }
        if (GeneralConfig.KycStatus.EXPIRED.equals(borrower.getKycStatus())) {
            return KycWorkflowState.EXPIRED;
        }
        if (GeneralConfig.KycStatus.SUSPENDED.equals(borrower.getKycStatus())) {
            return KycWorkflowState.SUSPENDED;
        }

        // Progressive state determination
        if (documentStats.getTotalDocuments() == 0) {
            return borrower.getKycStatus() == null || GeneralConfig.KycStatus.NOT_STARTED.equals(borrower.getKycStatus())
                    ? KycWorkflowState.NOT_STARTED
                    : KycWorkflowState.INITIATED;
        }

        if (documentStats.getTotalDocuments() < documentStats.getRequiredDocuments()) {
            return KycWorkflowState.DOCUMENT_COLLECTION;
        }

        if (documentStats.getVerifiedDocuments() < documentStats.getRequiredDocuments()) {
            return KycWorkflowState.DOCUMENT_UPLOAD_PENDING;
        }

        if (hasInProgressVerificationSteps(workflowSteps)) {
            return KycWorkflowState.VERIFICATION_IN_PROGRESS;
        }

        if (hasPendingApprovalSteps(workflowSteps)) {
            return KycWorkflowState.PENDING_APPROVAL;
        }

        return KycWorkflowState.UNDER_REVIEW;
    }

    private boolean hasInProgressVerificationSteps(List<KycWorkflowStepDto> workflowSteps) {
        return workflowSteps.stream()
                .anyMatch(step -> step.getStep().startsWith("VERIFY_") && "IN_PROGRESS".equals(step.getStatus()));
    }

    private boolean hasPendingApprovalSteps(List<KycWorkflowStepDto> workflowSteps) {
        return workflowSteps.stream()
                .anyMatch(step -> step.getStep().contains("APPROVAL") && "PENDING".equals(step.getStatus()));
    }

    // Missing Documents Identification
    private List<String> identifyMissingDocuments(Long borrowerId, List<BorrowerDocument> documents) {
        Set<DocumentConfig.DocumentType> requiredTypes = loanProductDocumentService.getRequiredDocumentTypes(borrowerId);
        Set<DocumentConfig.DocumentType> uploadedTypes = documents.stream()
                .map(BorrowerDocument::getDocumentType)
                .collect(Collectors.toSet());

        return requiredTypes.stream()
                .filter(type -> !uploadedTypes.contains(type))
                .map(type -> type.getDisplayName())
                .sorted()
                .collect(Collectors.toList());
    }

    // Simplified Helper Methods
    private String getCurrentStepDisplay(List<KycWorkflowStepDto> workflowSteps) {
        return workflowSteps.stream()
                .filter(step -> "IN_PROGRESS".equals(step.getStatus()))
                .findFirst()
                .map(KycWorkflowStepDto::getName)
                .orElse("Not Started");
    }

    private Integer calculateCompletionPercentage(DocumentStats stats, KycWorkflowState state, List<KycWorkflowStepDto> workflowSteps) {
        if (state == KycWorkflowState.VERIFIED) return 100;

        long completedSteps = workflowSteps.stream()
                .filter(step -> "COMPLETED".equals(step.getStatus()))
                .count();
        long totalSteps = workflowSteps.size();

        return totalSteps > 0 ? (int) ((completedSteps * 100) / totalSteps) : 0;
    }

    private Integer calculatePendingSteps(List<KycWorkflowStepDto> workflowSteps) {
        return (int) workflowSteps.stream()
                .filter(step -> !"COMPLETED".equals(step.getStatus()))
                .count();
    }

    private String getAssignedOfficerNameORG(Borrower borrower) {
        return borrower.getAssignedOfficerId() != null
                ? userService.getUserNameById(borrower.getAssignedOfficerId())
                : null;
    }

    private String getAssignedOfficerName(Borrower borrower) {
        if (borrower.getAssignedOfficerId() == null) {
            return null;
        }

        // BAD: This fetches User entity which might have @Data
        // User user = userRepository.findById(borrower.getAssignedOfficerId());
        // return user.getFullName(); // Causes StackOverflow if User has @Data

        // GOOD: Use projection
        return userService.findFullNameById(borrower.getAssignedOfficerId());

        // OR: Return just the ID for now
        // return "Officer #" + borrower.getAssignedOfficerId();
    }


    private String getVerifiedByNameORG(List<DocumentVerification> verifications) {
        return verifications.stream()
                .filter(v -> v.getVerificationDate() != null)
                .max(Comparator.comparing(DocumentVerification::getVerificationDate))
                .map(verification -> userService.getUserNameById(verification.getVerifiedById()))
                .orElse(null);
    }

    private String getVerifiedByName(List<DocumentVerification> verifications) {
        if (verifications == null || verifications.isEmpty()) {
            return null;
        }

        // Check if this accesses User entities
        // DocumentVerification might have User relationships
        return verifications.stream()
                .filter(v -> v.getVerifiedBy() != null)
                .findFirst()
                .map(v -> {
                    // BAD: If this calls v.getVerifiedByUser().getFullName()
                    // and User has @Data, it causes StackOverflow
                    return "Verified"; // Temporary
                })
                .orElse(null);
    }

    private LocalDateTime getLastVerificationDate(List<DocumentVerification> verifications) {
        return verifications.stream()
                .filter(v -> v.getVerificationDate() != null)
                .max(Comparator.comparing(DocumentVerification::getVerificationDate))
                .map(DocumentVerification::getVerificationDate)
                .orElse(null);
    }

    // Placeholder implementations for methods that need specific business logic
    private LocalDateTime getStepCompletionDate(KycWorkflowStep step, Long borrowerId) {
        // Implement based on your completion tracking system
        log.info("getStepCompletionDate Start for borrower: {}, Steps: {} ", borrowerId,step);
        return LocalDateTime.now().minusDays(1);
    }

    private LocalDateTime getKycCompletionDate(Borrower borrower, KycWorkflowState currentState) {
        if (!currentState.isTerminalState()) return null;
        return borrower.getKycVerifiedAt() != null ? borrower.getKycVerifiedAt() : borrower.getUpdatedAt();
    }

    private LocalDateTime calculateEstimatedCompletionDate(KycWorkflowState currentState, List<KycWorkflowStepDto> workflowSteps) {
        if (currentState.isTerminalState()) return null;
        return LocalDateTime.now().plusDays(7); // Simplified estimation
    }

    private GeneralConfig.KycStatus determineOverallKycStatus(KycWorkflowState currentState) {
        switch (currentState) {
            case VERIFIED: return GeneralConfig.KycStatus.VERIFIED;
            case REJECTED: return GeneralConfig.KycStatus.REJECTED;
            case EXPIRED: return GeneralConfig.KycStatus.EXPIRED;
            case SUSPENDED: return GeneralConfig.KycStatus.SUSPENDED;
            case NOT_STARTED: return GeneralConfig.KycStatus.NOT_STARTED;
            default: return GeneralConfig.KycStatus.PENDING;
        }
    }

    private List<DocumentStatusDto> buildDocumentStatuses(List<BorrowerDocument> documents) {
        return documents.stream()
                .map(this::convertToDocumentStatusDto)
                .sorted(Comparator.comparing(DocumentStatusDto::getStatus)
                        .thenComparing(DocumentStatusDto::getCreatedAt, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private DocumentStatusDto convertToDocumentStatusDto(BorrowerDocument document) {
        DocumentStatusDto dto = new DocumentStatusDto();

        // At minimum, set these basic fields
        if (document.getDocumentType() != null) {
            dto.setDocumentType(document.getDocumentType().name());
        }

        if (document.getStatus() != null) {
            dto.setStatus(document.getStatus().name());
        }

        dto.setVerifiedAt(document.getVerifiedAt());
        dto.setCreatedAt(document.getCreatedAt());

        // Default to true for now
        dto.setIsRequired(true);

        return dto;
    }

    // Inner class for document statistics
    @Getter
    @AllArgsConstructor
    private static class DocumentStats {
        private final int totalDocuments;
        private final int verifiedDocuments;
        private final int pendingDocuments;
        private final int expiredDocuments;
        private final int requiredDocuments;
        private List<String> requiredDocumentsType;

        public boolean hasExpiredDocuments() {
            return expiredDocuments > 0;
        }
    }


    @Override
    public List<BorrowerActiveLoanDto> getBorrowerActiveLoans(Long borrowerId) {
        log.info("Fetching active loans for borrower: {}", borrowerId);

        // Verify borrower exists
        if (!borrowerRepository.existsById(borrowerId)) {
            throw new ResourceNotFoundException("Borrower not found with id: " + borrowerId);
        }

        // Fetch active loans from loan repository
        List<Loan> activeLoans = loanRepository.findByBorrowerIdAndStatusIn(
                borrowerId,
                Arrays.asList(GeneralConfig.LoanStatus.ACTIVE, GeneralConfig.LoanStatus.DELINQUENT)
        );

        return activeLoans.stream()
                .map(this::mapToActiveLoanDto)
                .collect(Collectors.toList());
    }


    @Override
    public BorrowerStatisticsDto getBorrowerStatistics(Long borrowerId) {
        log.info(">>> Fetching statistics for borrower: {}", borrowerId);

        // Get all loans for borrower
        List<Loan> allLoans = loanRepository.findByBorrowerId(borrowerId);

        // Calculate statistics
        long activeLoans = allLoans.stream()
                .filter(l -> l.getStatus() == GeneralConfig.LoanStatus.ACTIVE || l.getStatus() == GeneralConfig.LoanStatus.DELINQUENT)
                .count();

        long delinquentLoans = allLoans.stream()
                .filter(l -> l.getStatus() == GeneralConfig.LoanStatus.DELINQUENT)
                .count();

        BigDecimal totalPrincipal = allLoans.stream()
                .map(Loan::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstandingBalance = allLoans.stream()
                .filter(l -> l.getStatus() == GeneralConfig.LoanStatus.ACTIVE || l.getStatus() == GeneralConfig.LoanStatus.DELINQUENT)
                .map(Loan::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Find first and last loan dates
        Optional<LocalDate> firstLoanDate = allLoans.stream()
                .map(Loan::getDisbursementDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo);

        Optional<LocalDate> lastLoanDate = allLoans.stream()
                .map(Loan::getDisbursementDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo);

        return BorrowerStatisticsDto.builder()
                .totalLoans((long) allLoans.size())
                .activeLoanCount(activeLoans)
                .completedLoans(allLoans.stream().filter(l -> l.getStatus() == GeneralConfig.LoanStatus.CLOSED).count())
                .delinquentLoans(delinquentLoans)
                .totalPrincipalBorrowed(totalPrincipal)
                .totalOutstandingBalance(outstandingBalance)
                .firstLoanDate(firstLoanDate.orElse(null))
                .lastLoanDate(lastLoanDate.orElse(null))
                .hasActiveLoans(activeLoans > 0)
                .isDelinquent(delinquentLoans > 0)
                .build();
    }

    @Override
    public Page<BorrowerActiveLoanDto> getBorrowerActiveLoansPaginated(Long borrowerId, Pageable pageable) {
        return null;
    }

    @Override
    public Page<BorrowerLoanHistoryDto> getBorrowerLoanHistoryPaginated(Long borrowerId, Pageable pageable) {
        return null;
    }

    @Override
    public BorrowerStatisticsDto getBorrowerStatisticsByDateRange(Long borrowerId, LocalDate startDate, LocalDate endDate) {
        return null;
    }

    @Override
    public BorrowerLoanSummaryDto getBorrowerLoanSummary(Long borrowerId) {
        return null;
    }


    private BorrowerActiveLoanDto mapToActiveLoanDto(Loan loan) {
        if (loan == null) return null;

        // Calculate remaining installments
        Integer remainingInstallments = null;
        if (loan.getMaturityDate() != null && loan.getDisbursementDate() != null) {
            long monthsBetween = ChronoUnit.MONTHS.between(LocalDate.now(), loan.getMaturityDate());
            remainingInstallments = Math.max(0, (int) monthsBetween);
        }

        return BorrowerActiveLoanDto.builder()
                .id(loan.getId())
                .loanAccountNumber(loan.getLoanAccountNumber())
                .principalAmount(loan.getPrincipalAmount())
                .outstandingBalance(loan.getOutstandingBalance() != null ?
                        loan.getOutstandingBalance() : loan.getPrincipalAmount())
                .monthlyPayment(calculateMonthlyPayment(loan))
                .disbursementDate(loan.getDisbursementDate())
                .maturityDate(loan.getMaturityDate())
                .status(loan.getStatus() != null ? loan.getStatus().toString() : null)
                .daysDelinquent(loan.getDaysDelinquent() != null ? loan.getDaysDelinquent() : 0)
                .interestRate(loan.getInterestRate())
                .tenureMonths(loan.getTenureMonths())
                .remainingInstallments(remainingInstallments)
                .loanProductName(getLoanProductName(loan))
                .branchId(loan.getBranch() != null ? loan.getBranch().getId() : null)
                .branchName(loan.getBranch() != null ? loan.getBranch().getName() : null)
                .build();
    }

    private BorrowerLoanHistoryDto mapToLoanHistoryDto(Loan loan) {
        if (loan == null) return null;

        // Get last payment date from repayment schedules
        LocalDate lastPaymentDate = null;
        Integer numberOfPayments = 0;
        Integer latePayments = 0;

        if (loan.getRepaymentSchedules() != null && !loan.getRepaymentSchedules().isEmpty()) {
            lastPaymentDate = loan.getRepaymentSchedules().stream()
                    .filter(s -> s.getPaidDate() != null)
                    .map(RepaymentSchedule::getPaidDate)
                    .max(LocalDate::compareTo)
                    .orElse(null);

            numberOfPayments = (int) loan.getRepaymentSchedules().stream()
                    .filter(s -> s.getPaidDate() != null)
                    .count();

            latePayments = (int) loan.getRepaymentSchedules().stream()
                    .filter(s -> s.getPaidDate() != null &&
                            s.getDueDate() != null &&
                            s.getPaidDate().isAfter(s.getDueDate()))
                    .count();
        }

        return BorrowerLoanHistoryDto.builder()
                .id(loan.getId())
                .loanAccountNumber(loan.getLoanAccountNumber())
                .principalAmount(loan.getPrincipalAmount())
                .totalPaid(calculateTotalPaid(loan))
                .outstandingBalance(loan.getOutstandingBalance() != null ?
                        loan.getOutstandingBalance() : loan.getPrincipalAmount())
                .disbursementDate(loan.getDisbursementDate())
                .closedDate(loan.getClosedDate())
                .status(loan.getStatus() != null ? loan.getStatus().toString() : null)
                .loanProductName(getLoanProductName(loan))
                .tenureMonths(loan.getTenureMonths())
                .interestRate(loan.getInterestRate())
                .lastPaymentDate(lastPaymentDate)
                .numberOfPayments(numberOfPayments)
                .latePayments(latePayments)
                .disbursedBy(loan.getDisbursedBy() != null ?
                        loan.getDisbursedBy().getFirstName() + " " + loan.getDisbursedBy().getLastName() : null)
                .closedBy(loan.getClosedBy() != null ?
                        loan.getClosedBy().getFirstName() + " " + loan.getClosedBy().getLastName() : null)
                .build();
    }

    // Helper methods
    private BigDecimal calculateMonthlyPayment(Loan loan) {
        if (loan == null || loan.getPrincipalAmount() == null || loan.getTenureMonths() == null) {
            return BigDecimal.ZERO;
        }

        // Simple calculation - you may want to use your actual repayment calculation logic
        BigDecimal monthlyPrincipal = loan.getPrincipalAmount()
                .divide(BigDecimal.valueOf(loan.getTenureMonths()), 2, RoundingMode.HALF_UP);

        BigDecimal monthlyInterest = loan.getPrincipalAmount()
                .multiply(loan.getInterestRate() != null ? loan.getInterestRate() : BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        return monthlyPrincipal.add(monthlyInterest);
    }

    private BigDecimal calculateTotalPaid(Loan loan) {
        if (loan == null || loan.getRepaymentSchedules() == null) {
            return BigDecimal.ZERO;
        }

        return loan.getRepaymentSchedules().stream()
                .map(s -> s.getTotalPaid() != null ? s.getTotalPaid() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String getLoanProductName(Loan loan) {
        if (loan == null) return null;
        if (loan.getLoanProduct() != null) {
            return loan.getLoanProduct().getName();
        }
        if (loan.getLoanApplication() != null &&
                loan.getLoanApplication().getLoanProduct() != null) {
            return loan.getLoanApplication().getLoanProduct().getName();
        }
        return "Unknown Product";
    }

    /**
     * Alternative simplified version if you don't need all the detailed metrics
     */
    @Override
    public List<BorrowerLoanHistoryDto> getBorrowerLoanHistory(Long borrowerId) {
        log.info("Fetching loan history for borrower ID: {}", borrowerId);

        // Verify borrower exists
        if (!borrowerRepository.existsById(borrowerId)) {
            throw new ResourceNotFoundException("Borrower not found with id: " + borrowerId);
        }

        // Fetch all loans for borrower
        List<Loan> allLoans = loanRepository.findAllByBorrowerIdOrderByCreatedAtDesc(borrowerId);

        // Map to simplified DTOs
        return allLoans.stream()
                .map(loan -> {
                    // Calculate payment summary
                    long totalPayments = loan.getRepaymentSchedules() != null ?
                            loan.getRepaymentSchedules().size() : 0;
                    long paymentsMade = loan.getRepaymentSchedules() != null ?
                            loan.getRepaymentSchedules().stream()
                                    .filter(s -> s.getPaidDate() != null)
                                    .count() : 0;

                    return BorrowerLoanHistoryDto.builder()
                            .id(loan.getId())
                            .loanAccountNumber(loan.getLoanAccountNumber())
                            .principalAmount(loan.getPrincipalAmount())
                            .totalPaid(calculateTotalPaid(loan))
                            .outstandingBalance(loan.getOutstandingBalance())
                            .disbursementDate(loan.getDisbursementDate())
                            .closedDate(loan.getClosedDate())
                            .status(loan.getStatus() != null ? loan.getStatus().toString() : null)
                            .loanProductName(getLoanProductName(loan))
                            .tenureMonths(loan.getTenureMonths())
                            .interestRate(loan.getInterestRate())
                            .lastPaymentDate(getLastPaymentDate(loan))
                            .numberOfPayments((int) paymentsMade)
                            .latePayments(calculateLatePayments(loan))
                            .disbursedBy(getUserFullName(loan.getDisbursedBy()))
                            .closedBy(getUserFullName(loan.getClosedBy()))
                            .build();
                })
                .collect(Collectors.toList());
    }


    /**
     * Get user full name safely
     */
    private String getUserFullName(User user) {
        if (user == null) return null;
        try {
            return user.getFirstName() + " " + user.getLastName();
        } catch (Exception e) {
            log.warn("Could not get user full name: {}", e.getMessage());
            return "Unknown User";
        }
    }


    /**
     * Calculate late payments
     */
    private int calculateLatePayments(Loan loan) {
        if (loan.getRepaymentSchedules() == null) return 0;

        return (int) loan.getRepaymentSchedules().stream()
                .filter(s -> s.getPaidDate() != null &&
                        s.getPaidDate().isAfter(s.getDueDate()))
                .count();
    }

    /**
     * Get last payment date
     */
    private LocalDate getLastPaymentDate(Loan loan) {
        if (loan.getRepaymentSchedules() == null) return null;

        return loan.getRepaymentSchedules().stream()
                .filter(s -> s.getPaidDate() != null)
                .map(RepaymentSchedule::getPaidDate)
                .max(LocalDate::compareTo)
                .orElse(null);
    }







}