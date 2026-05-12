package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalPerformanceDto {

    // Basic information
    private Long approverId;
    private String approverName;
    private String approverRole;
    private String approverBranch;

    // Period information
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String periodType; // WEEKLY, MONTHLY, QUARTERLY, YEARLY

    // Performance metrics
    private Long totalDecisions;
    private Long approvedCount;
    private Long rejectedCount;
    private Long returnedCount;
    private Long pendingCount;

    // Rates and averages
    private Double approvalRate;
    private Double rejectionRate;
    private Double returnRate;
    private Double avgProcessingTimeHours;
    private Double onTimeApprovalRate;
    private Double slaComplianceRate;

    // Quality metrics
    private Integer slaBreaches;
    private Double satisfactionScore;
    private Double customerSatisfactionScore;
    private Integer escalations;
    private Integer conditionOverrides;

    // Risk metrics
    private Double averageRiskScore;
    private Integer highRiskApprovals;
    private Integer lowRiskRejections;

    // Amount metrics
    private Double totalApprovedAmount;
    private Double averageApprovedAmount;
    private Double largestApprovedAmount;
    private Double smallestApprovedAmount;

    // Time metrics
    private Integer fastestDecisionHours;
    private Integer slowestDecisionHours;
    private Double medianDecisionHours;

    // Breakdowns
    private List<MonthlyPerformance> monthlyPerformance;
    private List<ProductPerformance> productPerformance;
    private List<BranchPerformance> branchPerformance;

    // Monthly performance inner class
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyPerformance {
        private String month; // Format: "YYYY-MM"
        private String monthName; // "January", "February", etc.
        private Long totalDecisions;
        private Long approvedCount;
        private Long rejectedCount;
        private Long returnedCount;
        private Double avgProcessingTime;
        private Double approvalRate;
        private Double rejectionRate;
        private Double returnRate;
        private Integer slaCompliant;
        private Integer slaBreached;
        private Double slaComplianceRate;
        private Double totalApprovedAmount;

        // Trend indicators
        private Double trendApprovalRate; // vs previous period
        private Double trendProcessingTime; // vs previous period
        private String performanceStatus; // IMPROVING, DECLINING, STABLE
    }

    // Product performance inner class (optional)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPerformance {
        private String productType;
        private String productName;
        private Long totalDecisions;
        private Long approvedCount;
        private Long rejectedCount;
        private Double approvalRate;
        private Double avgProcessingTime;
        private Double totalApprovedAmount;
    }

    // Branch performance inner class (optional)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchPerformance {
        private Long branchId;
        private String branchName;
        private Long totalDecisions;
        private Long approvedCount;
        private Double approvalRate;
        private Double avgProcessingTime;
    }

    // Helper methods
    public void calculateDerivedMetrics() {
        // Calculate rates
        if (totalDecisions != null && totalDecisions > 0) {
            approvalRate = (double) approvedCount / totalDecisions * 100;
            rejectionRate = (double) rejectedCount / totalDecisions * 100;
            returnRate = (double) returnedCount / totalDecisions * 100;
        }

        // Calculate SLA compliance rate
        if (totalDecisions != null && slaBreaches != null) {
            slaComplianceRate = totalDecisions > 0 ?
                    (double) (totalDecisions - slaBreaches) / totalDecisions * 100 : 0.0;
        }

        // Calculate on-time approval rate
        if (totalDecisions != null && slaBreaches != null) {
            onTimeApprovalRate = totalDecisions > 0 ?
                    (double) (totalDecisions - slaBreaches) / totalDecisions * 100 : 0.0;
        }
    }
}