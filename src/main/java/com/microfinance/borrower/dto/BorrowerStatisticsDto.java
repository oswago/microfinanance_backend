// BorrowerStatisticsDto.java
package com.microfinance.borrower.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerStatisticsDto {
    // Loan counts
    private Long totalLoans;
    private Long activeLoanCount;
    private Long completedLoans;
    private Long delinquentLoans;
    private Long pendingLoans;
    
    // Financial statistics
    private BigDecimal totalPrincipalBorrowed;
    private BigDecimal totalPrincipalPaid;
    private BigDecimal totalOutstandingBalance;
    private BigDecimal totalInterestPaid;
    private BigDecimal averageLoanAmount;
    
    // Performance metrics
    private Integer daysSinceFirstLoan;
    private LocalDate firstLoanDate;
    private LocalDate lastLoanDate;
    private Double onTimePaymentRate;
    private Integer totalLatePayments;
    private Integer averageDaysLate;
    
    // Current status
    private Boolean hasActiveLoans;
    private Boolean isDelinquent;
    private BigDecimal currentMonthlyObligation;
    private BigDecimal debtToIncomeRatio;
    
    // Credit information
    private Integer creditScore;
    private String creditRating;
}