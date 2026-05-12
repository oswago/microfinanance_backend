// service/PaymentAllocationService.java
package com.microfinance.loanapplications.service;

import com.microfinance.loanapplications.dto.repayment.InstallmentAllocationDto;
import com.microfinance.loanapplications.dto.repayment.InstallmentRemainingDto;
import com.microfinance.loanapplications.dto.repayment.RepaymentAllocationDto;
import com.microfinance.loanapplications.dto.repayment.SingleInstallmentAllocation;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.entity.RepaymentSchedule;
import com.microfinance.loanapplications.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralized service for payment allocation logic
 * This ensures consistency across all repayment methods
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAllocationService {

    private final RepaymentScheduleRepository repaymentScheduleRepository;

    /**
     * Calculate allocation for a single installment payment
     * 
     * @param installment The installment to allocate payment to
     * @param paymentAmount The amount to allocate
     * @return Allocation result with breakdown
     */
    public SingleInstallmentAllocation allocateToSingleInstallment(RepaymentSchedule installment, BigDecimal paymentAmount) {
        log.debug("Allocating {} to installment {}", paymentAmount, installment.getId());
        
        // Get remaining amounts
        InstallmentRemainingDto remaining = getRemainingAmounts(installment);
        
        if (paymentAmount.compareTo(remaining.getTotal()) > 0) {
            throw new IllegalArgumentException(
                String.format("Payment amount %.2f exceeds remaining balance %.2f", 
                    paymentAmount, remaining.getTotal()));
        }
        
        BigDecimal remainingAmount = paymentAmount;
        BigDecimal principalPaid = BigDecimal.ZERO;
        BigDecimal interestPaid = BigDecimal.ZERO;
        BigDecimal penaltyPaid = BigDecimal.ZERO;
        BigDecimal feesPaid = BigDecimal.ZERO;
        
        // 1. First allocate to fees
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0 && remaining.getFees().compareTo(BigDecimal.ZERO) > 0) {
            feesPaid = remainingAmount.min(remaining.getFees());
            remainingAmount = remainingAmount.subtract(feesPaid);
        }
        
        // 2. Then allocate to penalty
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0 && remaining.getPenalty().compareTo(BigDecimal.ZERO) > 0) {
            penaltyPaid = remainingAmount.min(remaining.getPenalty());
            remainingAmount = remainingAmount.subtract(penaltyPaid);
        }
        
        // 3. Then allocate to interest
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0 && remaining.getInterest().compareTo(BigDecimal.ZERO) > 0) {
            interestPaid = remainingAmount.min(remaining.getInterest());
            remainingAmount = remainingAmount.subtract(interestPaid);
        }
        
        // 4. Finally allocate to principal
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0 && remaining.getPrincipal().compareTo(BigDecimal.ZERO) > 0) {
            principalPaid = remainingAmount.min(remaining.getPrincipal());
            remainingAmount = remainingAmount.subtract(principalPaid);
        }
        
        // Calculate if the installment is now fully paid
        boolean isFullyPaid = remaining.getTotal().subtract(paymentAmount).compareTo(BigDecimal.ZERO) <= 0;
        
        return SingleInstallmentAllocation.builder()
                .installmentId(installment.getId())
                .installmentNumber(installment.getInstallmentNumber())
                .principalPaid(principalPaid)
                .interestPaid(interestPaid)
                .penaltyPaid(penaltyPaid)
                .feesPaid(feesPaid)
                .totalPaid(paymentAmount)
                .remainingAfterPayment(remainingAmount)
                .isFullyPaid(isFullyPaid)
                .build();
    }
    
    /**
     * Calculate allocation across multiple pending installments
     * 
     * @param loan The loan
     * @param paymentAmount The total payment amount
     * @param pendingInstallments List of pending installments (optional, will fetch if null)
     * @return Allocation result with breakdown per installment
     */
    public RepaymentAllocationDto allocateToMultipleInstallments(Loan loan, BigDecimal paymentAmount,
                                                                 List<RepaymentSchedule> pendingInstallments) {
        log.debug("Allocating {} across installments for loan {}", paymentAmount, loan.getId());
        
        if (pendingInstallments == null) {
            pendingInstallments = getPendingInstallmentsSorted(loan);
        }
        
        BigDecimal remainingAmount = paymentAmount;
        List<InstallmentAllocationDto> allocations = new ArrayList<>();
        List<RepaymentSchedule> allocatedInstallments = new ArrayList<>();
        
        BigDecimal totalPrincipalAllocated = BigDecimal.ZERO;
        BigDecimal totalInterestAllocated = BigDecimal.ZERO;
        BigDecimal totalPenaltyAllocated = BigDecimal.ZERO;
        BigDecimal totalFeesAllocated = BigDecimal.ZERO;
        
        for (RepaymentSchedule installment : pendingInstallments) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;
            
            // Get remaining for this installment
            InstallmentRemainingDto remaining = getRemainingAmounts(installment);
            
            if (remaining.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
                continue; // Skip fully paid installments
            }
            
            // Calculate payment for this installment
            BigDecimal amountForThisInstallment = remainingAmount.min(remaining.getTotal());
            
            // Allocate within the installment
            SingleInstallmentAllocation allocation = allocateToSingleInstallment(installment, amountForThisInstallment);
            
            // Update totals
            totalPrincipalAllocated = totalPrincipalAllocated.add(allocation.getPrincipalPaid());
            totalInterestAllocated = totalInterestAllocated.add(allocation.getInterestPaid());
            totalPenaltyAllocated = totalPenaltyAllocated.add(allocation.getPenaltyPaid());
            totalFeesAllocated = totalFeesAllocated.add(allocation.getFeesPaid());
            
            // Create DTO for this installment
            allocations.add(InstallmentAllocationDto.builder()
                    .installmentId(installment.getId())
                    .installmentNumber(installment.getInstallmentNumber())
                    .dueDate(installment.getDueDate())
                    .outstandingAmount(remaining.getTotal())
                    .feesPaid(allocation.getFeesPaid())
                    .penaltyPaid(allocation.getPenaltyPaid())
                    .interestPaid(allocation.getInterestPaid())
                    .principalPaid(allocation.getPrincipalPaid())
                    .totalPaid(allocation.getTotalPaid())
                    .isFullyPaid(allocation.isFullyPaid())
                    .build());
            
            allocatedInstallments.add(installment);
            remainingAmount = remainingAmount.subtract(amountForThisInstallment);
        }
        
        return RepaymentAllocationDto.builder()
                .totalAmount(paymentAmount)
                .allocatedAmount(paymentAmount.subtract(remainingAmount))
                .remainingAmount(remainingAmount)
                .principalAmount(totalPrincipalAllocated)
                .interestAmount(totalInterestAllocated)
                .penaltyAmount(totalPenaltyAllocated)
                .feesAmount(totalFeesAllocated)
                .allocations(allocations)
                .allocatedInstallments(allocatedInstallments)
                .build();
    }
    
    /**
     * Apply allocation to an installment (update the entity)
     * 
     * @param installment The installment to update
     * @param allocation The allocation to apply
     * @return Updated installment
     */
    public RepaymentSchedule applyAllocationToInstallment(RepaymentSchedule installment, 
                                                           SingleInstallmentAllocation allocation) {
        log.debug("Applying allocation to installment {}", installment.getId());
        
        // Update paid amounts
        installment.setPrincipalPaid(installment.getPrincipalPaid().add(allocation.getPrincipalPaid()));
        installment.setInterestPaid(installment.getInterestPaid().add(allocation.getInterestPaid()));
        installment.setPenaltyPaid(installment.getPenaltyPaid().add(allocation.getPenaltyPaid()));
        installment.setFeesPaid(installment.getFeesPaid().add(allocation.getFeesPaid()));
        installment.setTotalPaid(installment.getTotalPaid().add(allocation.getTotalPaid()));
        installment.setPaidAmount(installment.getTotalPaid());
        
        // Update penalty accrued
        if (allocation.getPenaltyPaid().compareTo(BigDecimal.ZERO) > 0) {
            installment.setPenaltyAccrued(installment.getPenaltyAccrued().subtract(allocation.getPenaltyPaid()));
        }
        
        // Update status
        if (installment.getTotalPaid().compareTo(installment.getTotalDue()) >= 0) {
            installment.setStatus(com.microfinance.common.config.GeneralConfig.InstallmentStatus.PAID);
        } else if (installment.getTotalPaid().compareTo(BigDecimal.ZERO) > 0) {
            installment.setStatus(com.microfinance.common.config.GeneralConfig.InstallmentStatus.PARTIAL);
        }
        
        // Update outstanding balances
        installment.setPrincipalOutstanding(installment.getPrincipalDue().subtract(installment.getPrincipalPaid()));
        installment.setInterestOutstanding(installment.getInterestDue().subtract(installment.getInterestPaid()));
        installment.setPenaltyOutstanding(installment.getPenaltyAmount().subtract(installment.getPenaltyPaid()));
        installment.updateOutstandingBalances();
        
        return installment;
    }
    
    /**
     * Get remaining amounts for an installment
     */
    public InstallmentRemainingDto getRemainingAmounts(RepaymentSchedule installment) {
        BigDecimal remainingPrincipal = installment.getPrincipalDue().subtract(installment.getPrincipalPaid());
        BigDecimal remainingInterest = installment.getInterestDue().subtract(installment.getInterestPaid());
        BigDecimal remainingPenalty = installment.getPenaltyAccrued();
        BigDecimal remainingFees = (installment.getFeesDue() != null ? installment.getFeesDue() : BigDecimal.ZERO)
                .subtract(installment.getFeesPaid() != null ? installment.getFeesPaid() : BigDecimal.ZERO);
        
        return InstallmentRemainingDto.builder()
                .principal(remainingPrincipal.max(BigDecimal.ZERO))
                .interest(remainingInterest.max(BigDecimal.ZERO))
                .penalty(remainingPenalty.max(BigDecimal.ZERO))
                .fees(remainingFees.max(BigDecimal.ZERO))
                .total(remainingPrincipal.add(remainingInterest).add(remainingPenalty).add(remainingFees))
                .build();
    }
    
    /**
     * Get pending installments sorted by due date
     */
    private List<RepaymentSchedule> getPendingInstallmentsSorted(Loan loan) {
        return repaymentScheduleRepository.findByLoanIdAndStatusOrderByDueDateAsc(
                loan.getId(), 
                com.microfinance.common.config.GeneralConfig.InstallmentStatus.PENDING);
    }
}