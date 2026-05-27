// src/main/java/com/microfinance/loanapplications/dto/report/GroupPerformanceReport.java
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
public class GroupPerformanceReport {
    private Integer totalGroups;
    private Integer activeGroups;
    private Integer totalMembers;
    private Double averageGroupSize;
    private GroupsByStatus groupsByStatus;
    private List<GroupLoanPerformance> groupLoanPerformance;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupsByStatus {
        private Integer active;
        private Integer inactive;
        private Integer suspended;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupLoanPerformance {
        private String groupName;
        private Integer totalLoans;
        private Integer activeLoans;
        private BigDecimal outstandingAmount;
        private BigDecimal repaymentRate;
    }
}