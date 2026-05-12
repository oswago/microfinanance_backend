// dto/InstallmentRemainingDto.java
package com.microfinance.loanapplications.dto.repayment;

import com.microfinance.loanapplications.entity.RepaymentSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for calculating remaining amounts on an installment
 * Used to determine payment allocation and track outstanding balances
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentRemainingDto {
    
    /**
     * Remaining principal amount to be paid
     */
    @Builder.Default
    private BigDecimal principal = BigDecimal.ZERO;
    
    /**
     * Remaining interest amount to be paid
     */
    @Builder.Default
    private BigDecimal interest = BigDecimal.ZERO;
    
    /**
     * Remaining penalty amount to be paid
     */
    @Builder.Default
    private BigDecimal penalty = BigDecimal.ZERO;
    
    /**
     * Remaining fees amount to be paid
     */
    @Builder.Default
    private BigDecimal fees = BigDecimal.ZERO;
    
    /**
     * Total remaining amount (sum of all components)
     */
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;
    
    /**
     * Convenience method to check if there are any remaining amounts
     */
    public boolean hasRemaining() {
        return total != null && total.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Convenience method to check if principal is fully paid
     */
    public boolean isPrincipalFullyPaid() {
        return principal == null || principal.compareTo(BigDecimal.ZERO) <= 0;
    }
    
    /**
     * Convenience method to check if interest is fully paid
     */
    public boolean isInterestFullyPaid() {
        return interest == null || interest.compareTo(BigDecimal.ZERO) <= 0;
    }
    
    /**
     * Convenience method to check if penalty is fully paid
     */
    public boolean isPenaltyFullyPaid() {
        return penalty == null || penalty.compareTo(BigDecimal.ZERO) <= 0;
    }
    
    /**
     * Convenience method to check if fees are fully paid
     */
    public boolean isFeesFullyPaid() {
        return fees == null || fees.compareTo(BigDecimal.ZERO) <= 0;
    }
    
    /**
     * Subtract a payment amount from the remaining amounts
     * Order of allocation: fees -> penalty -> interest -> principal
     * 
     * @param paymentAmount The amount to allocate
     * @return A new InstallmentRemainingDto with updated remaining amounts
     */
    public InstallmentRemainingDto allocatePayment(BigDecimal paymentAmount) {
        BigDecimal remaining = paymentAmount;
        BigDecimal newFees = this.fees;
        BigDecimal newPenalty = this.penalty;
        BigDecimal newInterest = this.interest;
        BigDecimal newPrincipal = this.principal;
        
        // First allocate to fees
        if (remaining.compareTo(BigDecimal.ZERO) > 0 && newFees.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal feesPayment = remaining.min(newFees);
            newFees = newFees.subtract(feesPayment);
            remaining = remaining.subtract(feesPayment);
        }
        
        // Then allocate to penalty
        if (remaining.compareTo(BigDecimal.ZERO) > 0 && newPenalty.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal penaltyPayment = remaining.min(newPenalty);
            newPenalty = newPenalty.subtract(penaltyPayment);
            remaining = remaining.subtract(penaltyPayment);
        }
        
        // Then allocate to interest
        if (remaining.compareTo(BigDecimal.ZERO) > 0 && newInterest.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal interestPayment = remaining.min(newInterest);
            newInterest = newInterest.subtract(interestPayment);
            remaining = remaining.subtract(interestPayment);
        }
        
        // Finally allocate to principal
        if (remaining.compareTo(BigDecimal.ZERO) > 0 && newPrincipal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal principalPayment = remaining.min(newPrincipal);
            newPrincipal = newPrincipal.subtract(principalPayment);
            remaining = remaining.subtract(principalPayment);
        }
        
        return InstallmentRemainingDto.builder()
                .principal(newPrincipal.max(BigDecimal.ZERO))
                .interest(newInterest.max(BigDecimal.ZERO))
                .penalty(newPenalty.max(BigDecimal.ZERO))
                .fees(newFees.max(BigDecimal.ZERO))
                .total(newPrincipal.add(newInterest).add(newPenalty).add(newFees))
                .build();
    }
    
    /**
     * Get the difference between this DTO and another (for comparison)
     * 
     * @param other The other DTO to compare with
     * @return A new DTO containing the differences
     */
    public InstallmentRemainingDto difference(InstallmentRemainingDto other) {
        return InstallmentRemainingDto.builder()
                .principal(this.principal.subtract(other.getPrincipal()))
                .interest(this.interest.subtract(other.getInterest()))
                .penalty(this.penalty.subtract(other.getPenalty()))
                .fees(this.fees.subtract(other.getFees()))
                .total(this.total.subtract(other.getTotal()))
                .build();
    }
    
    /**
     * Create a DTO from an installment (to be used in the repayment service)
     * 
     * @param installment The repayment schedule installment
     * @return InstallmentRemainingDto with calculated remaining amounts
     */
    public static InstallmentRemainingDto fromInstallment(RepaymentSchedule installment) {
        if (installment == null) {
            return InstallmentRemainingDto.builder().build();
        }
        
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
    
    @Override
    public String toString() {
        return String.format("InstallmentRemainingDto{principal=%s, interest=%s, penalty=%s, fees=%s, total=%s}",
                principal, interest, penalty, fees, total);
    }
}