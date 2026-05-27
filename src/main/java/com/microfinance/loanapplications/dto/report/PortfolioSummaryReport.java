// src/main/java/com/microfinance/loanapplications/dto/report/PortfolioSummaryReport.java
package com.microfinance.loanapplications.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryReport {
    private BigDecimal totalPortfolioValue;
    private Integer activeLoans;
    private BigDecimal averageLoanSize;
    private BigDecimal outstandingBalance;
    private Integer overdueLoans;
    private BigDecimal overdueAmount;
    private ParDays parDays;
    private List<LoanProductDistribution> loanProductDistribution;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParDays {
        private BigDecimal par30;
        private BigDecimal par60;
        private BigDecimal par90;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanProductDistribution {
        private String productName;
        private Integer loanCount;
        private BigDecimal totalAmount;
    }
}