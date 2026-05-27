// src/main/java/com/microfinance/loanapplications/dto/report/RiskAssessmentReport.java
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
public class RiskAssessmentReport {
    private RiskDistribution riskDistribution;
    private List<RiskFactor> riskFactors;
    private List<HighRiskClient> highRiskClients;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskDistribution {
        private Integer low;
        private Integer medium;
        private Integer high;
        private Integer critical;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskFactor {
        private String factor;
        private Integer affectedClients;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighRiskClient {
        private Long id;
        private String name;
        private String borrowerNumber;
        private String riskRating;
        private BigDecimal outstandingBalance;
        private Integer daysOverdue;
    }
}