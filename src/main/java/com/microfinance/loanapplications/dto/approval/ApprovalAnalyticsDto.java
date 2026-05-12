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
public class ApprovalAnalyticsDto {
    private LocalDate reportDate;
    private String periodType; // DAY, WEEK, MONTH, QUARTER, YEAR, CUSTOM
    private Long totalApplications;
    private Long totalApproved;
    private Long totalRejected;
    private Long totalReturned;
    private Long totalPending;
    private Double overallApprovalRate;
    private Double overallRejectionRate;
    private Double overallReturnRate;
    private Double avgProcessingTime;

    // Time-based trends
    private List<ApprovalTrend> trends;

    // Breakdowns
    private List<ProductApprovalStats> productStats;
    private List<BranchApprovalStats> branchStats;
    private List<ApproverPerformance> topApprovers;

    // Performance metrics
    private Double slaComplianceRate;
    private Integer slaBreaches;
    //private Double avgSatisfactionScore;
    private Double riskAdjustedApprovalRate;

    // Amount metrics
    private Double totalApprovedAmount;
    private Double averageApprovedAmount;
    private Double largestApprovedAmount;
    private Double smallestApprovedAmount;

    // Time metrics
    private Integer fastestApprovalHours;
    private Integer slowestApprovalHours;
    private Double medianApprovalHours;

    // Approval Trend Inner Class
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalTrend {
        private String period; // "2024-01", "2024-02", "2024-01-15", etc.
        private String periodLabel; // "January 2024", "Week 1", "Monday"
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private Long totalApplications;
        private Long approvedCount;
        private Long rejectedCount;
        private Long returnedCount;
        private Long pendingCount;
        private Double approvalRate;
        private Double rejectionRate;
        private Double avgProcessingTime;
        private Double slaComplianceRate;
        private Double totalApprovedAmount;

        // Trend indicators
        private Boolean isImproving; // Compared to previous period
        private Double approvalRateChange; // Percentage change
        private String trendDirection; // UP, DOWN, STABLE
    }

    // Product Approval Stats Inner Class
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductApprovalStats {
        private String productType;
        private String productName;
        private String productCode;
        private Long totalApplications;
        private Long approvedCount;
        private Long rejectedCount;
        private Double approvalRate;
        private Double avgProcessingTime;
        private Double avgApprovedAmount;
        private Double totalApprovedAmount;
        private Integer riskScore;
        private String riskLevel;
    }

    // Branch Approval Stats Inner Class
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchApprovalStats {
        private Long branchId;
        private String branchName;
        private String branchCode;
        private String region;
        private Long totalApplications;
        private Long approvedCount;
        private Long rejectedCount;
        private Double approvalRate;
        private Double avgProcessingTime;
        private Double totalApprovedAmount;
        private Integer slaBreaches;
        private Double slaComplianceRate;
    }

    // Approver Performance Inner Class
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApproverPerformance {
        private Long approverId;
        private String approverName;
        private String approverRole;
        private String approverBranch;
        private Long totalDecisions;
        private Long approvedCount;
        private Long rejectedCount;
        private Long returnedCount;
        private Double approvalRate;
        private Double avgProcessingTime;
        private Double slaComplianceRate;
        private Integer slaBreaches;
        private Double satisfactionScore;
        private Double totalApprovedAmount;
        private Integer escalations;

        // Rankings
        private Integer rank;
        private String performanceCategory; // EXCELLENT, GOOD, AVERAGE, POOR
    }

    // Helper methods
    public void calculateDerivedMetrics() {
        // Calculate rates
        if (totalApplications != null && totalApplications > 0) {
            overallApprovalRate = calculateApprovalRate(totalApproved, totalApplications);
            overallRejectionRate = calculateApprovalRate(totalRejected, totalApplications);
            overallReturnRate = calculateApprovalRate(totalReturned, totalApplications);
        }

        // Calculate SLA compliance
        if (totalApplications != null && slaBreaches != null) {
            slaComplianceRate = (double) (totalApplications - slaBreaches) / totalApplications * 100;
        }
    }

    private Double calculateApprovalRate(Long count, Long total) {
        if (count == null || total == null || total == 0) {
            return 0.0;
        }
        return (double) count / total * 100;
    }
}