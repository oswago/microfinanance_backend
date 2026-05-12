// service/LoanRepaymentHelperService.java
package com.microfinance.loanapplications.service;

import com.microfinance.base.entity.User;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanapplications.dto.repayment.*;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.entity.LoanRepayment;
import com.microfinance.loanapplications.entity.RepaymentSchedule;
import com.microfinance.loanapplications.repository.LoanRepaymentRepository;
import com.microfinance.loanapplications.repository.LoanRepository;
import com.microfinance.loanapplications.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralized service for creating repayment records and updating loan totals
 * Used by both RepaymentScheduleService and LoanRepaymentService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanRepaymentHelperService {

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;

    @Autowired
    private final PaymentAllocationService allocationService;

    /**
     * Create a repayment record from a multi-installment allocation (used by LoanRepaymentService)
     *
     * @param dto The repayment DTO
     * @param loan The loan being repaid
     * @param currentUser The user recording the repayment
     * @param allocation The allocation details
     * @return Created LoanRepayment entity
     */
    public LoanRepayment createRepaymentRecordFromAllocation(RepaymentDto dto, Loan loan,
                                                             User currentUser,
                                                             RepaymentAllocationDto allocation) {
        LoanRepayment repayment = new LoanRepayment();
        setCommonRepaymentFields(repayment, loan, currentUser);
        
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
        // Set allocated installments
        repayment.setAllocatedInstallments(new ArrayList<>(allocation.getAllocatedInstallments()));
        
        return loanRepaymentRepository.save(repayment);
    }

    /**
     * Create a repayment record from a single installment allocation (used by RepaymentScheduleService)
     *
     * @param installment The installment being paid
     * @param allocation The allocation for this installment
     * @param paymentDto The payment DTO
     * @param currentUser The user recording the payment
     * @return Created LoanRepayment entity
     */
    public LoanRepayment createRepaymentRecordFromInstallment(RepaymentSchedule installment,
                                                               SingleInstallmentAllocation allocation,
                                                               InstallmentPaymentDto paymentDto,
                                                               User currentUser) {
        LoanRepayment repayment = new LoanRepayment();
        setCommonRepaymentFields(repayment, installment.getLoan(), currentUser);
        
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
        repayment.setInstallment(installment);
        
        // Set allocated installments
        List<RepaymentSchedule> allocatedInstallments = new ArrayList<>();
        allocatedInstallments.add(installment);
        repayment.setAllocatedInstallments(allocatedInstallments);
        
        return loanRepaymentRepository.save(repayment);
    }

    /**
     * Set common fields for any repayment record
     */
    private void setCommonRepaymentFields(LoanRepayment repayment, Loan loan, User currentUser) {
        repayment.setLoan(loan);
        repayment.setBorrower(loan.getBorrower());
        repayment.setReceivedBy(currentUser);
        repayment.setStatus(GeneralConfig.RepaymentStatus.COMPLETED);
        repayment.setIsReversed(false);
        repayment.setCreatedBy(currentUser.getId());
        repayment.generateReceiptNumber();
    }

    /**
     * Update loan totals after a multi-installment repayment
     *
     * @param loan The loan to update
     * @param allocation The allocation containing payment breakdown
     * @param paymentAmount The total payment amount
     * @param currentUser The user making the repayment
     * @return Updated loan
     */
    public Loan updateLoanTotalsFromAllocation(Loan loan, RepaymentAllocationDto allocation,
                                                BigDecimal paymentAmount, User currentUser) {
        return updateLoanTotals(loan, 
                allocation.getPrincipalAmount(),
                allocation.getInterestAmount(),
                allocation.getPenaltyAmount(),
                allocation.getFeesAmount(),
                paymentAmount,
                currentUser);
    }

    /**
     * Update loan totals after a single installment payment
     *
     * @param loan The loan to update
     * @param allocation The allocation for this installment
     * @param paymentAmount The payment amount
     * @param currentUser The user making the payment
     * @return Updated loan
     */
    public Loan updateLoanTotalsFromInstallment(Loan loan, SingleInstallmentAllocation allocation,
                                                 BigDecimal paymentAmount, User currentUser) {
        return updateLoanTotals(loan,
                allocation.getPrincipalPaid(),
                allocation.getInterestPaid(),
                allocation.getPenaltyPaid(),
                allocation.getFeesPaid(),
                paymentAmount,
                currentUser);
    }

    /**
     * Core method to update loan totals based on payment amounts
     * This is the single source of truth for loan total calculations
     *
     * @param loan The loan to update
     * @param principalPaid Amount of principal paid
     * @param interestPaid Amount of interest paid
     * @param penaltyPaid Amount of penalty paid
     * @param feesPaid Amount of fees paid
     * @param totalPayment Total payment amount
     * @param currentUser The user making the payment
     * @return Updated loan
     */
    private Loan updateLoanTotals(Loan loan, BigDecimal principalPaid, BigDecimal interestPaid,
                                   BigDecimal penaltyPaid, BigDecimal feesPaid, 
                                   BigDecimal totalPayment, User currentUser) {
        log.debug("Updating loan totals for loan {}: Principal={}, Interest={}, Penalty={}, Fees={}, Total={}",
                loan.getId(), principalPaid, interestPaid, penaltyPaid, feesPaid, totalPayment);

        // Get current totals (handle nulls)
        BigDecimal currentPrincipalPaid = getSafeBigDecimal(loan.getPrincipalPaid());
        BigDecimal currentInterestPaid = getSafeBigDecimal(loan.getInterestPaid());
        BigDecimal currentPenaltyPaid = getSafeBigDecimal(loan.getPenaltyPaid());
        BigDecimal currentFeesPaid = getSafeBigDecimal(loan.getFeesPaid());
        BigDecimal currentTotalPaid = getSafeBigDecimal(loan.getTotalPaid());

        // Update tracking fields
        loan.setPrincipalPaid(currentPrincipalPaid.add(principalPaid));
        loan.setInterestPaid(currentInterestPaid.add(interestPaid));
        loan.setPenaltyPaid(currentPenaltyPaid.add(penaltyPaid));
        loan.setFeesPaid(currentFeesPaid.add(feesPaid));

        // Update principal outstanding
        BigDecimal principalOutstanding = loan.getPrincipalAmount().subtract(loan.getPrincipalPaid());
        loan.setPrincipalOutstanding(principalOutstanding.max(BigDecimal.ZERO));

        // Update total paid
        loan.setTotalPaid(currentTotalPaid.add(totalPayment));

        // Calculate outstanding balance
        BigDecimal totalPrincipalAndInterest = loan.getPrincipalAmount()
                .add(getSafeBigDecimal(loan.getTotalInterestDue()));
        BigDecimal outstandingBalance = totalPrincipalAndInterest.subtract(loan.getTotalPaid());
        loan.setOutstandingBalance(outstandingBalance.max(BigDecimal.ZERO));

        // Update loan status if fully paid
        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(GeneralConfig.LoanStatus.CLOSED);
            loan.setClosedDate(LocalDate.now());
            loan.setClosedBy(currentUser);
            log.info("Loan {} has been fully paid and closed", loan.getLoanAccountNumber());
        }

        return loanRepository.save(loan);
    }

    /**
     * Helper method to safely get BigDecimal value (null becomes zero)
     */
    private BigDecimal getSafeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * Validate that a repayment can be made on a loan
     *
     * @param loan The loan to validate
     * @throws IllegalStateException if loan is not in a valid state for repayment
     */
    public void validateLoanForRepayment(Loan loan) {
        if (loan.getStatus() != GeneralConfig.LoanStatus.ACTIVE) {
            throw new IllegalStateException(
                String.format("Repayments can only be made for active loans. Current status: %s", 
                    loan.getStatus()));
        }

        BigDecimal outstandingBalance = getSafeBigDecimal(loan.getOutstandingBalance());
        if (outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Loan has no outstanding balance");
        }
    }

    /**
     * Validate a repayment DTO
     *
     * @param dto The repayment DTO to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateRepaymentDto(RepaymentDto dto) {
        if (dto.getAmountPaid() == null || dto.getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (dto.getPaymentDate() == null) {
            throw new IllegalArgumentException("Payment date is required");
        }
        if (dto.getPaymentMethod() == null || dto.getPaymentMethod().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (dto.getLoanId() == null) {
            throw new IllegalArgumentException("Loan ID is required");
        }
    }

    /**
     * Validate an installment payment DTO
     *
     * @param dto The installment payment DTO to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateInstallmentPaymentDto(InstallmentPaymentDto dto) {
        if (dto.getAmountPaid() == null || dto.getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (dto.getPaymentDate() == null) {
            throw new IllegalArgumentException("Payment date is required");
        }
        if (dto.getPaymentMethod() == null || dto.getPaymentMethod().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (dto.getInstallmentId() == null) {
            throw new IllegalArgumentException("Installment ID is required");
        }
    }



    /**
     * Apply allocation to all affected installments
     * This method updates the installment entities with the payment allocation
     *
     * @param allocation The allocation containing payment details for each installment
     */
    public void applyAllocationToInstallments(RepaymentAllocationDto allocation) {
        log.debug("Applying allocation to {} installments", allocation.getAllocations().size());

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

            // Apply the allocation using the centralized allocation service
            allocationService.applyAllocationToInstallment(installment, singleAllocation);

            // Set payment metadata (if needed)
            if (alloc.getTotalPaid().compareTo(BigDecimal.ZERO) > 0) {
                installment.setPaidDate(LocalDate.now());
                installment.setPaymentDate(LocalDate.now());
            }

            repaymentScheduleRepository.save(installment);
            log.debug("Applied payment of {} to installment {}", alloc.getTotalPaid(), installment.getId());
        }
    }

    /**
     * Alternative: Apply allocation and also set payment metadata from DTO
     */
    public void applyAllocationToInstallments(RepaymentAllocationDto allocation, RepaymentDto dto) {
        log.debug("Applying allocation to {} installments with metadata from DTO", allocation.getAllocations().size());

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

            // Apply the allocation using the centralized allocation service
            allocationService.applyAllocationToInstallment(installment, singleAllocation);

            // Set payment metadata from DTO
            installment.setPaidDate(dto.getPaymentDate());
            installment.setPaymentDate(dto.getPaymentDate());
            if (dto.getPaymentMethod() != null) {
                installment.setPaymentMethod(dto.getPaymentMethod());
            }
            if (dto.getTransactionReference() != null) {
                installment.setTransactionReference(dto.getTransactionReference());
            }
            if (dto.getNotes() != null) {
                installment.setNotes(dto.getNotes());
            }

            repaymentScheduleRepository.save(installment);
        }
    }

}