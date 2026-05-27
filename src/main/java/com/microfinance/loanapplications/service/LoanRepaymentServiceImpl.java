package com.microfinance.loanapplications.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.microfinance.audit.service.AuditService;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.BusinessException;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.integrations.service.FinancialIntegrationService;
import com.microfinance.loanapplications.dto.disbursement.LoanRepaymentDto;
import com.microfinance.loanapplications.dto.repayment.*;
import com.microfinance.loanapplications.dto.repayment.DailyCollectionDto;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.entity.LoanRepayment;
import com.microfinance.loanapplications.entity.RepaymentSchedule;
import com.microfinance.loanapplications.repository.LoanRepaymentRepository;
import com.microfinance.loanapplications.repository.LoanRepository;
import com.microfinance.loanapplications.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanRepaymentServiceImpl implements LoanRepaymentService {

    private final LoanRepaymentRepository loanRepaymentRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final PdfGenerationService pdfGenerationService;

    @Autowired
    private final FinancialIntegrationService financialIntegrationService;
    @Autowired
    private final AuditService auditService;
    @Autowired
    private final SecurityUtils securityUtils;

    @Autowired
    private final PaymentAllocationService allocationService;

    @Autowired
    private final LoanRepaymentHelperService repaymentHelper;  // Inject the helper




    @Override
    @Transactional
    public RepaymentReceiptDto recordRepayment(RepaymentDto dto, User currentUser) {
        log.info("Processing repayment for loan ID: {}, amount: {}", dto.getLoanId(), dto.getAmountPaid());

        // Validate repayment using centralized helper
        repaymentHelper.validateRepaymentDto(dto);

        // Get loan
        Loan loan = loanRepository.findById(dto.getLoanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with ID: " + dto.getLoanId()));

        // Validate loan for repayment using centralized helper
        repaymentHelper.validateLoanForRepayment(loan);

        // Calculate allocation using centralized service
        RepaymentAllocationDto allocation = allocationService.allocateToMultipleInstallments(loan, dto.getAmountPaid(), null);

        // Apply the allocation to the installments (using centralized helper)
        repaymentHelper.applyAllocationToInstallments(allocation, dto);

        // Create repayment record using centralized helper
        LoanRepayment savedRepayment = repaymentHelper.createRepaymentRecordFromAllocation(dto, loan, currentUser, allocation);

        // Update loan totals using centralized helper
        repaymentHelper.updateLoanTotalsFromAllocation(loan, allocation, dto.getAmountPaid(), currentUser);

        log.info("Repayment recorded successfully. Receipt: {}, Principal: {}, Interest: {}, Penalty: {}, Fees: {}",
                savedRepayment.getReceiptNumber(),
                allocation.getPrincipalAmount(),
                allocation.getInterestAmount(),
                allocation.getPenaltyAmount(),
                allocation.getFeesAmount());

        // Fetch the receipt DTO
        RepaymentReceiptDto receiptDto = loanRepaymentRepository
                .findReceiptDtoById(savedRepayment.getId())
                .orElseThrow(() -> new RuntimeException("Failed to generate receipt"));

        // Add allocation details
        receiptDto.setPrincipalAmount(allocation.getPrincipalAmount());
        receiptDto.setInterestAmount(allocation.getInterestAmount());
        receiptDto.setPenaltyAmount(allocation.getPenaltyAmount());
        receiptDto.setFeesAmount(allocation.getFeesAmount());

        // Integrate Financials
        loan.updateFinancialTrackingFields();
        auditService.logRepaymentAction(savedRepayment.getId(), currentUser.getId(), savedRepayment.getAmountPaid());
        financialIntegrationService.recordLoanRepayment(loan, savedRepayment, currentUser);

        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedRepayment.getId())) {
            auditService.masterAuditLogs(
                    savedRepayment.getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.LOAN_REPAYMENT,
                    "LOAN_REPAYMENT",
                    "Loan Repayment of ID:"+savedRepayment.getId()+" Loan No:"+savedRepayment.getLoan().getLoanAccountNumber()+  " has been CREATED by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section


        return receiptDto;
    }


    /**
     * Apply allocation to all affected installments using the centralized service
     */
    private void applyAllocationToInstallments(RepaymentAllocationDto allocation) {
        for (InstallmentAllocationDto alloc : allocation.getAllocations()) {
            RepaymentSchedule installment = repaymentScheduleRepository.findById(alloc.getInstallmentId())
                    .orElseThrow(() -> new RuntimeException("Installment not found: " + alloc.getInstallmentId()));

            // Convert InstallmentAllocationDto to SingleInstallmentAllocation
            SingleInstallmentAllocation singleAllocation = SingleInstallmentAllocation.builder()
                    .installmentId(alloc.getInstallmentId())
                    .installmentNumber(alloc.getInstallmentNumber())
                    .principalPaid(alloc.getPrincipalPaid())
                    .interestPaid(alloc.getInterestPaid())
                    .penaltyPaid(alloc.getPenaltyPaid())
                    .feesPaid(alloc.getFeesPaid())
                    .totalPaid(alloc.getTotalPaid())
                    .isFullyPaid(alloc.getIsFullyPaid())
                    .build();

            // Apply the allocation using the centralized service
            allocationService.applyAllocationToInstallment(installment, singleAllocation);
            // Set payment metadata
            installment.setPaidDate(LocalDate.now());
            installment.setPaymentDate(LocalDate.now());

            repaymentScheduleRepository.save(installment);
        }
    }



    @Override
    @Transactional
    public BulkRepaymentResultDto recordBulkRepayments(List<RepaymentDto> repayments, User currentUser) {
        log.info("Processing bulk repayments for {} loans", repayments.size());

        BulkRepaymentResultDto result = new BulkRepaymentResultDto();
        List<RepaymentReceiptDto> successfulRepayments = new ArrayList<>();
        List<BulkRepaymentErrorDto> errors = new ArrayList<>();

        for (int i = 0; i < repayments.size(); i++) {
            RepaymentDto repaymentDto = repayments.get(i);
            try {
                RepaymentReceiptDto receipt = recordRepayment(repaymentDto, currentUser);
                successfulRepayments.add(receipt);
            } catch (Exception e) {
                errors.add(BulkRepaymentErrorDto.builder()
                        .index(i)
                        .loanId(repaymentDto.getLoanId())
                        .errorMessage(e.getMessage())
                        .build());
                log.error("Failed to process repayment for loan {}: {}", repaymentDto.getLoanId(), e.getMessage());
            }
        }

        result.setSuccessfulRepayments(successfulRepayments);
        result.setErrors(errors);
        result.setTotalProcessed(successfulRepayments.size());
        result.setTotalFailed(errors.size());

        log.info("Bulk repayment processing completed. Success: {}, Failed: {}",
                successfulRepayments.size(), errors.size());

        return result;
    }

    @Override
    @Transactional
    public RepaymentReceiptDto reverseRepayment(Long repaymentId, String reason, User currentUser) {
        log.info("Reversing repayment ID: {}", repaymentId);

        LoanRepayment repayment = loanRepaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Repayment not found with ID: " + repaymentId));

        Optional<Loan> loan=loanRepository.findById(repayment.getLoan().getId());

        if (!repayment.isValidForReversal()) {
            throw new IllegalStateException("Repayment cannot be reversed");
        }

        // Reverse the repayment
        repayment.reverseRepayment(reason, currentUser);

        // Reverse allocation in installments
        reverseInstallmentAllocation(repayment);

        // Update loan totals
        reverseLoanTotals(repayment.getLoan(), repayment);

        LoanRepayment reversedRepayment = loanRepaymentRepository.save(repayment);

        // Flush to ensure changes are persisted
        loanRepaymentRepository.flush();

        log.info("Repayment reversed successfully. Receipt: {}", reversedRepayment.getReceiptNumber());

        // Fetch the receipt DTO using the repository method
        RepaymentReceiptDto receiptDto = loanRepaymentRepository
                .findReceiptDtoById(reversedRepayment.getId())
                .orElseThrow(() -> new RuntimeException("Failed to generate receipt for reversed repayment ID: " + reversedRepayment.getId()));

        // Since this is a reversal, we might want to indicate that in the response
        receiptDto.setTotalAllocated(BigDecimal.ZERO);
        receiptDto.setRemainingAmount(BigDecimal.ZERO);
        receiptDto.setInstallmentsAffected(0);


        //Integrate Financials and Log as well
        if (reversedRepayment != null && reversedRepayment.getId() != null) {

            if(loan.isPresent()){
                loan.get().updateFinancialTrackingFields();
            }
            auditService.logReverseRepaymentAction(reversedRepayment.getId(),currentUser.getId(),reversedRepayment.getAmountPaid());
            // financialIntegrationService.recordLoanRepayment(loan, savedRepayment, currentUser);

            //Audit Section
            Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
            String createdByName ="";
            Long createdById=null;
            if(currentUser1.isPresent()){
                createdByName=currentUser1.get().getFullName();
                createdById=currentUser1.get().getId();
            }
                auditService.masterAuditLogs(
                        reversedRepayment.getBorrower().getId(),
                        GeneralConfig.BorrowerActivityType.LOAN_REPAYMENT_REVERSED,
                        "LOAN_REPAYMENT",
                        "Loan Repayment of ID:"+reversedRepayment.getId()+" Loan No:"+reversedRepayment.getLoan().getLoanAccountNumber()+  " has been REVERSED by:"+createdByName+"-"+createdById
                );
            //End Audit Section


        }

        return receiptDto;
    }

    @Override
    @Transactional
    public RepaymentReceiptDto waiveRepayment(Long repaymentId, WaiveRepaymentDto dto, User currentUser) {
        log.info("Waiving repayment ID: {}", repaymentId);

        // Create a waived repayment record
        LoanRepayment repayment = loanRepaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Repayment not found with ID: " + repaymentId));

        Optional<Loan> loan=loanRepository.findById(repayment.getLoan().getId());

        // Create a new repayment record for the waived amount
        LoanRepayment waivedRepayment = new LoanRepayment();
        waivedRepayment.setLoan(repayment.getLoan());
        waivedRepayment.setPaymentDate(LocalDate.now());
        waivedRepayment.setAmountPaid(dto.getWaivedAmount());
        waivedRepayment.setPrincipalAmount(BigDecimal.ZERO);
        waivedRepayment.setInterestAmount(BigDecimal.ZERO);
        waivedRepayment.setPenaltyAmount(dto.getWaivedAmount());
        waivedRepayment.setPaymentMethod(GeneralConfig.PaymentMethod.WAIVER);
        waivedRepayment.setTransactionReference("WAIVER-" + System.currentTimeMillis());
        waivedRepayment.setNotes(dto.getReason());
        waivedRepayment.setReceivedBy(currentUser);
        waivedRepayment.generateReceiptNumber();
        waivedRepayment.setStatus(GeneralConfig.RepaymentStatus.valueOf(GeneralConfig.RepaymentStatus.WAIVED.name()));

        // Update the installment
        RepaymentSchedule installment = repaymentScheduleRepository.findById(dto.getInstallmentId())
                .orElseThrow(() -> new IllegalArgumentException("Installment not found"));

        // Calculate the new penalty amount
        BigDecimal newPenaltyAccrued = installment.getPenaltyAccrued().subtract(dto.getWaivedAmount());
        installment.setPenaltyAccrued(newPenaltyAccrued.max(BigDecimal.ZERO));

        // Update installment status if needed
        if (installment.getPenaltyAccrued().compareTo(BigDecimal.ZERO) <= 0) {
            // Penalty fully waived, you might want to update status
            log.info("Penalty fully waived for installment: {}", installment.getId());
        }

        repaymentScheduleRepository.save(installment);

        LoanRepayment savedWaiver = loanRepaymentRepository.save(waivedRepayment);

        // Flush to ensure changes are persisted
        loanRepaymentRepository.flush();
        log.info("Repayment waived successfully. Receipt: {}", savedWaiver.getReceiptNumber());


        if (waivedRepayment.getId() != null) {
            if(loan.isPresent()){
                loan.get().updateFinancialTrackingFields();
            }
            auditService.logWaivedRepaymentAction(waivedRepayment.getId(),currentUser.getId(),waivedRepayment.getAmountPaid());
          //  financialIntegrationService.recordLoanRepayment(loan, savedRepayment, currentUser);
            //Audit Section
            Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
            String createdByName ="";
            Long createdById=null;
            if(currentUser1.isPresent()){
                createdByName=currentUser1.get().getFullName();
                createdById=currentUser1.get().getId();
            }
            auditService.masterAuditLogs(
                    waivedRepayment.getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.LOAN_REPAYMENT_WAIVED,
                    "LOAN_REPAYMENT",
                    "Loan Repayment of ID:"+waivedRepayment.getId()+" Loan No:"+waivedRepayment.getLoan().getLoanAccountNumber()+  " has been WAIVED by:"+createdByName+"-"+createdById
            );
            //End Audit Section

        }

        // Fetch the receipt DTO using the repository method
        RepaymentReceiptDto receiptDto = loanRepaymentRepository
                .findReceiptDtoById(savedWaiver.getId())
                .orElseThrow(() -> new RuntimeException("Failed to generate receipt for waived repayment ID: " + savedWaiver.getId()));

        // For waivers, the allocation is just the waived amount
        receiptDto.setTotalAllocated(dto.getWaivedAmount());
        receiptDto.setRemainingAmount(BigDecimal.ZERO);
        receiptDto.setInstallmentsAffected(1);

        return receiptDto;
    }

    @Override
    public List<RepaymentScheduleDto> getRepaymentSchedule(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with ID: " + loanId));

        return loan.getRepaymentSchedules().stream()
                .sorted(Comparator.comparing(RepaymentSchedule::getDueDate))
                .map(this::mapToScheduleDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<LoanRepaymentDto> getRepaymentHistory(Long loanId, Pageable pageable) {
        Page<LoanRepayment> repayments = loanRepaymentRepository.findByLoanIdAndIsReversedFalse(loanId, pageable);
        return repayments.map(this::mapToRepaymentDto);
    }

    @Override
    public Page<LoanRepaymentDto> getRepaymentHistoryByBorrower(Long borrowerId, Pageable pageable) {
        Page<LoanRepayment> repayments = loanRepaymentRepository.findByLoanBorrowerIdAndIsReversedFalse(borrowerId, pageable);
        return repayments.map(this::mapToRepaymentDto);
    }

    @Override
    public EarlyRepaymentQuoteDto calculateEarlyRepaymentAmount(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found with ID: " + loanId));

        // Validate loan eligibility for early repayment
        validateEarlyRepaymentEligibility(loan);

        // Calculate all required amounts
        BigDecimal originalPrincipal = loan.getPrincipalAmount();
        BigDecimal currentOutstandingPrincipal = calculateOutstandingPrincipal(loan);
        BigDecimal totalInterestPaidToDate = calculateTotalInterestPaidToDate(loan);
        BigDecimal totalPenaltyPaidToDate = calculateTotalPenaltyPaidToDate(loan);
        BigDecimal remainingInterest = calculateRemainingInterest(loan);
        BigDecimal interestRebate = calculateInterestRebate(loan, remainingInterest);
        BigDecimal earlyRepaymentFee = calculateEarlyRepaymentFee(loan);
        BigDecimal processingFee = calculateProcessingFee(loan);
        BigDecimal otherCharges = calculateOtherCharges(loan);

        // Calculate final amounts
        BigDecimal principalAmountDue = currentOutstandingPrincipal;
        BigDecimal interestAmountDue = remainingInterest.subtract(interestRebate).max(BigDecimal.ZERO);
        BigDecimal feeAmountDue = earlyRepaymentFee.add(processingFee).add(otherCharges);
        BigDecimal totalEarlyRepaymentAmount = principalAmountDue.add(interestAmountDue).add(feeAmountDue);

        // Calculate savings
        BigDecimal totalSavings = calculateTotalSavings(loan, totalEarlyRepaymentAmount);
        BigDecimal interestSavings = calculateInterestSavings(loan);
        BigDecimal percentageSavings = calculatePercentageSavings(loan, totalEarlyRepaymentAmount);

        return EarlyRepaymentQuoteDto.builder()
                .loanId(loanId)
                .loanAccountNumber(loan.getLoanAccountNumber())
                .quoteDate(LocalDate.now())
                .quoteExpiryDate(LocalDate.now().plusDays(7))
                .originalPrincipal(originalPrincipal)
                .currentOutstandingPrincipal(currentOutstandingPrincipal)
                .totalInterestPaidToDate(totalInterestPaidToDate)
                .totalPenaltyPaidToDate(totalPenaltyPaidToDate)
                .remainingInterest(remainingInterest)
                .interestRebate(interestRebate)
                .earlyRepaymentFee(earlyRepaymentFee)
                .processingFee(processingFee)
                .otherCharges(otherCharges)
                .totalEarlyRepaymentAmount(totalEarlyRepaymentAmount)
                .totalSavings(totalSavings)
                .percentageSavings(percentageSavings)
                .interestSavings(interestSavings)
                .principalAmountDue(principalAmountDue)
                .interestAmountDue(interestAmountDue)
                .feeAmountDue(feeAmountDue)
                .isEligibleForEarlyRepayment(true)
                .eligibilityCriteria(getEligibilityCriteria())
                .termsAndConditions(getEarlyRepaymentTerms())
                .calculationMethod("REDUCING_BALANCE_WITH_REBATE")
                .quoteReference(generateQuoteReference(loan))
                .isQuoteValid(true)
                .validationMessage("Early repayment quote is valid and ready for processing")
                .build();
    }

    @Override
    public Page<OverdueInstallmentDto> getOverdueInstallments(LocalDate date, Long branchId, Pageable pageable) {
        return repaymentScheduleRepository.findOverdueInstallments(date, branchId, pageable)
                .map(this::mapToOverdueInstallmentDto);
    }

    @Override
    public DailyCollectionDto getDailyCollectionReport(LocalDate date, Long branchId, Long officerId) {
        log.info("Generating daily collection report for date: {}", date);

        // Get total collection
        BigDecimal totalCollection;
        if (branchId != null) {
            totalCollection = loanRepaymentRepository.getDailyCollectionByBranch(date, branchId);
        } else if (officerId != null) {
            totalCollection = loanRepaymentRepository.getDailyCollectionByOfficer(date, officerId);
        } else {
            totalCollection = loanRepaymentRepository.getDailyCollectionTotal(date);
        }

        // Get collection by payment method
        List<Object[]> paymentMethodData = loanRepaymentRepository.getDailyCollectionByPaymentMethod(date);
        List<PaymentMethodBreakdownDto> paymentMethodBreakdown = paymentMethodData.stream()
                .map(row -> PaymentMethodBreakdownDto.builder()
                        .paymentMethod(row[0].toString())
                        .amount((BigDecimal) row[1])
                        .build())
                .collect(Collectors.toList());

        // Get recent repayments
        Pageable recentPageable = PageRequest.of(0, 10);
        Page<LoanRepayment> recentRepayments = loanRepaymentRepository.findByPaymentDateAndIsReversedFalse(date, recentPageable);

        return DailyCollectionDto.builder()
                .reportDate(date)
                .branchId(branchId)
                .officerId(officerId)
                .totalCollection(totalCollection != null ? totalCollection : BigDecimal.ZERO)
                .numberOfTransactions(recentRepayments.getTotalElements())
                .paymentMethodBreakdown(paymentMethodBreakdown)
                .recentRepayments(recentRepayments.map(this::mapToRepaymentDto).getContent())
                .build();
    }

    @Override
    public CollectionPerformanceDto getCollectionPerformance(Long officerId, LocalDate startDate, LocalDate endDate) {
        log.info("Calculating collection performance for officer: {}, period: {} to {}", officerId, startDate, endDate);

        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        Long uniqueLoansCollected = loanRepaymentRepository.countUniqueLoansCollectedByOfficer(officerId, startDate, endDate);
        BigDecimal totalCollected = loanRepaymentRepository.getTotalCollectionByOfficer(officerId, startDate, endDate);
        Long totalRepayments = loanRepaymentRepository.countRepaymentsByOfficer(officerId, startDate, endDate);

        // Calculate target achievement (mock implementation - adjust based on your business rules)
        BigDecimal targetAmount = BigDecimal.valueOf(100000); // Example target
        Double targetAchievement = totalCollected != null ?
                totalCollected.divide(targetAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue() : 0.0;

        return CollectionPerformanceDto.builder()
                .officerId(officerId)
                .startDate(startDate)
                .endDate(endDate)
                .uniqueLoansCollected(uniqueLoansCollected != null ? uniqueLoansCollected : 0L)
                .totalCollected(totalCollected != null ? totalCollected : BigDecimal.ZERO)
                .totalRepayments(BigDecimal.valueOf(totalRepayments != null ? totalRepayments : 0L))
                .averageRepayment(totalRepayments != null && totalRepayments > 0 ?
                        totalCollected.divide(BigDecimal.valueOf(totalRepayments), 2, RoundingMode.HALF_UP) :
                        BigDecimal.ZERO)
                .targetAmount(targetAmount)
                .targetAchievement(BigDecimal.valueOf(targetAchievement))
                .build();
    }


    @Override
    public RepaymentAllocationDto calculateRepaymentAllocation(Loan loan, BigDecimal paymentAmount) {
        // Simply delegate to the centralized service
        return allocationService.allocateToMultipleInstallments(loan, paymentAmount, null);
    }


    @Override
    public void validateRepayment(RepaymentDto dto) {
        if (dto.getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        if (dto.getPaymentDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Payment date cannot be in the future");
        }

        if (dto.getPaymentMethod() == null || dto.getPaymentMethod().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
    }


    private void validateLoanForRepayment(Loan loan) {
        if (loan.getStatus() != GeneralConfig.LoanStatus.ACTIVE) {
            throw new IllegalStateException("Repayments can only be made for active loans");
        }

        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Loan has no outstanding balance");
        }
    }

    @Override
    public RepaymentStatisticsDto getRepaymentStatistics() {
        LocalDate today = LocalDate.now();

        // Get due today count
        Long dueToday = repaymentScheduleRepository.countDueToday(today);

        // Get collected today
        BigDecimal collectedToday = loanRepaymentRepository.getDailyCollectionTotal(today);

        // Get overdue count
        Long overdue = repaymentScheduleRepository.countOverdueAsOfDate(today);

        // Calculate on-time rate
        Long totalPaid = loanRepaymentRepository.countRepaymentsBetweenDates(
                today.withDayOfMonth(1), today);
        Long onTimePaid = loanRepaymentRepository.countOnTimeRepaymentsBetweenDates(
                today.withDayOfMonth(1), today);
        Double onTimeRate = totalPaid > 0 ?
                (onTimePaid.doubleValue() / totalPaid.doubleValue()) * 100 : 0.0;

        // Get total collected this month
        BigDecimal totalCollected = loanRepaymentRepository.getTotalCollectionBetweenDates(
                today.withDayOfMonth(1), today);

        return RepaymentStatisticsDto.builder()
                .dueToday(dueToday != null ? dueToday : 0L)
                .collectedToday(collectedToday != null ? collectedToday : BigDecimal.ZERO)
                .overdue(overdue != null ? overdue : 0L)
                .onTimeRate(onTimeRate)
                .totalCollected(totalCollected != null ? totalCollected : BigDecimal.ZERO)
                .totalRepayments(totalPaid != null ? totalPaid : 0L)
                .averageRepayment(totalPaid != null && totalPaid > 0 ?
                        totalCollected.divide(BigDecimal.valueOf(totalPaid), 2, RoundingMode.HALF_UP) :
                        BigDecimal.ZERO)
                .build();
    }


    // ==================== PRIVATE HELPER METHODS ====================

    private List<RepaymentSchedule> getPendingInstallmentsSorted(Loan loan) {
        return loan.getRepaymentSchedules().stream()
                .filter(schedule -> !schedule.isFullyPaid())
                .sorted(Comparator.comparing(RepaymentSchedule::getDueDate))
                .collect(Collectors.toList());
    }


    private LoanRepayment createRepaymentRecordORG(RepaymentDto dto, Loan loan, User currentUser,
                                                RepaymentAllocationDto allocation) {
        LoanRepayment repayment = new LoanRepayment();
        repayment.setLoan(loan);
        repayment.setPaymentDate(dto.getPaymentDate());
        repayment.setAmountPaid(dto.getAmountPaid());
        repayment.setAmount(dto.getAmountPaid());
        repayment.setPrincipalAmount(allocation.getPrincipalAmount());
        repayment.setInterestAmount(allocation.getInterestAmount());
        repayment.setPenaltyAmount(allocation.getPenaltyAmount());
        repayment.setPaymentMethod(GeneralConfig.PaymentMethod.valueOf(dto.getPaymentMethod()));
        repayment.setTransactionReference(dto.getTransactionReference());
        repayment.setNotes(dto.getNotes());
        repayment.setReceivedBy(currentUser);
        repayment.setStatus(GeneralConfig.RepaymentStatus.valueOf(GeneralConfig.RepaymentStatus.COMPLETED.name()));
        repayment.setIsReversed(false);
        repayment.generateReceiptNumber();

        return repayment;
    }

    /**
     * Create repayment record from allocation
     */
    private LoanRepayment createRepaymentRecord(RepaymentDto dto, Loan loan, User currentUser,
                                                RepaymentAllocationDto allocation) {
        LoanRepayment repayment = new LoanRepayment();
        repayment.setLoan(loan);
        repayment.setBorrower(loan.getBorrower());
        repayment.setPaymentDate(dto.getPaymentDate());
        repayment.setAmountPaid(dto.getAmountPaid());
        repayment.setAmount(dto.getAmountPaid());
        repayment.setPrincipalAmount(allocation.getPrincipalAmount());
        repayment.setInterestAmount(allocation.getInterestAmount());
        repayment.setPenaltyAmount(allocation.getPenaltyAmount());
        repayment.setFeesAmount(allocation.getFeesAmount());
        repayment.setPaymentMethod(GeneralConfig.PaymentMethod.valueOf(dto.getPaymentMethod()));
        repayment.setTransactionReference(dto.getTransactionReference());
        repayment.setNotes(dto.getNotes());
        repayment.setReceivedBy(currentUser);
        repayment.setStatus(GeneralConfig.RepaymentStatus.COMPLETED);
        repayment.setIsReversed(false);
        repayment.generateReceiptNumber();
        repayment.setCreatedBy(currentUser.getId());
        // Set allocated installments
        repayment.setAllocatedInstallments(new ArrayList<>(allocation.getAllocatedInstallments()));

        return repayment;
    }


    /**
     * Update loan totals based on allocation
     */
    private void updateLoanTotals(Loan loan, RepaymentAllocationDto allocation,
                                  BigDecimal paymentAmount, User currentUser) {
        // Update loan tracking fields
        BigDecimal currentPrincipalPaid = loan.getPrincipalPaid() != null ? loan.getPrincipalPaid() : BigDecimal.ZERO;
        BigDecimal currentInterestPaid = loan.getInterestPaid() != null ? loan.getInterestPaid() : BigDecimal.ZERO;
        BigDecimal currentPenaltyPaid = loan.getPenaltyPaid() != null ? loan.getPenaltyPaid() : BigDecimal.ZERO;
        BigDecimal currentFeesPaid = loan.getFeesPaid() != null ? loan.getFeesPaid() : BigDecimal.ZERO;

        loan.setPrincipalPaid(currentPrincipalPaid.add(allocation.getPrincipalAmount()));
        loan.setInterestPaid(currentInterestPaid.add(allocation.getInterestAmount()));
        loan.setPenaltyPaid(currentPenaltyPaid.add(allocation.getPenaltyAmount()));
        loan.setFeesPaid(currentFeesPaid.add(allocation.getFeesAmount()));

        // Update principal outstanding
        loan.setPrincipalOutstanding(loan.getPrincipalAmount().subtract(loan.getPrincipalPaid()));

        // Update total paid and outstanding balance
        loan.setTotalPaid(loan.getTotalPaid() != null ? loan.getTotalPaid().add(paymentAmount) : paymentAmount);

        // Calculate outstanding balance (principal + interest - paid)
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


   /* private void applyPaymentToInstallmentsORG(List<RepaymentSchedule> installments, LoanRepayment repayment) {
        for (RepaymentSchedule installment : installments) {
            BigDecimal amountForInstallment = calculateAmountForInstallment(installment, repayment);
            installment.applyPayment(amountForInstallment);
            installment.setPaidAmount(installment.getTotalPaid());
            repaymentScheduleRepository.save(installment);
        }
    }*/

    private void applyPaymentToInstallments(RepaymentAllocationDto allocation, LoanRepayment repayment) {
        for (InstallmentAllocationDto alloc : allocation.getAllocations()) {
            RepaymentSchedule installment = repaymentScheduleRepository.findById(alloc.getInstallmentId())
                    .orElseThrow(() -> new RuntimeException("Installment not found: " + alloc.getInstallmentId()));

            // Apply the exact allocated amounts to this installment
            installment.applyPayment(
                    alloc.getTotalPaid(),
                    alloc.getFeesPaid() != null ? alloc.getFeesPaid() : BigDecimal.ZERO,
                    alloc.getPenaltyPaid()
            );

            // Set the payment reference
            installment.setPaymentDate(repayment.getPaymentDate());
            installment.setTransactionReference(repayment.getTransactionReference());
            installment.setPaymentMethod(repayment.getPaymentMethod().name());

            repaymentScheduleRepository.save(installment);
        }
    }

    private void updateLoanTotalsORG(Loan loan, LoanRepayment repayment) {
        loan.setTotalPaid(loan.getTotalPaid().add(repayment.getAmountPaid()));
        loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(repayment.getAmountPaid()));

        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(GeneralConfig.LoanStatus.CLOSED);
            loan.setClosedDate(LocalDate.now());
        }

        loanRepository.save(loan);
    }




    private void reverseInstallmentAllocation(LoanRepayment repayment) {
        // Implementation would need to track which installments were paid by this repayment
        // For simplicity, we're not implementing the full reversal logic here
        log.warn("Full reversal of installment allocation not implemented");
    }


    private void reverseLoanTotals(Loan loan, LoanRepayment repayment) {
        loan.setTotalPaid(loan.getTotalPaid().subtract(repayment.getAmountPaid()));
        loan.setOutstandingBalance(loan.getOutstandingBalance().add(repayment.getAmountPaid()));

        if (loan.getStatus() == GeneralConfig.LoanStatus.CLOSED) {
            loan.setStatus(GeneralConfig.LoanStatus.ACTIVE);
            loan.setClosedDate(null);
        }

        loanRepository.save(loan);
    }

    private BigDecimal calculateAmountForInstallmentORG(RepaymentSchedule installment, LoanRepayment repayment) {
        // Simplified calculation - in reality, you'd need to track allocation per installment
        return repayment.getAmountPaid();
    }

    private BigDecimal calculateAmountForInstallment(RepaymentSchedule installment, RepaymentAllocationDto allocation) {
        // Find the allocation for this specific installment
        return allocation.getAllocations().stream()
                .filter(a -> a.getInstallmentId().equals(installment.getId()))
                .map(InstallmentAllocationDto::getTotalPaid)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    // ==================== MAPPING METHODS ====================

    @Transactional
    private RepaymentScheduleDto mapToScheduleDto(RepaymentSchedule schedule) {
        if (schedule == null) return null;

        return RepaymentScheduleDto.builder()
                .id(schedule.getId())
                .installmentNumber(schedule.getInstallmentNumber())
                .dueDate(schedule.getDueDate())
                .principalDue(schedule.getPrincipalDue())
                .interestDue(schedule.getInterestDue())
                .totalDue(schedule.getTotalDue())
                .principalPaid(schedule.getPrincipalPaid())
                .interestPaid(schedule.getInterestPaid())
                .totalPaid(schedule.getTotalPaid())
                .outstandingAmount(schedule.getOutstandingAmount())
                .status(schedule.getStatus() != null ? schedule.getStatus().name() : null)
                .daysOverdue(schedule.getDaysOverdue())
                .isOverdue(schedule.isOverdue())
                .isFullyPaid(schedule.isFullyPaid())
                .build();
    }

    @Transactional
    private LoanRepaymentDto mapToRepaymentDto(LoanRepayment repayment) {
        if (repayment == null) return null;

        return LoanRepaymentDto.builder()
                .id(repayment.getId())
                .receiptNumber(repayment.getReceiptNumber())
                .loanId(repayment.getLoan() != null ? repayment.getLoan().getId() : null)
                .loanAccountNumber(repayment.getLoan() != null ? repayment.getLoan().getLoanAccountNumber() : null)
                .borrowerName(repayment.getLoan() != null && repayment.getLoan().getBorrower() != null ?
                        repayment.getLoan().getBorrower().getFullName() : null)
                .amountPaid(repayment.getAmountPaid())
                .principalAmount(repayment.getPrincipalAmount())
                .interestAmount(repayment.getInterestAmount())
                .penaltyAmount(repayment.getPenaltyAmount())
                .paymentDate(repayment.getPaymentDate())
                .paymentMethod(repayment.getPaymentMethod() != null ? repayment.getPaymentMethod().name() : null)
                .transactionReference(repayment.getTransactionReference())
                .receivedByName(repayment.getReceivedBy() != null ?
                        repayment.getReceivedBy().getFirstName() + " " + repayment.getReceivedBy().getLastName() : null)
                .status(String.valueOf(repayment.getStatus()))
                .notes(repayment.getNotes())
                .build();
    }
    @Transactional
    private OverdueInstallmentDto mapToOverdueInstallmentDto(RepaymentSchedule schedule) {
        if (schedule == null) return null;

        Loan loan = schedule.getLoan();

        return OverdueInstallmentDto.builder()
                .installmentId(schedule.getId())
                .loanId(loan != null ? loan.getId() : null)
                .loanAccountNumber(loan != null ? loan.getLoanAccountNumber() : null)
                .borrowerName(loan != null && loan.getBorrower() != null ?
                        loan.getBorrower().getFullName() : null)
                .borrowerPhone(loan != null && loan.getBorrower() != null ?
                        loan.getBorrower().getPhoneNumber() : null)
                .installmentNumber(schedule.getInstallmentNumber())
                .dueDate(schedule.getDueDate())
                .principalDue(schedule.getPrincipalDue())
                .interestDue(schedule.getInterestDue())
                .totalDue(schedule.getTotalDue())
                .outstandingAmount(schedule.getOutstandingAmount())
                .daysOverdue(schedule.getDaysOverdue())
                .penaltyAccrued(schedule.getPenaltyAccrued())
                .loanOfficerName(loan != null && loan.getLoanOfficer() != null ?
                        loan.getLoanOfficer().getFirstName() + " " + loan.getLoanOfficer().getLastName() : null)
                .branchId(loan != null && loan.getBranch() != null ? loan.getBranch().getId() : null)
                .branchName(loan != null && loan.getBranch() != null ? loan.getBranch().getName() : null)
                .build();
    }


    @Override
    @Transactional(readOnly = true)  // Add this to the public method
    public List<LoanRepaymentDto> getRecentRepayments(int limit) {
        log.info("Fetching {} recent repayments", limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by("paymentDate").descending());
        Page<LoanRepayment> recentRepayments = loanRepaymentRepository.findRecentRepayments(pageable);

        return recentRepayments.stream()
                .map(this::mapToRepaymentDto)
                .collect(Collectors.toList());
    }


    // ==================== EARLY REPAYMENT HELPER METHODS ====================

    private void validateEarlyRepaymentEligibility(Loan loan) {
        if (loan.getStatus() != GeneralConfig.LoanStatus.ACTIVE) {
            throw new IllegalStateException("Early repayment is only available for active loans");
        }

        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Loan has no outstanding balance");
        }

        // Check if loan product allows early repayment
        if (loan.getLoanProduct() != null && !loan.getLoanProduct().isEarlyRepaymentAllowed()) {
            throw new IllegalStateException("This loan product does not allow early repayment");
        }
    }

    private BigDecimal calculateOutstandingPrincipal(Loan loan) {
        return loan.getRepaymentSchedules().stream()
                .map(schedule -> schedule.getPrincipalDue().subtract(schedule.getPrincipalPaid()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .max(BigDecimal.ZERO);
    }

    private BigDecimal calculateTotalInterestPaidToDate(Loan loan) {
        return loan.getRepayments().stream()
                .filter(repayment -> !repayment.getIsReversed())
                .map(LoanRepayment::getInterestAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalPenaltyPaidToDate(Loan loan) {
        return loan.getRepayments().stream()
                .filter(repayment -> !repayment.getIsReversed())
                .map(LoanRepayment::getPenaltyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateRemainingInterest(Loan loan) {
        return loan.getRepaymentSchedules().stream()
                .filter(schedule -> !schedule.isFullyPaid())
                .map(schedule -> schedule.getInterestDue().subtract(schedule.getInterestPaid()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .max(BigDecimal.ZERO);
    }

    private BigDecimal calculateInterestRebate(Loan loan, BigDecimal remainingInterest) {
        long remainingMonths = calculateRemainingMonths(loan);
        double rebatePercentage = calculateRebatePercentage(remainingMonths);

        return remainingInterest.multiply(BigDecimal.valueOf(rebatePercentage / 100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateEarlyRepaymentFee(Loan loan) {
        BigDecimal feePercentage = BigDecimal.valueOf(0.02); // 2%
        return calculateOutstandingPrincipal(loan)
                .multiply(feePercentage)
                .setScale(2, RoundingMode.HALF_UP)
                .max(BigDecimal.valueOf(100)); // Minimum fee
    }

    private BigDecimal calculateProcessingFee(Loan loan) {
        return BigDecimal.valueOf(500); // Fixed processing fee
    }

    private BigDecimal calculateOtherCharges(Loan loan) {
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateTotalSavings(Loan loan, BigDecimal earlyRepaymentAmount) {
        BigDecimal totalRemainingPayments = calculateTotalRemainingPayments(loan);
        return totalRemainingPayments.subtract(earlyRepaymentAmount).max(BigDecimal.ZERO);
    }

    private BigDecimal calculateInterestSavings(Loan loan) {
        BigDecimal remainingInterest = calculateRemainingInterest(loan);
        BigDecimal interestRebate = calculateInterestRebate(loan, remainingInterest);
        return interestRebate;
    }

    private BigDecimal calculatePercentageSavings(Loan loan, BigDecimal earlyRepaymentAmount) {
        BigDecimal totalRemainingPayments = calculateTotalRemainingPayments(loan);
        if (totalRemainingPayments.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalRemainingPayments.subtract(earlyRepaymentAmount)
                .divide(totalRemainingPayments, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalRemainingPayments(Loan loan) {
        return loan.getRepaymentSchedules().stream()
                .filter(schedule -> !schedule.isFullyPaid())
                .map(schedule -> schedule.getPrincipalDue()
                        .add(schedule.getInterestDue())
                        .add(schedule.getPenaltyAccrued())
                        .subtract(schedule.getPrincipalPaid())
                        .subtract(schedule.getInterestPaid()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long calculateRemainingMonths(Loan loan) {
        LocalDate today = LocalDate.now();
        return loan.getRepaymentSchedules().stream()
                .filter(schedule -> !schedule.isFullyPaid() && schedule.getDueDate().isAfter(today))
                .count();
    }

    private double calculateRebatePercentage(long remainingMonths) {
        if (remainingMonths >= 12) return 50.0;
        if (remainingMonths >= 6) return 30.0;
        if (remainingMonths >= 3) return 15.0;
        return 5.0;
    }

    private String getEligibilityCriteria() {
        return "Loan must be active, have outstanding balance, and early repayment must be allowed by loan product terms.";
    }

    private String getEarlyRepaymentTerms() {
        return "Early repayment fee of 2% applies. Interest rebate based on remaining term. Quote valid for 7 days.";
    }

    private String generateQuoteReference(Loan loan) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return "EQ-" + loan.getLoanAccountNumber() + "-" + timestamp.substring(timestamp.length() - 6);
    }

//Repayment History Section
        @Transactional(readOnly = true)
        @Override
        public byte[] exportRepaymentHistory(Page<LoanRepaymentDto> repayments, Long loanId, String format) {
            log.debug("Exporting repayment history for loan: {} in format: {}", loanId, format);

            // Get loan details for header
            Loan loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

            if (format.equalsIgnoreCase("PDF")) {
                return generatePdfReport(repayments, loan);
            } else if (format.equalsIgnoreCase("EXCEL") || format.equalsIgnoreCase("XLSX")) {
                return generateExcelReport(repayments, loan);
            } else {
                throw new BusinessException("Unsupported format: " + format);
            }
        }

        private byte[] generatePdfReport(Page<LoanRepaymentDto> repayments, Loan loan) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Document document = new Document(PageSize.A4);
                PdfWriter.getInstance(document, baos);
                document.open();
                // Reuse PDF generation service methods for consistent styling
                addReportHeader(document, loan);
                addLoanInfoSection(document, loan);
                addSummaryStatistics(document, repayments);
                addPaymentHistoryTable(document, repayments);
                addReportFooter(document);
                document.close();
                return baos.toByteArray();
            } catch (DocumentException e) {
                log.error("Error generating PDF report", e);
                throw new BusinessException("Failed to generate PDF report: " + e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Add report header using PDF generation service styling
         */
        private void addReportHeader(Document document, Loan loan) throws DocumentException {
            // Use the letterhead method from PdfGenerationService
            // Since it's private, we'll recreate similar styling
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);

            // Left side - Company Name
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            Paragraph companyName = new Paragraph("MICROFINANCE SYSTEM", pdfGenerationService.getTitleFont());
            companyName.setAlignment(Element.ALIGN_LEFT);
            leftCell.addElement(companyName);
            leftCell.addElement(new Paragraph("Repayment History Report", pdfGenerationService.getHeaderFont()));
            headerTable.addCell(leftCell);

            // Right side - Report Info
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(new Paragraph("Generated: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")),
                    pdfGenerationService.getSmallFont()));
            rightCell.addElement(new Paragraph("Loan Account: " + loan.getLoanAccountNumber(),
                    pdfGenerationService.getNormalFont()));
            headerTable.addCell(rightCell);

            document.add(headerTable);

            // Add horizontal line
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBorder(Rectangle.BOTTOM);
            lineCell.setBorderColor(BaseColor.GRAY);
            lineCell.setBorderWidth(1);
            line.addCell(lineCell);
            document.add(line);

            document.add(new Paragraph(" "));
        }

        /**
         * Add loan information section using PDF generation service table styling
         */
        private void addLoanInfoSection(Document document, Loan loan) throws DocumentException {
            Paragraph loanHeader = new Paragraph("LOAN INFORMATION", pdfGenerationService.getSubheaderFont());
            loanHeader.setSpacingBefore(10);
            loanHeader.setSpacingAfter(5);
            document.add(loanHeader);

            PdfPTable loanTable = new PdfPTable(2);
            loanTable.setWidthPercentage(100);
            loanTable.setWidths(new float[]{30f, 70f});

            // Use the table row styling from PdfGenerationService
            pdfGenerationService.addTableRow(loanTable, "Loan Account:", loan.getLoanAccountNumber());
            pdfGenerationService.addTableRow(loanTable, "Borrower:", loan.getBorrower() != null ?
                    loan.getBorrower().getFullName() : "N/A");
            pdfGenerationService.addTableRow(loanTable, "Principal Amount:",
                    pdfGenerationService.formatCurrency(loan.getPrincipalAmount()));
            pdfGenerationService.addTableRow(loanTable, "Outstanding Balance:",
                    pdfGenerationService.formatCurrency(loan.getOutstandingBalance()));
            pdfGenerationService.addTableRow(loanTable, "Interest Rate:",
                    loan.getInterestRate() + "%");
            pdfGenerationService.addTableRow(loanTable, "Disbursement Date:",
                    pdfGenerationService.formatDate(loan.getDisbursementDate()));
            pdfGenerationService.addTableRow(loanTable, "Maturity Date:",
                    pdfGenerationService.formatDate(loan.getMaturityDate()));

            document.add(loanTable);
            document.add(new Paragraph(" "));
        }

        /**
         * Add summary statistics section
         */
        private void addSummaryStatistics(Document document, Page<LoanRepaymentDto> repayments) throws DocumentException {
            // Calculate totals
            BigDecimal totalPaid = repayments.getContent().stream()
                    .map(LoanRepaymentDto::getAmountPaid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalPrincipal = repayments.getContent().stream()
                    .map(LoanRepaymentDto::getPrincipalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalInterest = repayments.getContent().stream()
                    .map(LoanRepaymentDto::getInterestAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Paragraph summaryHeader = new Paragraph("SUMMARY STATISTICS", pdfGenerationService.getSubheaderFont());
            summaryHeader.setSpacingBefore(10);
            summaryHeader.setSpacingAfter(5);
            document.add(summaryHeader);

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);

            pdfGenerationService.addTableRow(summaryTable, "Total Payments:", String.valueOf(repayments.getTotalElements()));
            pdfGenerationService.addTableRow(summaryTable, "Total Amount Paid:", pdfGenerationService.formatCurrency(totalPaid));
            pdfGenerationService.addTableRow(summaryTable, "Total Principal Paid:", pdfGenerationService.formatCurrency(totalPrincipal));
            pdfGenerationService.addTableRow(summaryTable, "Total Interest Paid:", pdfGenerationService.formatCurrency(totalInterest));
            pdfGenerationService.addTableRow(summaryTable, "Average Payment:",
                    repayments.getContent().isEmpty() ? "KES 0.00" :
                            pdfGenerationService.formatCurrency(totalPaid.divide(BigDecimal.valueOf(repayments.getContent().size()), 2, BigDecimal.ROUND_HALF_UP)));

            document.add(summaryTable);
            document.add(new Paragraph(" "));
        }

        /**
         * Add payment history table
         */
        private void addPaymentHistoryTable(Document document, Page<LoanRepaymentDto> repayments) throws DocumentException {
            Paragraph historyHeader = new Paragraph("PAYMENT HISTORY", pdfGenerationService.getSubheaderFont());
            historyHeader.setSpacingBefore(10);
            historyHeader.setSpacingAfter(5);
            document.add(historyHeader);

            PdfPTable paymentTable = new PdfPTable(7);
            paymentTable.setWidthPercentage(100);
            paymentTable.setWidths(new float[]{1f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f});

            // Table header
            addTableHeader(paymentTable, "Date");
            addTableHeader(paymentTable, "Receipt #");
            addTableHeader(paymentTable, "Amount");
            addTableHeader(paymentTable, "Principal");
            addTableHeader(paymentTable, "Interest");
            addTableHeader(paymentTable, "Method");
            addTableHeader(paymentTable, "Reference");

            // Table body
            for (LoanRepaymentDto payment : repayments.getContent()) {
                addTableCell(paymentTable, payment.getPaymentDate() != null ?
                        payment.getPaymentDate().toString() : "N/A");
                addTableCell(paymentTable, payment.getReceiptNumber() != null ?
                        payment.getReceiptNumber() : "N/A");
                addTableCell(paymentTable, pdfGenerationService.formatCurrency(payment.getAmountPaid()));
                addTableCell(paymentTable, pdfGenerationService.formatCurrency(payment.getPrincipalAmount()));
                addTableCell(paymentTable, pdfGenerationService.formatCurrency(payment.getInterestAmount()));
                addTableCell(paymentTable, payment.getPaymentMethod() != null ?
                        payment.getPaymentMethod() : "N/A");
                addTableCell(paymentTable, payment.getTransactionReference() != null ?
                        payment.getTransactionReference() : "N/A");
            }

            document.add(paymentTable);
            document.add(new Paragraph(" "));
        }

        /**
         * Add report footer using PDF generation service styling
         */
        private void addReportFooter(Document document) throws DocumentException {
            document.add(new Paragraph(" "));

            PdfPTable footerTable = new PdfPTable(1);
            footerTable.setWidthPercentage(100);

            PdfPCell footerCell = new PdfPCell();
            footerCell.setBorder(Rectangle.NO_BORDER);
            footerCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph note = new Paragraph(
                    "This report is generated automatically from the system. For inquiries, contact support.",
                    pdfGenerationService.getSmallFont());
            note.setAlignment(Element.ALIGN_CENTER);
            footerCell.addElement(note);

            footerTable.addCell(footerCell);
            document.add(footerTable);
        }

        private void addTableHeader(PdfPTable table, String header) {
            PdfPCell cell = new PdfPCell(new Phrase(header, pdfGenerationService.getBoldFont()));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        private void addTableCell(PdfPTable table, String value) {
            PdfPCell cell = new PdfPCell(new Phrase(value, pdfGenerationService.getNormalFont()));
            cell.setPadding(5);
            table.addCell(cell);
        }

        private byte[] generateExcelReport(Page<LoanRepaymentDto> repayments, Loan loan) {
            // Excel generation implementation using Apache POI
            // ... (same as previous implementation)
            return new byte[0];
        }


    }


