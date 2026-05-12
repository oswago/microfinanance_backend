// RepaymentScheduleServiceImpl.java
package com.microfinance.loanapplications.service;

import com.microfinance.audit.service.AuditService;
import com.microfinance.base.entity.User;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.BusinessException;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.integrations.service.FinancialIntegrationService;
import com.microfinance.loanapplications.dto.RescheduleRequestDto;
import com.microfinance.loanapplications.dto.repayment.*;
import com.microfinance.loanapplications.entity.*;
import com.microfinance.loanapplications.repository.LoanRepository;
import com.microfinance.loanapplications.repository.RepaymentScheduleRepository;
import com.microfinance.loanapplications.repository.LoanRepaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RepaymentScheduleServiceImpl implements RepaymentScheduleService {

    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;

    @Autowired
    private final FinancialIntegrationService financialIntegrationService;
    @Autowired
    private final AuditService auditService;

    @Autowired
    private final PaymentAllocationService allocationService;

    @Autowired
    private final LoanRepaymentHelperService repaymentHelper;  // Inject the helper



    @Override
    public Page<RepaymentScheduleDto> getRepaymentSchedules(String status, Long branchId, Long loanProductId,
                                                            LocalDate startDate, LocalDate endDate,
                                                            String search, Pageable pageable) {
        log.info(">>> Fetching repayment schedules with filters");

        Page<Loan> loansPage = repaymentScheduleRepository.findLoansWithFilters(
                status, branchId, loanProductId, search, pageable);

        return loansPage.map(this::mapLoanToScheduleDto);
    }

    private RepaymentScheduleDto mapLoanToScheduleDto(Loan loan) {
        if (loan == null) return null;

        List<RepaymentSchedule> installments = loan.getRepaymentSchedules();
        if (installments == null) {
            installments = Collections.emptyList();
        }

        // Calculate statistics
        int totalInstallments = loan.getTenureMonths() != null ? loan.getTenureMonths() : installments.size();

        // Paid installments count
        int paidInstallments = (int) installments.stream()
                .filter(i -> i.getPaidAmount() != null &&
                        i.getPaidAmount().compareTo(i.getTotalDue()) >= 0)
                .count();

        // Total paid amount
        BigDecimal totalPaid = installments.stream()
                .map(i -> i.getPaidAmount() != null ? i.getPaidAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total due amount
        BigDecimal totalDue = installments.stream()
                .map(i -> i.getTotalDue() != null ? i.getTotalDue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total overdue amount
        BigDecimal totalOverdue = installments.stream()
                .filter(i -> i.getDueDate().isBefore(LocalDate.now()) &&
                        (i.getPaidAmount() == null ||
                                i.getPaidAmount().compareTo(i.getTotalDue()) < 0))
                .map(i -> {
                    BigDecimal paid = i.getPaidAmount() != null ? i.getPaidAmount() : BigDecimal.ZERO;
                    return i.getTotalDue().subtract(paid);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total interest
        BigDecimal totalInterest = installments.stream()
                .map(i -> i.getInterestAmount() != null ? i.getInterestAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Find next unpaid installment
        RepaymentSchedule nextInstallment = installments.stream()
                .filter(i -> i.getPaidAmount() == null ||
                        i.getPaidAmount().compareTo(i.getTotalDue()) < 0)
                .min(Comparator.comparing(RepaymentSchedule::getDueDate))
                .orElse(null);

        // Remaining installments count
        int remainingInstallments = nextInstallment != null ?
                (int) installments.stream()
                        .filter(i -> i.getPaidAmount() == null ||
                                i.getPaidAmount().compareTo(i.getTotalDue()) < 0)
                        .count() : 0;

        // Calculate total repayable
        BigDecimal totalRepayable = totalDue;

        // Get payment frequency (default to MONTHLY if not available)
        String paymentFrequency = "MONTHLY";
        if (loan.getLoanProduct() != null && loan.getLoanProduct().getPaymentFrequency() != null) {
            paymentFrequency = loan.getLoanProduct().getPaymentFrequency();
        }

        // Get next payment date and amount
        LocalDate nextPaymentDate = nextInstallment != null ? nextInstallment.getDueDate() : null;
        BigDecimal nextPaymentAmount = nextInstallment != null ? nextInstallment.getTotalDue() : BigDecimal.ZERO;

        return RepaymentScheduleDto.builder()
                // Basic loan information
                .id(loan.getId())
                .loanNumber(loan.getLoanAccountNumber())
                .loanId(loan.getId())

                // Borrower information
                .borrowerName(loan.getBorrower() != null ? loan.getBorrower().getFullName() : null)
                .borrowerIdNumber(loan.getBorrower() != null ? loan.getBorrower().getBorrowerNumber() : null)

                // Product information
                .loanProductName(loan.getLoanProduct() != null ? loan.getLoanProduct().getName() : null)
                .loanProductId(loan.getLoanProduct() != null ? loan.getLoanProduct().getId() : null)

                // Branch information
                .branchId(loan.getBranch() != null ? loan.getBranch().getId() : null)
                .branchName(loan.getBranch() != null ? loan.getBranch().getName() : null)

                // Schedule status (using loan status)
                .scheduleStatus(loan.getStatus() != null ? loan.getStatus().name() : null)

                // Next payment information
                .nextPaymentDate(nextPaymentDate)
                .nextPaymentAmount(nextPaymentAmount)

                // Installment counts
                .remainingInstallments(remainingInstallments)
                .totalInstallments(totalInstallments)
                .paidInstallments(paidInstallments)

                // Financial amounts
                .loanAmount(loan.getPrincipalAmount())
                .totalInterest(totalInterest)
                .totalRepayable(totalRepayable)
                .totalPaid(totalPaid)
                .totalDue(totalDue.subtract(totalPaid))
                .totalOverdue(totalOverdue)

                // Payment frequency
                .paymentFrequency(paymentFrequency)

                // Dates
                .disbursementDate(loan.getDisbursementDate())
                .maturityDate(loan.getMaturityDate())

                // Installments list (optional - for detailed view)
                .installments(installments.stream()
                        .map(this::mapToInstallmentDto)
                        .collect(Collectors.toList()))
                // Additional fields that might be needed
                .interestRate(loan.getInterestRate())
                .tenureMonths(loan.getTenureMonths())
                .outstandingBalance(loan.getOutstandingBalance())
                .build();
    }



    private InstallmentDto mapToInstallmentDto(RepaymentSchedule installment, List<LoanRepayment> repayments) {
        if (installment == null) return null;

        int daysOverdue = 0;
        if (installment.getDueDate() != null &&
                installment.getDueDate().isBefore(LocalDate.now()) &&
                (installment.getPaidAmount() == null ||
                        installment.getPaidAmount().compareTo(installment.getTotalDue()) < 0)) {
            daysOverdue = (int) ChronoUnit.DAYS.between(installment.getDueDate(), LocalDate.now());
        }

        String status;
        if (installment.getPaidAmount() != null &&
                installment.getPaidAmount().compareTo(installment.getTotalDue()) >= 0) {
            status = "PAID";
        } else if (daysOverdue > 0) {
            status = "OVERDUE";
        } else if (installment.getDueDate() != null &&
                (installment.getDueDate().isBefore(LocalDate.now()) ||
                        installment.getDueDate().isEqual(LocalDate.now()))) {
            status = "DUE";
        } else {
            status = "UPCOMING";
        }

        // Get the most recent repayment for payment details
        LoanRepayment latestRepayment = repayments.stream()
                .max((r1, r2) -> r1.getCreatedAt().compareTo(r2.getCreatedAt()))
                .orElse(null);

        // Map repayment history
        List<RepaymentSummaryDto> repaymentHistory = repayments.stream()
                .map(this::mapToRepaymentSummaryDto)
                .collect(Collectors.toList());

        return InstallmentDto.builder()
                .id(installment.getId())
                .installmentNumber(installment.getInstallmentNumber())
                .dueDate(installment.getDueDate())
                .principalAmount(installment.getPrincipalDue())
                .interestAmount(installment.getInterestDue())
                .totalAmount(installment.getTotalDue())
                .paidAmount(installment.getPaidAmount() != null ?
                        installment.getPaidAmount() : BigDecimal.ZERO)
                .paymentDate(installment.getPaidDate())
                .daysOverdue(daysOverdue)
                .status(status)

                // Payment details from latest repayment
                .paymentMethod(latestRepayment != null ?
                        latestRepayment.getPaymentMethod().name() :
                        installment.getPaymentMethod())
                .transactionReference(latestRepayment != null ?
                        latestRepayment.getTransactionReference() :
                        installment.getTransactionReference())
                .receiptNumber(latestRepayment != null ?
                        latestRepayment.getReceiptNumber() : null)
                .penaltyAmount(latestRepayment != null ?
                        latestRepayment.getPenaltyAmount() : BigDecimal.ZERO)
                .feesAmount(latestRepayment != null ?
                        latestRepayment.getFeesAmount() : BigDecimal.ZERO)
                .notes(latestRepayment != null ?
                        latestRepayment.getNotes() :
                        installment.getNotes())
                .receivedBy(latestRepayment != null && latestRepayment.getReceivedBy() != null ?
                        latestRepayment.getReceivedBy().getUsername() : null)
                .createdAt(latestRepayment != null ?
                        latestRepayment.getCreatedAt() : null)
                .repaymentHistory(repaymentHistory)
                .build();
    }

    private RepaymentSummaryDto mapToRepaymentSummaryDto(LoanRepayment repayment) {
        if (repayment == null) return null;

        return RepaymentSummaryDto.builder()
                .id(repayment.getId())
                .receiptNumber(repayment.getReceiptNumber())
                .paymentDate(repayment.getPaymentDate())
                .amountPaid(repayment.getAmountPaid())
                .principalAmount(repayment.getPrincipalAmount())
                .interestAmount(repayment.getInterestAmount())
                .penaltyAmount(repayment.getPenaltyAmount())
                .feesAmount(repayment.getFeesAmount())
                .paymentMethod(repayment.getPaymentMethod() != null ?
                        repayment.getPaymentMethod().name() : null)
                .transactionReference(repayment.getTransactionReference())
                .receivedBy(repayment.getReceivedBy() != null ?
                        repayment.getReceivedBy().getUsername() : null)
                .createdAt(repayment.getCreatedAt())
                .status(repayment.getStatus() != null ?
                        repayment.getStatus().name() : null)
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public List<RepaymentScheduleDto> getRepaymentSchedulesByLoanId(Long loanId) {
        log.debug("Fetching all repayment schedules for loan ID: {}", loanId);

        // Verify loan exists (you may want to add this check)
        // loanRepository.findById(loanId).orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        List<RepaymentSchedule> schedules = repaymentScheduleRepository.findByLoanIdOrderByDueDateAsc(loanId);

        return schedules.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RepaymentScheduleDto> getRepaymentSchedulesByLoanId(Long loanId, Pageable pageable) {
        log.debug("Fetching paginated repayment schedules for loan ID: {}, page: {}", loanId, pageable.getPageNumber());

        Page<RepaymentSchedule> schedulesPage = repaymentScheduleRepository.findByLoanId(loanId, pageable);

        return schedulesPage.map(this::convertToDto);
    }

    /**
     * Helper method to convert RepaymentSchedule entity to DTO
     */
    private RepaymentScheduleDto convertToDto(RepaymentSchedule schedule) {
        return RepaymentScheduleDto.builder()
                .id(schedule.getId())
                .loanId(schedule.getLoan().getId())
                .loanAccountNumber(schedule.getLoan().getLoanAccountNumber())
                .borrowerName(schedule.getLoan().getBorrower() != null ?
                        schedule.getLoan().getBorrower().getFullName() : null)
                .installmentNumber(schedule.getInstallmentNumber())
                .dueDate(schedule.getDueDate())
                .principalDue(schedule.getPrincipalDue())
                .interestDue(schedule.getInterestDue())
                .totalDue(schedule.getTotalDue())
                .principalPaid(schedule.getPrincipalPaid())
                .interestPaid(schedule.getInterestPaid())
                .totalPaid(schedule.getTotalPaid())
                .outstandingPrincipal(schedule.getPrincipalDue().subtract(schedule.getPrincipalPaid()))
                .outstandingInterest(schedule.getInterestDue().subtract(schedule.getInterestPaid()))
                .totalOutstanding(schedule.getOutstandingAmount())
                .status(schedule.getStatus() != null ? schedule.getStatus().name() : null)
                .isOverdue(schedule.isOverdue())
                .daysOverdue(schedule.getDaysOverdue())
                .paidDate(schedule.getPaidDate())
                .paymentMethod(schedule.getPaymentMethod())
                .transactionReference(schedule.getTransactionReference())
                .build();
    }



    /**
     * Helper method to map a single installment to InstallmentDto
     */
    private InstallmentDto mapToInstallmentDto(RepaymentSchedule installment) {
        if (installment == null) return null;

        int daysOverdue = 0;
        if (installment.getDueDate() != null &&
                installment.getDueDate().isBefore(LocalDate.now()) &&
                (installment.getPaidAmount() == null ||
                        installment.getPaidAmount().compareTo(installment.getTotalDue()) < 0)) {
            daysOverdue = (int) ChronoUnit.DAYS.between(installment.getDueDate(), LocalDate.now());
        }

        String status;
        if (installment.getPaidAmount() != null &&
                installment.getPaidAmount().compareTo(installment.getTotalDue()) >= 0) {
            status = "PAID";
        } else if (daysOverdue > 0) {
            status = "OVERDUE";
        } else if (installment.getDueDate() != null &&
                installment.getDueDate().isBefore(LocalDate.now()) ||
                installment.getDueDate().isEqual(LocalDate.now())) {
            status = "DUE";
        } else {
            status = "UPCOMING";
        }

        return InstallmentDto.builder()
                .id(installment.getId())
                .installmentNumber(installment.getInstallmentNumber())
                .dueDate(installment.getDueDate())
                .principalAmount(installment.getPrincipalDue())
                .interestAmount(installment.getInterestDue())
                .totalAmount(installment.getTotalDue())
                .paidAmount(installment.getPaidAmount() != null ?
                        installment.getPaidAmount() : BigDecimal.ZERO)
                .paymentDate(installment.getPaidDate())
                .daysOverdue(daysOverdue)
                .status(status)
                .paymentMethod(installment.getPaymentMethod() != null ?
                        installment.getPaymentMethod() : null)
                .transactionReference(installment.getTransactionReference())
                .notes(installment.getNotes())
                .build();
    }


    // New method specifically for mapping a RepaymentSchedule to a DTO
    private RepaymentScheduleDto mapScheduleToDto(RepaymentSchedule schedule) {
        if (schedule == null) return null;

        Loan loan = schedule.getLoan();
        if (loan == null) return null;

        // Get all installments for this loan
        List<RepaymentSchedule> allInstallments = loan.getRepaymentSchedules();

        // Calculate statistics
        int totalInstallments = loan.getTenureMonths() != null ? loan.getTenureMonths() : 0;
        int paidInstallments = allInstallments != null ?
                (int) allInstallments.stream()
                        .filter(i -> i.getPaidAmount() != null &&
                                i.getPaidAmount().compareTo(i.getTotalDue()) >= 0)
                        .count() : 0;

        BigDecimal totalPaid = allInstallments != null ?
                allInstallments.stream()
                        .map(i -> i.getPaidAmount() != null ? i.getPaidAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        BigDecimal totalDue = allInstallments != null ?
                allInstallments.stream()
                        .map(i -> i.getTotalDue() != null ? i.getTotalDue() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        BigDecimal totalOverdue = allInstallments != null ?
                allInstallments.stream()
                        .filter(i -> i.getDueDate().isBefore(LocalDate.now()) &&
                                (i.getPaidAmount() == null ||
                                        i.getPaidAmount().compareTo(i.getTotalDue()) < 0))
                        .map(i -> i.getTotalDue().subtract(
                                i.getPaidAmount() != null ? i.getPaidAmount() : BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        RepaymentSchedule nextInstallment = allInstallments != null ?
                allInstallments.stream()
                        .filter(i -> i.getPaidAmount() == null ||
                                i.getPaidAmount().compareTo(i.getTotalDue()) < 0)
                        .min(Comparator.comparing(RepaymentSchedule::getDueDate))
                        .orElse(null) : null;

        BigDecimal totalInterest = allInstallments != null ?
                allInstallments.stream()
                        .map(i -> i.getInterestAmount() != null ? i.getInterestAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        return RepaymentScheduleDto.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanAccountNumber())
                .borrowerName(loan.getBorrower() != null ? loan.getBorrower().getFullName() : null)
                .borrowerIdNumber(loan.getBorrower() != null ? loan.getBorrower().getBorrowerNumber() : null)
                .loanProductName(loan.getLoanProduct() != null ? loan.getLoanProduct().getName() : null)
                .scheduleStatus(loan.getStatus() != null ? loan.getStatus().name() : null)
                .nextPaymentDate(nextInstallment != null ? nextInstallment.getDueDate() : null)
                .nextPaymentAmount(nextInstallment != null ? nextInstallment.getTotalDue() : null)
                .remainingInstallments(nextInstallment != null ?
                        (int) allInstallments.stream()
                                .filter(i -> i.getPaidAmount() == null ||
                                        i.getPaidAmount().compareTo(i.getTotalDue()) < 0)
                                .count() : 0)
                .totalInstallments(totalInstallments)
                .loanAmount(loan.getPrincipalAmount())
                .totalInterest(totalInterest)
                .totalRepayable(totalDue)
                .paidInstallments(paidInstallments)
                .paymentFrequency("MONTHLY")
                .totalPaid(totalPaid)
                .totalDue(totalDue.subtract(totalPaid))
                .totalOverdue(totalOverdue)
                .disbursementDate(loan.getDisbursementDate())
                .loanId(loan.getId())
                .branchId(loan.getBranch() != null ? loan.getBranch().getId() : null)
                .loanProductId(loan.getLoanProduct() != null ? loan.getLoanProduct().getId() : null)
                .build();
    }


    @Override
    public RepaymentScheduleDto getRepaymentScheduleById(Long id) {
        log.info("Fetching repayment schedule with id: {}", id);

        RepaymentSchedule schedule = repaymentScheduleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repayment schedule not found with id: " + id));

        return mapToDto(schedule.getLoan());
    }

    @Override
    public List<InstallmentDto> getScheduleInstallments(Long scheduleId) {
        log.info("Fetching installments for schedule: {}", scheduleId);

        List<RepaymentSchedule> installments = repaymentScheduleRepository.findByLoanIdOrderByInstallmentNumber(scheduleId);

        return installments.stream()
                .map(this::mapToInstallmentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RepaymentScheduleDto generateRepaymentSchedule(CreateScheduleRequestDto requestDto, User currentUser) {
        log.info("Generating repayment schedule for loan: {}", requestDto.getLoanId());

        Loan loan = loanRepository.findById(requestDto.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + requestDto.getLoanId()));

        // Check if schedule already exists
        if (!loan.getRepaymentSchedules().isEmpty()) {
            throw new BusinessException("Loan already has a repayment schedule");
        }

        // Generate installments based on loan terms
        List<RepaymentSchedule> installments = generateInstallments(loan, requestDto);

        // Save all installments
        List<RepaymentSchedule> savedInstallments = repaymentScheduleRepository.saveAll(installments);

        // Update loan with schedule reference
        loan.setRepaymentSchedules(savedInstallments);
        loanRepository.save(loan);

        log.info("Generated {} installments for loan: {}", savedInstallments.size(), loan.getLoanAccountNumber());

        return mapToDto(loan);
    }

    @Override
    public byte[] exportSchedules(String status, Long branchId, Long loanProductId,
                                  LocalDate startDate, LocalDate endDate, String format) {
        log.info("Exporting repayment schedules in format: {}", format);

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<RepaymentScheduleDto> schedules = getRepaymentSchedules(
                status, branchId, loanProductId, startDate, endDate, null, pageable);

        try {
            if ("PDF".equalsIgnoreCase(format)) {
                return generateSchedulesPdfReport(schedules.getContent());
            } else if ("EXCEL".equalsIgnoreCase(format)) {
                return generateSchedulesExcelReport(schedules.getContent());
            } else {
                throw new BusinessException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            log.error("Error exporting schedules", e);
            throw new BusinessException("Failed to export schedules: " + e.getMessage());
        }
    }

    @Override
    public byte[] exportSchedule(Long scheduleId, String format) {
        log.info("Exporting schedule {} in format: {}", scheduleId, format);

        RepaymentScheduleDto schedule = getRepaymentScheduleById(scheduleId);

        try {
            if ("PDF".equalsIgnoreCase(format)) {
                return generateSchedulePdfReport(schedule);
            } else if ("EXCEL".equalsIgnoreCase(format)) {
                return generateScheduleExcelReport(schedule);
            } else {
                throw new BusinessException("Unsupported export format: " + format);
            }
        } catch (Exception e) {
            log.error("Error exporting schedule", e);
            throw new BusinessException("Failed to export schedule: " + e.getMessage());
        }
    }

    @Override
    public byte[] printSchedule(Long scheduleId) {
        log.info("Printing schedule: {}", scheduleId);

        RepaymentScheduleDto schedule = getRepaymentScheduleById(scheduleId);

        try {
            return generateSchedulePrintPdf(schedule);
        } catch (Exception e) {
            log.error("Error printing schedule", e);
            throw new BusinessException("Failed to print schedule: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void sendReminder(Long scheduleId, User currentUser) {
        log.info("Sending payment reminder for schedule: {}", scheduleId);

        RepaymentSchedule schedule = repaymentScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));

        // In a real implementation, this would send an email/SMS
        // For now, we'll just log it
        log.info("Reminder sent for schedule: {} by user: {}", scheduleId, currentUser.getUsername());

        // You could implement actual notification logic here
        // notificationService.sendPaymentReminder(schedule);
    }

    @Override
    @Transactional
    public RepaymentScheduleDto closeSchedule(Long scheduleId, User currentUser) {
        log.info("Closing repayment schedule: {} by user: {}", scheduleId, currentUser.getUsername());

        RepaymentSchedule schedule = repaymentScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));

        Loan loan = schedule.getLoan();

        // Verify all installments are paid
        boolean allPaid = loan.getRepaymentSchedules().stream()
                .allMatch(inst -> inst.getPaidAmount() != null &&
                        inst.getPaidAmount().compareTo(inst.getTotalDue()) >= 0);

        if (!allPaid) {
            throw new BusinessException("Cannot close schedule: not all installments are paid");
        }

        // Update loan status to CLOSED
        loan.setStatus(GeneralConfig.LoanStatus.CLOSED);
        loan.setClosedDate(LocalDate.now());
        loan.setClosedBy(currentUser);
        loanRepository.save(loan);

        log.info("Repayment schedule closed for loan: {}", loan.getLoanAccountNumber());

        return mapToDto(loan);
    }

    @Override
    public ScheduleStatisticsDto getScheduleStatistics(Long branchId, LocalDate asOfDate) {
        log.info("Fetching schedule statistics for branch: {}, as of: {}", branchId, asOfDate);

        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }

        LocalDate startOfMonth = asOfDate.withDayOfMonth(1);

        // Active schedules count
        Long activeSchedules = repaymentScheduleRepository.countActiveSchedules(branchId, asOfDate);

        // Total amount due
        BigDecimal totalAmountDue = repaymentScheduleRepository.sumTotalDue(branchId, asOfDate);

        // Upcoming payments (next 7 days)
        LocalDate sevenDaysFromNow = asOfDate.plusDays(7);
        Long upcomingPayments = repaymentScheduleRepository.countUpcomingPayments(
                asOfDate, sevenDaysFromNow, branchId);

        // Overdue payments
        Long overduePayments = repaymentScheduleRepository.countOverduePayments(asOfDate, branchId);

        // Total collected this month
        BigDecimal totalCollected = loanRepaymentRepository.getTotalCollectionBetweenDates(
                startOfMonth, asOfDate);

        // Collection rate (simple calculation)
        BigDecimal collectionRate = totalAmountDue.compareTo(BigDecimal.ZERO) > 0 ?
                totalCollected.divide(totalAmountDue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

        return ScheduleStatisticsDto.builder()
                .activeSchedules(activeSchedules != null ? activeSchedules : 0L)
                .totalAmountDue(totalAmountDue != null ? totalAmountDue : BigDecimal.ZERO)
                .upcomingPayments(upcomingPayments != null ? upcomingPayments : 0L)
                .overduePayments(overduePayments != null ? overduePayments : 0L)
                .totalCollected(totalCollected != null ? totalCollected : BigDecimal.ZERO)
                .collectionRate(BigDecimal.valueOf(collectionRate.doubleValue()))
                .build();
    }

    @Override
    public List<InstallmentDto> getUpcomingPayments(int days, Long branchId) {
        log.info("Fetching upcoming payments for next {} days, branch: {}", days, branchId);

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);

        List<RepaymentSchedule> upcoming = repaymentScheduleRepository.findUpcomingPayments(
                today, endDate, branchId);

        return upcoming.stream()
                .map(this::mapToInstallmentDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<InstallmentDto> getOverduePayments(Long branchId) {
        log.info("Fetching overdue payments for branch: {}", branchId);

        LocalDate today = LocalDate.now();

        List<RepaymentSchedule> overdue = repaymentScheduleRepository.findOverduePayments(today, branchId);

        return overdue.stream()
                .map(this::mapToInstallmentDto)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public InstallmentDto recordInstallmentPayment(InstallmentPaymentDto paymentDto, User currentUser) {
        log.info("Recording payment for installment: {}", paymentDto.getInstallmentId());

        // Validate using centralized helper
        repaymentHelper.validateInstallmentPaymentDto(paymentDto);

        RepaymentSchedule installment = repaymentScheduleRepository.findById(paymentDto.getInstallmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found"));

        // Check if installment is already fully paid
        InstallmentRemainingDto remaining = allocationService.getRemainingAmounts(installment);
        if (remaining.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Installment is already fully paid");
        }

        BigDecimal paymentAmount = paymentDto.getAmountPaid();

        if (paymentAmount.compareTo(remaining.getTotal()) > 0) {
            throw new BusinessException("Payment amount exceeds remaining balance. Remaining: " + remaining.getTotal());
        }

        // Calculate allocation using centralized service
        SingleInstallmentAllocation allocation = allocationService.allocateToSingleInstallment(installment, paymentAmount);

        // Apply allocation to installment using centralized service
        allocationService.applyAllocationToInstallment(installment, allocation);

        // Set payment metadata
        installment.setPaidDate(paymentDto.getPaymentDate());
        installment.setPaymentDate(paymentDto.getPaymentDate());
        installment.setPaymentMethod(paymentDto.getPaymentMethod());
        installment.setTransactionReference(paymentDto.getTransactionReference());
        installment.setNotes(paymentDto.getNotes());

        // Create repayment record using centralized helper
        LoanRepayment savedRepayment = repaymentHelper.createRepaymentRecordFromInstallment(
                installment, allocation, paymentDto, currentUser);

        // Save installment updates
        repaymentScheduleRepository.save(installment);

        // Update loan totals using centralized helper
        repaymentHelper.updateLoanTotalsFromInstallment(
                installment.getLoan(), allocation, paymentAmount, currentUser);

        log.info("Payment recorded for installment: {}. Principal: {}, Interest: {}, Penalty: {}, Total: {}",
                paymentDto.getInstallmentId(), allocation.getPrincipalPaid(),
                allocation.getInterestPaid(), allocation.getPenaltyPaid(), paymentAmount);

        // Integrate Financials
        Loan loan = installment.getLoan();
        loan.updateFinancialTrackingFields();
        auditService.logRepaymentAction(savedRepayment.getId(), currentUser.getId(), paymentAmount);
        financialIntegrationService.recordLoanRepayment(loan, savedRepayment, currentUser);

        // Fetch all repayments for this installment
        List<LoanRepayment> repayments = loanRepaymentRepository.findByInstallmentId(installment.getId());

        return mapToInstallmentDto(installment, repayments);
    }



        private LoanRepayment createRepaymentRecord(RepaymentSchedule installment,
                                                    SingleInstallmentAllocation allocation,
                                                    InstallmentPaymentDto paymentDto,
                                                    User currentUser) {
            LoanRepayment repayment = new LoanRepayment();
            repayment.setLoan(installment.getLoan());
            repayment.setBorrower(installment.getLoan().getBorrower());
            repayment.setAmountPaid(paymentDto.getAmountPaid());
            repayment.setAmount(paymentDto.getAmountPaid());
            repayment.setPrincipalAmount(allocation.getPrincipalPaid());
            repayment.setInterestAmount(allocation.getInterestPaid());
            repayment.setPenaltyAmount(allocation.getPenaltyPaid());
            repayment.setFeesAmount(allocation.getFeesPaid());
            repayment.setPaymentDate(paymentDto.getPaymentDate());
            repayment.setPaymentMethod(GeneralConfig.PaymentMethod.valueOf(paymentDto.getPaymentMethod()));
            repayment.setTransactionReference(paymentDto.getTransactionReference());
            repayment.setNotes(paymentDto.getNotes());
            repayment.setReceivedBy(currentUser);
            repayment.generateReceiptNumber();
            repayment.setStatus(GeneralConfig.RepaymentStatus.COMPLETED);
            repayment.setIsReversed(false);
            repayment.setCreatedBy(currentUser.getId());
            repayment.setInstallment(installment);

            // Set allocated installments
            List<RepaymentSchedule> allocatedInstallments = new ArrayList<>();
            allocatedInstallments.add(installment);
            repayment.setAllocatedInstallments(allocatedInstallments);

            return repayment;
        }

        private void updateLoanTotals(Loan loan, SingleInstallmentAllocation allocation,
                                      BigDecimal paymentAmount, User currentUser) {
            // Update loan tracking fields
            BigDecimal currentPrincipalPaid = loan.getPrincipalPaid() != null ? loan.getPrincipalPaid() : BigDecimal.ZERO;
            BigDecimal currentInterestPaid = loan.getInterestPaid() != null ? loan.getInterestPaid() : BigDecimal.ZERO;
            BigDecimal currentPenaltyPaid = loan.getPenaltyPaid() != null ? loan.getPenaltyPaid() : BigDecimal.ZERO;
            BigDecimal currentFeesPaid = loan.getFeesPaid() != null ? loan.getFeesPaid() : BigDecimal.ZERO;

            loan.setPrincipalPaid(currentPrincipalPaid.add(allocation.getPrincipalPaid()));
            loan.setInterestPaid(currentInterestPaid.add(allocation.getInterestPaid()));
            loan.setPenaltyPaid(currentPenaltyPaid.add(allocation.getPenaltyPaid()));
            loan.setFeesPaid(currentFeesPaid);

            // Update principal outstanding
            loan.setPrincipalOutstanding(loan.getPrincipalAmount().subtract(loan.getPrincipalPaid()));

            // Update total paid and outstanding balance
            loan.setTotalPaid(loan.getTotalPaid() != null ? loan.getTotalPaid().add(paymentAmount) : paymentAmount);

            // Calculate outstanding balance
            BigDecimal totalPrincipalAndInterest = loan.getPrincipalAmount()
                    .add(loan.getTotalInterestDue() != null ? loan.getTotalInterestDue() : BigDecimal.ZERO);
            loan.setOutstandingBalance(totalPrincipalAndInterest.subtract(loan.getTotalPaid()));

            if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
                loan.setStatus(GeneralConfig.LoanStatus.CLOSED);
                loan.setClosedDate(LocalDate.now());
                loan.setClosedBy(currentUser);
            }

            loanRepository.save(loan);
        }




    /**
     * Helper method to get remaining amounts for an installment
     */
    private InstallmentRemainingDto getRemainingAmounts(RepaymentSchedule installment) {
        BigDecimal remainingPrincipal = installment.getPrincipalDue().subtract(installment.getPrincipalPaid());
        BigDecimal remainingInterest = installment.getInterestDue().subtract(installment.getInterestPaid());
        BigDecimal remainingPenalty = installment.getPenaltyAccrued();
        BigDecimal remainingFees = (installment.getFeesDue() != null ? installment.getFeesDue() : BigDecimal.ZERO)
                .subtract(installment.getFeesPaid() != null ? installment.getFeesPaid() : BigDecimal.ZERO);

        return InstallmentRemainingDto.builder()
                .principal(remainingPrincipal)
                .interest(remainingInterest)
                .penalty(remainingPenalty)
                .fees(remainingFees)
                .total(remainingPrincipal.add(remainingInterest).add(remainingPenalty).add(remainingFees))
                .build();
    }



    @Override
    public InstallmentDto getInstallmentDetails(Long installmentId) {
        log.info("Fetching installment details: {}", installmentId);

        RepaymentSchedule installment = repaymentScheduleRepository.findById(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found with id: " + installmentId));
        // Fetch all repayments for this installment
        List<LoanRepayment> repayments = loanRepaymentRepository.findByInstallmentId(installmentId);

        return mapToInstallmentDto(installment,repayments);
    }

    /*
@Override
public List<LoanRepayment> getInstallmentPaymentHistory(Long installmentId) {
        log.info("Fetching payment history for installment: {}", installmentId);

        return loanRepaymentRepository.findByInstallmentId(installmentId);
    }
*/
    @Transactional(readOnly = true)
    public List<RepaymentSummaryDto> getInstallmentPaymentHistory(Long installmentId) {
        log.info("Fetching payment history for installment: {}", installmentId);

        // Verify installment exists
        if (!repaymentScheduleRepository.existsById(installmentId)) {
            throw new ResourceNotFoundException("Installment not found with id: " + installmentId);
        }

        // Fetch all repayments for this installment
        List<LoanRepayment> repayments = loanRepaymentRepository.findByInstallmentId(installmentId);

        // Map to DTOs
        return repayments.stream()
                .map(this::mapToRepaymentSummaryDto)
                .collect(Collectors.toList());
    }


    @Override
    public byte[] generateScheduleStatement(Long scheduleId) {
        log.info("Generating statement for schedule: {}", scheduleId);

        RepaymentScheduleDto schedule = getRepaymentScheduleById(scheduleId);

        try {
            return generateScheduleStatementPdf(schedule);
        } catch (Exception e) {
            log.error("Error generating statement", e);
            throw new BusinessException("Failed to generate statement: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public RepaymentScheduleDto rescheduleSchedule(Long scheduleId, RescheduleRequestDto requestDto, User currentUser) {
        log.info("Rescheduling schedule: {} by user: {}", scheduleId, currentUser.getUsername());

        RepaymentSchedule schedule = repaymentScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));

        Loan loan = schedule.getLoan();

        // Validate rescheduling conditions
        if (loan.getStatus() != GeneralConfig.LoanStatus.ACTIVE) {
            throw new BusinessException("Only active loans can be rescheduled");
        }

        // Calculate new due dates for remaining installments
        LocalDate newStartDate = requestDto.getNewStartDate() != null ?
                requestDto.getNewStartDate() : LocalDate.now();

        List<RepaymentSchedule> remainingInstallments = loan.getRepaymentSchedules().stream()
                .filter(inst -> inst.getPaidAmount() == null ||
                        inst.getPaidAmount().compareTo(inst.getTotalDue()) < 0)
                .sorted(Comparator.comparing(RepaymentSchedule::getDueDate))
                .collect(Collectors.toList());

        // Recalculate due dates
        for (int i = 0; i < remainingInstallments.size(); i++) {
            RepaymentSchedule inst = remainingInstallments.get(i);
            LocalDate newDueDate = newStartDate.plusMonths(i + 1);
            inst.setDueDate(newDueDate);
            repaymentScheduleRepository.save(inst);
        }

        log.info("Schedule rescheduled for loan: {}. New start date: {}", loan.getLoanAccountNumber(), newStartDate);

        return mapToDto(loan);
    }

    @Override
    public List<CalendarEventDto> getScheduleCalendar(Integer month, Integer year, Long branchId) {
        log.info("Fetching schedule calendar for month: {}, year: {}, branch: {}", month, year, branchId);

        if (month == null) month = LocalDate.now().getMonthValue();
        if (year == null) year = LocalDate.now().getYear();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<RepaymentSchedule> installments = repaymentScheduleRepository.findInstallmentsInDateRange(
                startDate, endDate, branchId);

        return installments.stream()
                .map(this::mapToCalendarEvent)
                .collect(Collectors.toList());
    }


    public byte[] generateDueReportsBK(LocalDate startDate, LocalDate endDate, Long branchId, String format) {
        log.info("Generating due reports for period: {} to {}, branch: {}, format: {}",
                startDate, endDate, branchId, format);

        List<RepaymentSchedule> dueInstallments = repaymentScheduleRepository.findDueInstallmentsInDateRange(
                startDate, endDate, branchId);

        try {
            if ("PDF".equalsIgnoreCase(format)) {
                return generateDueReportsPdf(dueInstallments, startDate, endDate, branchId);
            } else if ("EXCEL".equalsIgnoreCase(format)) {
                return generateDueReportsExcel(dueInstallments, startDate, endDate, branchId);
            } else {
                throw new BusinessException("Unsupported format: " + format);
            }
        } catch (Exception e) {
            log.error("Error generating due reports", e);
            throw new BusinessException("Failed to generate due reports: " + e.getMessage());
        }
    }


    @Override
    public byte[] generateDueReports(LocalDate startDate, LocalDate endDate, Long branchId, String format) {
        log.info("========== GENERATING DUE REPORTS ==========");
        log.info("Period: {} to {}, branch: {}, format: {}", startDate, endDate, branchId, format);

        // Log the query parameters
        log.info("Fetching due installments with params - startDate: {}, endDate: {}, branchId: {}",
                startDate, endDate, branchId);

        List<RepaymentSchedule> dueInstallments = repaymentScheduleRepository.findDueInstallmentsInDateRange(
                startDate, endDate, branchId);

        // Log the results
        log.info("Found {} due installments", dueInstallments.size());

        if (dueInstallments.isEmpty()) {
            log.warn("No due installments found for the given criteria");
        } else {
            // Log first few installments to see the data
            log.info("Sample of first 3 installments:");
            dueInstallments.stream().limit(3).forEach(inst -> {
                log.info("  Installment ID: {}, Loan: {}, Due Date: {}, Amount: {}, Status: {}",
                        inst.getId(),
                        inst.getLoan() != null ? inst.getLoan().getLoanAccountNumber() : "N/A",
                        inst.getDueDate(),
                        inst.getTotalDue(),
                        inst.getStatus());
            });

            // Calculate totals
            BigDecimal totalAmount = dueInstallments.stream()
                    .map(RepaymentSchedule::getTotalDue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.info("Total amount due: {}", totalAmount);

            // Log by status
            Map<GeneralConfig.InstallmentStatus, Long> countByStatus = dueInstallments.stream()
                    .collect(Collectors.groupingBy(RepaymentSchedule::getStatus, Collectors.counting()));
            log.info("Count by status: {}", countByStatus);
        }

        try {
            if ("PDF".equalsIgnoreCase(format)) {
                log.info("Generating PDF report with {} installments", dueInstallments.size());
                byte[] pdfData = generateDueReportsPdf(dueInstallments, startDate, endDate, branchId);
                log.info("PDF generated, size: {} bytes", pdfData.length);

                // Log first few bytes to verify PDF header
                if (pdfData.length > 5) {
                    String header = new String(pdfData, 0, 5, StandardCharsets.US_ASCII);
                    log.info("PDF header: {}", header);
                }

                return pdfData;

            } else if ("EXCEL".equalsIgnoreCase(format)) {
                log.info("Generating Excel report with {} installments", dueInstallments.size());
                byte[] excelData = generateDueReportsExcel(dueInstallments, startDate, endDate, branchId);
                log.info("Excel generated, size: {} bytes", excelData.length);
                return excelData;

            } else {
                log.error("Unsupported format: {}", format);
                throw new BusinessException("Unsupported format: " + format);
            }
        } catch (Exception e) {
            log.error("Error generating due reports", e);
            throw new BusinessException("Failed to generate due reports: " + e.getMessage());
        } finally {
            log.info("========== END DUE REPORTS GENERATION ==========");
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private List<RepaymentSchedule> generateInstallments(Loan loan, CreateScheduleRequestDto requestDto) {
        List<RepaymentSchedule> installments = new ArrayList<>();

        BigDecimal monthlyPrincipal = loan.getPrincipalAmount()
                .divide(BigDecimal.valueOf(loan.getTenureMonths()), 2, RoundingMode.HALF_UP);

        BigDecimal monthlyInterestRate = loan.getInterestRate()
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

        BigDecimal remainingPrincipal = loan.getPrincipalAmount();
        LocalDate startDate = requestDto.getFirstPaymentDate() != null ?
                requestDto.getFirstPaymentDate() : LocalDate.now().plusMonths(1);

        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            LocalDate dueDate = startDate.plusMonths(i - 1);

            BigDecimal interest = remainingPrincipal.multiply(monthlyInterestRate)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal principalForThisInstallment = monthlyPrincipal;
            if (i == loan.getTenureMonths()) {
                principalForThisInstallment = remainingPrincipal;
            }

            BigDecimal totalDue = principalForThisInstallment.add(interest);

            RepaymentSchedule installment = new RepaymentSchedule();
            installment.setLoan(loan);
            installment.setInstallmentNumber(i);
            installment.setDueDate(dueDate);
            installment.setPrincipalAmount(principalForThisInstallment);
            installment.setInterestAmount(interest);
            installment.setPrincipalDue(principalForThisInstallment);
            installment.setInterestDue(interest);
            installment.setTotalDue(totalDue);
            installment.setTotalDueAmount(totalDue);
            installment.setOutstandingAmount(totalDue);
            installment.setStatus(GeneralConfig.InstallmentStatus.PENDING);
            installment.setCreatedAt(LocalDateTime.now());
            installment.setDeleted(false);

            installments.add(installment);
            remainingPrincipal = remainingPrincipal.subtract(principalForThisInstallment);
        }

        return installments;
    }

    private RepaymentScheduleDto mapToDto(Loan loan) {
        if (loan == null) return null;

        List<RepaymentSchedule> installments = loan.getRepaymentSchedules();

        int totalInstallments = loan.getTenureMonths() != null ? loan.getTenureMonths() : 0;
        int paidInstallments = installments != null ?
                (int) installments.stream()
                        .filter(i -> i.getPaidAmount() != null &&
                                i.getPaidAmount().compareTo(i.getTotalDue()) >= 0)
                        .count() : 0;

        BigDecimal totalPaid = installments != null ?
                installments.stream()
                        .map(i -> i.getPaidAmount() != null ? i.getPaidAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        BigDecimal totalDue = installments != null ?
                installments.stream()
                        .map(i -> i.getTotalDue() != null ? i.getTotalDue() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        BigDecimal totalOverdue = installments != null ?
                installments.stream()
                        .filter(i -> i.getDueDate().isBefore(LocalDate.now()) &&
                                (i.getPaidAmount() == null ||
                                        i.getPaidAmount().compareTo(i.getTotalDue()) < 0))
                        .map(i -> i.getTotalDue().subtract(
                                i.getPaidAmount() != null ? i.getPaidAmount() : BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        RepaymentSchedule nextInstallment = installments != null ?
                installments.stream()
                        .filter(i -> i.getPaidAmount() == null ||
                                i.getPaidAmount().compareTo(i.getTotalDue()) < 0)
                        .min(Comparator.comparing(RepaymentSchedule::getDueDate))
                        .orElse(null) : null;

        BigDecimal totalInterest = installments != null ?
                installments.stream()
                        .map(i -> i.getInterestAmount() != null ? i.getInterestAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        return RepaymentScheduleDto.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanAccountNumber())
                .borrowerName(loan.getBorrower() != null ? loan.getBorrower().getFullName() : null)
                .borrowerIdNumber(loan.getBorrower() != null ? loan.getBorrower().getBorrowerNumber() : null)
                .loanProductName(loan.getLoanProduct() != null ? loan.getLoanProduct().getName() : null)
                .scheduleStatus(loan.getStatus() != null ? loan.getStatus().name() : null)
                .nextPaymentDate(nextInstallment != null ? nextInstallment.getDueDate() : null)
                .nextPaymentAmount(nextInstallment != null ? nextInstallment.getTotalDue() : null)
                .remainingInstallments(nextInstallment != null ?
                        (int) installments.stream()
                                .filter(i -> i.getPaidAmount() == null ||
                                        i.getPaidAmount().compareTo(i.getTotalDue()) < 0)
                                .count() : 0)
                .totalInstallments(totalInstallments)
                .loanAmount(loan.getPrincipalAmount())
                .totalInterest(totalInterest)
                .totalRepayable(totalDue)
                .paidInstallments(paidInstallments)
                .paymentFrequency("MONTHLY") // Default, could be from loan product
                .totalPaid(totalPaid)
                .totalDue(totalDue.subtract(totalPaid))
                .totalOverdue(totalOverdue)
                .disbursementDate(loan.getDisbursementDate())
                .loanId(loan.getId())
                .branchId(loan.getBranch() != null ? loan.getBranch().getId() : null)
                .loanProductId(loan.getLoanProduct() != null ? loan.getLoanProduct().getId() : null)
                .build();
    }

    private CalendarEventDto mapToCalendarEvent(RepaymentSchedule installment) {
        if (installment == null) return null;

        Loan loan = installment.getLoan();
        String status = installment.getPaidAmount() != null &&
                installment.getPaidAmount().compareTo(installment.getTotalDue()) >= 0 ?
                "paid" : (installment.getDueDate().isBefore(LocalDate.now()) ? "overdue" : "pending");

        return CalendarEventDto.builder()
                .id(installment.getId())
                .title("Installment #" + installment.getInstallmentNumber() + " - " +
                        (loan != null ? loan.getLoanAccountNumber() : "Unknown"))
                .startDate(installment.getDueDate().atStartOfDay())
                .endDate(installment.getDueDate().atStartOfDay().plusHours(23).plusMinutes(59))
                .allDay(true)
                .status(status)
                .amount(installment.getTotalDue())
                .paidAmount(installment.getPaidAmount())
                .loanId(loan != null ? loan.getId() : null)
                .borrowerName(loan != null && loan.getBorrower() != null ?
                        loan.getBorrower().getFullName() : null)
                .build();
    }

    // ==================== PDF GENERATION METHODS ====================

    private byte[] generateSchedulesPdfReport(List<RepaymentScheduleDto> schedules) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, baos);
        document.open();

        // Title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("REPAYMENT SCHEDULES REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" "));

        // Date
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
        document.add(new Paragraph("Generated On: " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")), normalFont));

        document.add(new Paragraph(" "));

        // Table
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);

        // Headers
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        addTableCell(table, "Loan #", headerFont);
        addTableCell(table, "Borrower", headerFont);
        addTableCell(table, "Product", headerFont);
        addTableCell(table, "Status", headerFont);
        addTableCell(table, "Next Due", headerFont);
        addTableCell(table, "Next Amount", headerFont);
        addTableCell(table, "Progress", headerFont);
        addTableCell(table, "Overdue", headerFont);

        // Data
        Font dataFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        for (RepaymentScheduleDto schedule : schedules) {
            addTableCell(table, schedule.getLoanNumber(), dataFont);
            addTableCell(table, schedule.getBorrowerName(), dataFont);
            addTableCell(table, schedule.getLoanProductName(), dataFont);
            addTableCell(table, schedule.getScheduleStatus(), dataFont);
            addTableCell(table, schedule.getNextPaymentDate() != null ?
                    schedule.getNextPaymentDate().toString() : "N/A", dataFont);
            addTableCell(table, formatCurrency(schedule.getNextPaymentAmount()), dataFont);
            addTableCell(table, schedule.getPaidInstallments() + "/" + schedule.getTotalInstallments(), dataFont);
            addTableCell(table, formatCurrency(schedule.getTotalOverdue()), dataFont);
        }

        document.add(table);
        document.close();

        return baos.toByteArray();
    }

    private byte[] generateSchedulePdfReport(RepaymentScheduleDto schedule) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);
        document.open();

        // Title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("REPAYMENT SCHEDULE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" "));

        // Loan Info
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        document.add(new Paragraph("Loan Information", headerFont));
        document.add(new Paragraph("Loan #: " + schedule.getLoanNumber(), normalFont));
        document.add(new Paragraph("Borrower: " + schedule.getBorrowerName(), normalFont));
        document.add(new Paragraph("Product: " + schedule.getLoanProductName(), normalFont));
        document.add(new Paragraph("Loan Amount: " + formatCurrency(schedule.getLoanAmount()), normalFont));
        document.add(new Paragraph("Status: " + schedule.getScheduleStatus(), normalFont));

        document.add(new Paragraph(" "));

        // Installments Table
        document.add(new Paragraph("Installment Schedule", headerFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);

        // Headers
        Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        addTableCell(table, "#", tableHeaderFont);
        addTableCell(table, "Due Date", tableHeaderFont);
        addTableCell(table, "Principal", tableHeaderFont);
        addTableCell(table, "Interest", tableHeaderFont);
        addTableCell(table, "Total", tableHeaderFont);
        addTableCell(table, "Paid", tableHeaderFont);

        // Data
        Font dataFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        for (InstallmentDto inst : schedule.getInstallments()) {
            addTableCell(table, String.valueOf(inst.getInstallmentNumber()), dataFont);
            addTableCell(table, inst.getDueDate().toString(), dataFont);
            addTableCell(table, formatCurrency(inst.getPrincipalAmount()), dataFont);
            addTableCell(table, formatCurrency(inst.getInterestAmount()), dataFont);
            addTableCell(table, formatCurrency(inst.getTotalAmount()), dataFont);
            addTableCell(table, formatCurrency(inst.getPaidAmount()), dataFont);
        }

        document.add(table);
        document.close();

        return baos.toByteArray();
    }

    private byte[] generateSchedulePrintPdf(RepaymentScheduleDto schedule) throws Exception {
        // Similar to generateSchedulePdfReport but with print-friendly formatting
        return generateSchedulePdfReport(schedule);
    }

    private byte[] generateScheduleStatementPdf(RepaymentScheduleDto schedule) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);
        document.open();

        // Title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("REPAYMENT STATEMENT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" "));

        // Header
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        document.add(new Paragraph("Loan #: " + schedule.getLoanNumber(), normalFont));
        document.add(new Paragraph("Borrower: " + schedule.getBorrowerName(), normalFont));
        document.add(new Paragraph("Statement Date: " + LocalDate.now().toString(), normalFont));

        document.add(new Paragraph(" "));

        // Summary
        document.add(new Paragraph("Summary", headerFont));
        document.add(new Paragraph("Total Loan Amount: " + formatCurrency(schedule.getLoanAmount()), normalFont));
        document.add(new Paragraph("Total Repayable: " + formatCurrency(schedule.getTotalRepayable()), normalFont));
        document.add(new Paragraph("Total Paid: " + formatCurrency(schedule.getTotalPaid()), normalFont));
        document.add(new Paragraph("Outstanding Balance: " + formatCurrency(schedule.getTotalDue()), normalFont));

        document.add(new Paragraph(" "));

        // Transaction History
        document.add(new Paragraph("Payment History", headerFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        // Headers
        Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        addTableCell(table, "Date", tableHeaderFont);
        addTableCell(table, "Installment #", tableHeaderFont);
        addTableCell(table, "Amount", tableHeaderFont);
        addTableCell(table, "Method", tableHeaderFont);

        // Data - This would need actual repayment data
        // For now, just a placeholder
        Font dataFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        addTableCell(table, "-", dataFont);
        addTableCell(table, "-", dataFont);
        addTableCell(table, "-", dataFont);
        addTableCell(table, "-", dataFont);

        document.add(table);
        document.close();

        return baos.toByteArray();
    }

    private byte[] generateSchedulesExcelReport(List<RepaymentScheduleDto> schedules) throws Exception {
        // Excel generation logic would go here using Apache POI
        // For now, return empty array
        return new byte[0];
    }

    private byte[] generateScheduleExcelReport(RepaymentScheduleDto schedule) throws Exception {
        // Excel generation logic would go here using Apache POI
        // For now, return empty array
        return new byte[0];
    }

    private byte[] generateDueReportsPdf(List<RepaymentSchedule> installments,
                                         LocalDate startDate, LocalDate endDate,
                                         Long branchId) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, baos);
        document.open();

        // Title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("DUE REPAYMENTS REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" "));

        // Period
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
        document.add(new Paragraph("Period: " + startDate + " to " + endDate, normalFont));

        document.add(new Paragraph(" "));

        // Table
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);

        // Headers
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        addTableCell(table, "Loan #", headerFont);
        addTableCell(table, "Borrower", headerFont);
        addTableCell(table, "Installment #", headerFont);
        addTableCell(table, "Due Date", headerFont);
        addTableCell(table, "Amount Due", headerFont);
        addTableCell(table, "Days Overdue", headerFont);
        addTableCell(table, "Status", headerFont);

        // Data
        Font dataFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        for (RepaymentSchedule inst : installments) {
            Loan loan = inst.getLoan();
            int daysOverdue = (int) ChronoUnit.DAYS.between(inst.getDueDate(), LocalDate.now());

            addTableCell(table, loan != null ? loan.getLoanAccountNumber() : "N/A", dataFont);
            addTableCell(table, loan != null && loan.getBorrower() != null ?
                    loan.getBorrower().getFullName() : "N/A", dataFont);
            addTableCell(table, String.valueOf(inst.getInstallmentNumber()), dataFont);
            addTableCell(table, inst.getDueDate().toString(), dataFont);
            addTableCell(table, formatCurrency(inst.getTotalDue()), dataFont);
            addTableCell(table, String.valueOf(daysOverdue > 0 ? daysOverdue : 0), dataFont);
            addTableCell(table, inst.getStatus() != null ? inst.getStatus().toString() : "PENDING", dataFont);
        }

        document.add(table);
        document.close();

        return baos.toByteArray();
    }

    private byte[] generateDueReportsExcel(List<RepaymentSchedule> installments,
                                           LocalDate startDate, LocalDate endDate,
                                           Long branchId) throws Exception {
        // Excel generation logic would go here using Apache POI
        // For now, return empty array
        return new byte[0];
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toString();
    }
}