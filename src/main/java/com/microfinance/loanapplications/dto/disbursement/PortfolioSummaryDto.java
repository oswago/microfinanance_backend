package com.microfinance.loanapplications.dto.disbursement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor  // For setters/JPA
@AllArgsConstructor // For builder pattern
public class PortfolioSummaryDto {
    private LocalDate reportDate;
    private Long branchId;
    private String branchName;

    private LocalDate asOfDate;
    private BigDecimal totalOutstanding;
    private BigDecimal parPercentage;

    // Portfolio totals
    private Integer totalActiveLoans;
    private BigDecimal totalPortfolioValue;
    private BigDecimal totalOutstandingPrincipal;
    private BigDecimal totalAccruedInterest;

    // Status breakdown
    private Integer currentLoans;
    private Integer delinquentLoans;
    private Integer restructuredLoans;
    private Integer writtenOffLoans;

    // Performance metrics
    private BigDecimal portfolioAtRisk; // PAR > 30 days
    private BigDecimal collectionRate;
    private BigDecimal averageLoanSize;

    // Disbursement metrics
    private Integer loansDisbursedThisMonth;
    private BigDecimal amountDisbursedThisMonth;
    private Integer loansDisbursedThisQuarter;
    private BigDecimal amountDisbursedThisQuarter;

    // Delinquency breakdown
    private Integer loans1To30DaysLate;
    private Integer loans31To60DaysLate;
    private Integer loans61To90DaysLate;
    private Integer loansOver90DaysLate;

    // Risk metrics
    private BigDecimal provisionForLosses;
    private BigDecimal writeOffRatio;

    public BigDecimal getParRate() {
        if (totalActiveLoans == null || totalActiveLoans == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(delinquentLoans != null ? delinquentLoans : 0)
                .divide(BigDecimal.valueOf(totalActiveLoans), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}