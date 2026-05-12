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
public class CollectionPerformanceDto {
    private Long officerId;
    private String officerName;
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Collection metrics
    private Integer totalLoansAssigned;
    private Integer activeLoans;
    private Integer overdueLoans;
    
    // Amount metrics
    private BigDecimal totalPortfolioAmount;
    private BigDecimal totalDueAmount;
    private BigDecimal totalCollectedAmount;
    private BigDecimal totalOverdueAmount;
    private BigDecimal totalOutstandingAmount;
    
    // Performance ratios
    private BigDecimal collectionRate;
    private BigDecimal overdueRate;
    private BigDecimal recoveryRate;
    
    // Detailed breakdown
    private Integer totalRepaymentsProcessed;
    private Integer successfulRepayments;
    private Integer failedRepayments;
    private BigDecimal averageCollectionAmount;

    private Long uniqueLoansCollected;

    private BigDecimal totalCollected;
    private BigDecimal totalRepayments;
    private BigDecimal averageRepayment;
    private BigDecimal targetAmount;
    private BigDecimal targetAchievement;
    
    // Period comparison
    private BigDecimal collectionGrowthRate;
    private BigDecimal overdueReductionRate;
    
    public BigDecimal getCollectionRate() {
        if (totalDueAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalCollectedAmount.divide(totalDueAmount, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getOverdueRate() {
        if (totalPortfolioAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalOverdueAmount.divide(totalPortfolioAmount, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getRecoveryRate() {
        if (totalOverdueAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalCollectedAmount.divide(totalOverdueAmount, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}