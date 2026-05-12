// dto/report/FinancialReportDto.java
package com.microfinance.reports.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class FinancialReportDto {
    private LocalDate reportDate;
    private String reportPeriod;
    
    // Income Statement
    private BigDecimal totalInterestIncome;
    private BigDecimal totalFeeIncome;
    private BigDecimal totalPenaltyIncome;
    private BigDecimal totalOtherIncome;
    private BigDecimal totalIncome;
    
    // Expenses
    private BigDecimal totalInterestExpense;
    private BigDecimal totalOperatingExpense;
    private BigDecimal totalProvisionExpense;
    private BigDecimal totalExpenses;
    
    // Profit/Loss
    private BigDecimal netProfit;
    private BigDecimal netProfitMargin;
    
    // Balance Sheet
    private BigDecimal totalLoanPortfolio;
    private BigDecimal totalCashAndBank;
    private BigDecimal totalReceivables;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    
    // Ratios
    private BigDecimal returnOnAssets;
    private BigDecimal returnOnEquity;
    private BigDecimal operatingEfficiency;
}