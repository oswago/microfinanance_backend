package com.microfinance.borrower.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BorrowerPortfolioSummaryDto {
    private Long borrowerId;
    private String borrowerName;
    private Integer activeLoans;
    private Integer completedLoans;
    private BigDecimal totalBorrowed;
    private BigDecimal totalRepaid;
    private BigDecimal outstandingBalance;
    private BigDecimal totalSavings;
    private BigDecimal totalInterestPaid;
    private LocalDateTime lastLoanDate;
    private String repaymentBehavior; // GOOD, AVERAGE, POOR
}