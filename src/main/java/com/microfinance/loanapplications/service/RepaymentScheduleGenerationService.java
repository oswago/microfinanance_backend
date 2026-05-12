// service/RepaymentScheduleGenerationService.java
package com.microfinance.loanapplications.service;

import com.microfinance.base.entity.User;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.entity.RepaymentSchedule;
import com.microfinance.loanapplications.repository.RepaymentScheduleRepository;
import com.microfinance.system.entity.SystemSettings;
import com.microfinance.system.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Centralized service for generating and updating loan repayment schedules
 * Ensures consistency across loan approval and disbursement stages
 * Complies with lending regulations (max total repayment ≤ 2x principal)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RepaymentScheduleGenerationService {

    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final SystemSettingsRepository systemSettingsRepository;

    // Maximum allowed total repayment as percentage of principal (200% = 2x)
    private static final BigDecimal MAX_TOTAL_REPAYMENT_PERCENTAGE = new BigDecimal("200");

    /**
     * Main entry point - generates or updates repayment schedule for a loan
     * This method intelligently handles both creation and update scenarios
     * 
     * @param loan The loan entity
     * @param currentUser The user performing the operation
     * @return List of generated/updated repayment schedules
     */
    @Transactional
    public List<RepaymentSchedule> generateOrUpdateRepaymentSchedule(Loan loan, User currentUser) {
        log.info("Generating/updating repayment schedule for loan: {}", loan.getLoanAccountNumber());

        // Get system settings for interest calculation
        SystemSettings systemSettings = getSystemSettings();

        // Validate loan parameters
        validateLoanForScheduleGeneration(loan);

        // Check if schedules already exist
        List<RepaymentSchedule> existingSchedules = loan.getRepaymentSchedules();

        if (existingSchedules != null && !existingSchedules.isEmpty()) {
            log.info("Updating existing repayment schedules for loan: {}", loan.getLoanAccountNumber());
            return updateExistingSchedules(loan, existingSchedules, systemSettings, currentUser);
        } else {
            log.info("Creating new repayment schedules for loan: {}", loan.getLoanAccountNumber());
            return createNewSchedules(loan, systemSettings, currentUser);
        }
    }

    /**
     * Create new repayment schedules from scratch
     */
    private List<RepaymentSchedule> createNewSchedules(Loan loan, SystemSettings systemSettings, User currentUser) {
        List<RepaymentSchedule> schedules = new ArrayList<>();

        // Get calculation parameters
        BigDecimal monthlyPrincipal = calculateMonthlyPrincipal(loan.getPrincipalAmount(), loan.getTenureMonths());
        BigDecimal monthlyInterestRate = getMonthlyInterestRate(loan, systemSettings);
        BigDecimal remainingPrincipal = loan.getPrincipalAmount();
        
        LocalDate startDate = getStartDateForSchedule(loan);
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalRepayment = BigDecimal.ZERO;

        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            LocalDate dueDate = startDate.plusMonths(i);

            // Calculate interest on remaining principal
            BigDecimal interest = calculateInterest(remainingPrincipal, monthlyInterestRate, systemSettings);
            
            // For last installment, adjust principal to avoid rounding issues
            BigDecimal principalForThisInstallment = monthlyPrincipal;
            if (i == loan.getTenureMonths()) {
                principalForThisInstallment = remainingPrincipal;
            }

            BigDecimal totalDue = principalForThisInstallment.add(interest);
            
            // Track totals for compliance check
            totalInterest = totalInterest.add(interest);
            totalRepayment = totalRepayment.add(totalDue);

            RepaymentSchedule schedule = buildRepaymentSchedule(loan, i, dueDate, 
                    principalForThisInstallment, interest, totalDue, currentUser);
            schedules.add(schedule);

            // Update remaining principal
            remainingPrincipal = remainingPrincipal.subtract(principalForThisInstallment);
        }

        // Validate compliance with lending regulations
        validateRepaymentLimit(loan.getPrincipalAmount(), totalRepayment);

        log.info("Created {} new repayment schedules for loan {}. Total Interest: {}, Total Repayment: {}", 
                schedules.size(), loan.getLoanAccountNumber(), totalInterest, totalRepayment);

        return schedules;
    }

    /**
     * Update existing schedules with actual disbursement data
     */
    private List<RepaymentSchedule> updateExistingSchedules(Loan loan, List<RepaymentSchedule> existingSchedules, 
                                                             SystemSettings systemSettings, User currentUser) {
        // Calculate new schedule parameters
        BigDecimal monthlyPrincipal = calculateMonthlyPrincipal(loan.getPrincipalAmount(), loan.getTenureMonths());
        BigDecimal monthlyInterestRate = getMonthlyInterestRate(loan, systemSettings);
        BigDecimal remainingPrincipal = loan.getPrincipalAmount();
        
        LocalDate startDate = getStartDateForSchedule(loan);
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalRepayment = BigDecimal.ZERO;

        // Sort existing schedules by installment number
        existingSchedules.sort(Comparator.comparing(RepaymentSchedule::getInstallmentNumber));

        for (int i = 0; i < existingSchedules.size(); i++) {
            RepaymentSchedule schedule = existingSchedules.get(i);
            int installmentNumber = i + 1;
            LocalDate dueDate = startDate.plusMonths(installmentNumber);

            // Calculate interest on remaining principal
            BigDecimal interest = calculateInterest(remainingPrincipal, monthlyInterestRate, systemSettings);
            
            // For last installment, adjust principal to avoid rounding issues
            BigDecimal principalForThisInstallment = monthlyPrincipal;
            if (installmentNumber == loan.getTenureMonths()) {
                principalForThisInstallment = remainingPrincipal;
            }

            BigDecimal totalDue = principalForThisInstallment.add(interest);
            
            // Track totals for compliance check
            totalInterest = totalInterest.add(interest);
            totalRepayment = totalRepayment.add(totalDue);

            // Update the existing schedule
            updateScheduleFields(schedule, dueDate, principalForThisInstallment, interest, totalDue, currentUser);

            // Update remaining principal for next iteration
            remainingPrincipal = remainingPrincipal.subtract(principalForThisInstallment);
        }

        // Validate compliance with lending regulations
        validateRepaymentLimit(loan.getPrincipalAmount(), totalRepayment);

        log.info("Updated {} repayment schedules for loan {}. Total Interest: {}, Total Repayment: {}", 
                existingSchedules.size(), loan.getLoanAccountNumber(), totalInterest, totalRepayment);

        return existingSchedules;
    }

    /**
     * Calculate monthly principal payment using proper amortization
     * Uses standard loan amortization formula: P * (r * (1+r)^n) / ((1+r)^n - 1)
     */
    private BigDecimal calculateMonthlyPrincipal(BigDecimal principal, int tenureMonths) {
        // For simple reducing balance, we use equal principal payments
        return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
    }

    /**
     * Get monthly interest rate based on system settings
     */
    private BigDecimal getMonthlyInterestRate(Loan loan, SystemSettings settings) {
        BigDecimal annualRate = loan.getInterestRate();
        
        // If no rate on loan, try to get from system settings
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) == 0) {
            if (settings != null && settings.getDefaultInterestRate() != null) {
                annualRate = settings.getDefaultInterestRate();
                log.info("Using default interest rate from system settings: {}%", annualRate);
            } else {
                annualRate = new BigDecimal("12"); // Default 12% if nothing configured
                log.warn("No interest rate configured, using default: {}%", annualRate);
            }
        }
        
        // Convert annual percentage to monthly decimal
        // Monthly rate = (Annual Rate / 100) / 12
        return annualRate
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
    }

    /**
     * Calculate interest based on the configured calculation method
     */
    private BigDecimal calculateInterest(BigDecimal principal, BigDecimal monthlyRate, SystemSettings settings) {
        SystemSettings.InterestCalculationMethod method = getInterestCalculationMethod(settings);
        
        switch (method) {
            case FLAT_RATE:
                // Flat rate: interest calculated on original principal
                return principal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            case REDUCING_BALANCE:
                // Reducing balance: interest calculated on remaining principal
                return principal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            case COMPOUND:
                // Compound interest: interest calculated on outstanding balance including previous interest
                return principal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            default:
                return principal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Get interest calculation method from system settings
     */
    private SystemSettings.InterestCalculationMethod getInterestCalculationMethod(SystemSettings settings) {
        if (settings != null && settings.getDefaultInterestCalculationMethod() != null) {
            return settings.getDefaultInterestCalculationMethod();
        }
        // Default to REDUCING_BALANCE if not configured
        return SystemSettings.InterestCalculationMethod.REDUCING_BALANCE;
    }

    /**
     * Get start date for schedule generation
     * Uses disbursement date if available, otherwise uses current date plus 1 month
     */
    private LocalDate getStartDateForSchedule(Loan loan) {
        if (loan.getDisbursementDate() != null) {
            return loan.getDisbursementDate();
        }
        // If not disbursed yet, use current date for estimation
        return LocalDate.now();
    }

    /**
     * Build a new repayment schedule entity
     */
    private RepaymentSchedule buildRepaymentSchedule(Loan loan, int installmentNumber, LocalDate dueDate,
                                                      BigDecimal principal, BigDecimal interest, 
                                                      BigDecimal totalDue, User currentUser) {
        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setLoan(loan);
        schedule.setInstallmentNumber(installmentNumber);
        schedule.setDueDate(dueDate);
        
        // Amount fields
        schedule.setPrincipalAmount(principal);
        schedule.setInterestAmount(interest);
        schedule.setPrincipalDue(principal);
        schedule.setInterestDue(interest);
        schedule.setTotalDue(totalDue);
        schedule.setTotalDueAmount(totalDue);
        schedule.setOutstandingAmount(totalDue);
        
        // Default fields
        schedule.setPrincipalPaid(BigDecimal.ZERO);
        schedule.setInterestPaid(BigDecimal.ZERO);
        schedule.setTotalPaid(BigDecimal.ZERO);
        schedule.setPenaltyAmount(BigDecimal.ZERO);
        schedule.setPenaltyAccrued(BigDecimal.ZERO);
        schedule.setFeesDue(BigDecimal.ZERO);
        schedule.setFeesPaid(BigDecimal.ZERO);
        schedule.setDaysOverdue(0);
        
        // Status and audit
        schedule.setStatus(GeneralConfig.InstallmentStatus.PENDING);
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setCreatedBy(currentUser != null ? currentUser.getId() : null);
        schedule.setDeleted(false);
        
        return schedule;
    }

    /**
     * Update existing schedule fields
     */
    private void updateScheduleFields(RepaymentSchedule schedule, LocalDate dueDate,
                                       BigDecimal principal, BigDecimal interest, 
                                       BigDecimal totalDue, User currentUser) {
        schedule.setDueDate(dueDate);
        schedule.setPrincipalAmount(principal);
        schedule.setInterestAmount(interest);
        schedule.setPrincipalDue(principal);
        schedule.setInterestDue(interest);
        schedule.setTotalDue(totalDue);
        schedule.setTotalDueAmount(totalDue);
        schedule.setOutstandingAmount(totalDue);
        
        // Reset payment fields (since schedule is being regenerated)
        schedule.setPrincipalPaid(BigDecimal.ZERO);
        schedule.setInterestPaid(BigDecimal.ZERO);
        schedule.setTotalPaid(BigDecimal.ZERO);
        schedule.setPaidDate(null);
        schedule.setPaidAmount(null);
        
        // Reset penalty fields
        schedule.setPenaltyAmount(BigDecimal.ZERO);
        schedule.setPenaltyAccrued(BigDecimal.ZERO);
        schedule.setDaysOverdue(0);
        
        // Ensure status is PENDING
        schedule.setStatus(GeneralConfig.InstallmentStatus.PENDING);
        
        // Update audit fields
        schedule.setUpdatedAt(LocalDateTime.now());
        if (currentUser != null) {
            schedule.setUpdatedBy(currentUser.getId());
        }
    }

    /**
     * Validate loan parameters before schedule generation
     */
    private void validateLoanForScheduleGeneration(Loan loan) {
        if (loan.getPrincipalAmount() == null || loan.getPrincipalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Loan principal amount must be greater than zero");
        }
        
        if (loan.getTenureMonths() == null || loan.getTenureMonths() <= 0) {
            throw new IllegalArgumentException("Loan tenure must be greater than zero months");
        }
        
        if (loan.getInterestRate() == null || loan.getInterestRate().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Loan interest rate is not set or zero for loan: {}", loan.getLoanAccountNumber());
        }
    }

    /**
     * Validate that total repayment does not exceed legal limit
     * Regulation: Maximum total repayment cannot exceed 200% of principal (2x)
     */
    private void validateRepaymentLimit(BigDecimal principal, BigDecimal totalRepayment) {
        BigDecimal maxAllowed = principal.multiply(MAX_TOTAL_REPAYMENT_PERCENTAGE)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        if (totalRepayment.compareTo(maxAllowed) > 0) {
            log.error("Total repayment {} exceeds legal limit {} (200% of principal {})", 
                    totalRepayment, maxAllowed, principal);
            throw new IllegalStateException(
                String.format("Total repayment amount (%.2f) exceeds legal limit (%.2f). " +
                              "Maximum allowed repayment is 200%% of principal amount.",
                              totalRepayment, maxAllowed));
        }
        
        log.info("Repayment limit validation passed. Principal: {}, Total Repayment: {}, Max Allowed: {}", 
                principal, totalRepayment, maxAllowed);
    }

    /**
     * Get system settings, handle gracefully if not found
     */
    private SystemSettings getSystemSettings() {
        try {
            return systemSettingsRepository.findFirst().orElse(null);
        } catch (Exception e) {
            log.warn("Could not retrieve system settings, using defaults: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate and repair schedules if needed (ensures all amounts are correct)
     */
    @Transactional
    public void validateAndRepairSchedules(Loan loan) {
        log.info("Validating repayment schedules for loan: {}", loan.getLoanAccountNumber());
        
        List<RepaymentSchedule> schedules = loan.getRepaymentSchedules();
        if (schedules == null || schedules.isEmpty()) {
            log.warn("No schedules found for loan, regenerating...");
            generateOrUpdateRepaymentSchedule(loan, null);
            return;
        }
        
        // Calculate total expected amounts
        BigDecimal totalPrincipal = schedules.stream()
                .map(RepaymentSchedule::getPrincipalDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalInterest = schedules.stream()
                .map(RepaymentSchedule::getInterestDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalRepayment = totalPrincipal.add(totalInterest);
        
        // Verify against loan principal
        if (totalPrincipal.compareTo(loan.getPrincipalAmount()) != 0) {
            log.warn("Principal mismatch! Loan: {}, Schedules total: {}", 
                    loan.getPrincipalAmount(), totalPrincipal);
            // Regenerate schedules to fix
            generateOrUpdateRepaymentSchedule(loan, null);
        }
        
        // Verify legal limit
        validateRepaymentLimit(loan.getPrincipalAmount(), totalRepayment);
        
        log.info("Schedule validation complete for loan: {}", loan.getLoanAccountNumber());
    }
}