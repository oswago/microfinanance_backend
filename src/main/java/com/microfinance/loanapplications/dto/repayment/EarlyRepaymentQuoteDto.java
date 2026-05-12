package com.microfinance.loanapplications.dto.repayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarlyRepaymentQuoteDto {
    private Long loanId;
    private String loanAccountNumber;
    private LocalDate quoteDate;
    private LocalDate quoteExpiryDate;
    
    // Current loan status
    private BigDecimal originalPrincipal;
    private BigDecimal currentOutstandingPrincipal;
    private BigDecimal totalInterestPaidToDate;
    private BigDecimal totalPenaltyPaidToDate;
    
    // Early repayment calculation
    private BigDecimal remainingInterest;
    private BigDecimal interestRebate;
    private BigDecimal earlyRepaymentFee;
    private BigDecimal processingFee;
    private BigDecimal otherCharges;
    
    // Final amounts
    private BigDecimal totalEarlyRepaymentAmount;
    private BigDecimal totalSavings;
    private BigDecimal percentageSavings;
    private BigDecimal interestSavings;

    
    // Breakdown
    private BigDecimal principalAmountDue;
    private BigDecimal interestAmountDue;
    private BigDecimal feeAmountDue;
    private BigDecimal  totalAmountDue;
    
    // Terms and conditions
    private Boolean isEligibleForEarlyRepayment;
    private String eligibilityCriteria;
    private String termsAndConditions;
    private String calculationMethod;
    
    // Validation
    private String quoteReference;
    private Boolean isQuoteValid;
    private String validationMessage;
    
    public BigDecimal getTotalSavings() {
        BigDecimal totalRemainingPayments = calculateTotalRemainingPayments();
        return totalRemainingPayments.subtract(totalEarlyRepaymentAmount).max(BigDecimal.ZERO);
    }
    
    public BigDecimal getPercentageSavings() {
        BigDecimal totalRemainingPayments = calculateTotalRemainingPayments();
        if (totalRemainingPayments.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getTotalSavings()
                .divide(totalRemainingPayments, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    private BigDecimal calculateTotalRemainingPayments() {
        // This would typically be calculated based on remaining installments
        // For now, return a simplified calculation
        return currentOutstandingPrincipal.add(remainingInterest).add(earlyRepaymentFee);
    }
}