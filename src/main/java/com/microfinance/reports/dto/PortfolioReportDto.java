// dto/report/PortfolioReportDto.java
package com.microfinance.reports.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PortfolioReportDto {
    private LocalDate reportDate;
    
    // Portfolio Overview
    private Integer totalActiveLoans;
    private Integer totalDisbursedLoans;
    private BigDecimal totalDisbursedAmount;
    private BigDecimal totalOutstandingAmount;
    private BigDecimal averageLoanSize;
    private BigDecimal recoveredAmount;
    // Portfolio Quality
    private BigDecimal portfolioAtRisk1Day;
    private BigDecimal portfolioAtRisk30Days;
    private BigDecimal portfolioAtRisk60Days;
    private BigDecimal portfolioAtRisk90Days;
    private BigDecimal portfolioAtRisk180Days;
    private BigDecimal writeOffAmount;
    private BigDecimal recoveryRate;
    
    // Aging Analysis
    private BigDecimal currentPortfolio;
    private BigDecimal overdue1To30Days;
    private BigDecimal overdue31To60Days;
    private BigDecimal overdue61To90Days;
    private BigDecimal overdue91To180Days;
    private BigDecimal overdue180PlusDays;
    
    // By Product Type
    private java.util.Map<String, ProductPortfolioDto> portfolioByProduct;
    private java.util.Map<String, ProductPortfolioDto> portfolioByBranch;
}