package com.microfinance.loanapplications.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for Loan Application Statistics
 */
@Data
@Builder
public class ApplicationStatsDto {
    
    // Overall Statistics
    private LocalDate reportDate;
    private String periodType; // DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
    private LocalDate startDate;
    private LocalDate endDate;
    private Long branchId;
    private String branchName;
    
    // Application Counts
    private TotalCounts totalCounts;
    private StatusCounts statusCounts;
    private StageCounts stageCounts;
    
    // Financial Statistics
    private FinancialStats financialStats;
    
    // Approval Statistics
    private ApprovalStats approvalStats;
    
    // Performance Metrics
    private PerformanceMetrics performanceMetrics;
    
    // Trend Data
    private List<DailyStats> dailyTrends;
    private List<MonthlyStats> monthlyTrends;
    
    // Officer Performance
    private List<OfficerPerformance> officerPerformance;
    
    // Product-wise Statistics
    private List<ProductStats> productStats;
    
    // Risk Assessment
    private RiskStats riskStats;

    // Inner classes for organized data structure
    
    @Data
    @Builder
    public static class TotalCounts {
        private Long totalApplications;
        private Long newApplicationsToday;
        private Long newApplicationsThisWeek;
        private Long newApplicationsThisMonth;
        private Double growthRate; // Percentage growth compared to previous period
    }
    
    @Data
    @Builder
    public static class StatusCounts {
        private Long draftCount;
        private Long pendingApprovalCount;
        private Long approvedCount;
        private Long rejectedCount;
        private Long cancelledCount;
        private Long disbursedCount;
        
        // Calculated percentages
        public Double getApprovalRate() {
            if (totalProcessed() == 0) return 0.0;
            return (approvedCount.doubleValue() / totalProcessed()) * 100;
        }
        
        public Double getRejectionRate() {
            if (totalProcessed() == 0) return 0.0;
            return (rejectedCount.doubleValue() / totalProcessed()) * 100;
        }
        
        private Long totalProcessed() {
            return approvedCount + rejectedCount + cancelledCount;
        }
    }
    
    @Data
    @Builder
    public static class StageCounts {
        private Long applicationStageCount;
        private Long underReviewCount;
        private Long creditAssessmentCount;
        private Long approvalStageCount;
        private Long disbursementStageCount;
        private Long activeStageCount;
    }
    
    @Data
    @Builder
    public static class FinancialStats {
        private BigDecimal totalAppliedAmount;
        private BigDecimal averageApplicationAmount;
        private BigDecimal minApplicationAmount;
        private BigDecimal maxApplicationAmount;
        private BigDecimal totalApprovedAmount;
        private BigDecimal totalDisbursedAmount;
        
        // Amount by status
        private BigDecimal draftAmount;
        private BigDecimal pendingApprovalAmount;
        private BigDecimal approvedAmount;
        private BigDecimal rejectedAmount;
        private BigDecimal disbursedAmount;
        
        // Conversion rates
        public Double getApprovalToApplicationRate() {
            if (totalAppliedAmount.compareTo(BigDecimal.ZERO) == 0) return 0.0;
            return (totalApprovedAmount.doubleValue() / totalAppliedAmount.doubleValue()) * 100;
        }
        
        public Double getDisbursementToApprovalRate() {
            if (totalApprovedAmount.compareTo(BigDecimal.ZERO) == 0) return 0.0;
            return (totalDisbursedAmount.doubleValue() / totalApprovedAmount.doubleValue()) * 100;
        }
    }
    
    @Data
    @Builder
    public static class ApprovalStats {
        private Long totalPendingApproval;
        private Long totalApproved;
        private Long totalRejected;
        private Long totalReturnedForRevision;
        
        private Double averageApprovalTime; // in days
        private Double averageProcessingTime; // in days
        
        // Approval levels
        private Long level1Approvals;
        private Long level2Approvals;
        private Long level3Approvals;
        
        // Time-based metrics
        private Long applicationsApprovedWithin24h;
        private Long applicationsApprovedWithin48h;
        private Long applicationsApprovedWithin7days;
        private Long applicationsPendingOver7days;
        
        public Double getAverageApprovalTimeInDays() {
            return averageApprovalTime != null ? averageApprovalTime : 0.0;
        }
    }
    
    @Data
    @Builder
    public static class PerformanceMetrics {
        // Time-based metrics
        private Double averageApplicationToSubmissionTime; // hours
        private Double averageSubmissionToApprovalTime; // hours
        private Double averageApprovalToDisbursementTime; // hours
        private Double averageTotalProcessingTime; // hours
        
        // Efficiency metrics
        private Integer applicationsPerOfficer;
        private Double applicationsProcessedPerDay;
        private Double backlogSize; // applications pending > 3 days
        
        // Quality metrics
        private Double dataCompletenessRate; // percentage of applications with complete data
        private Double documentationCompletionRate;
        private Double kycVerificationRate;
    }
    
    @Data
    @Builder
    public static class DailyStats {
        private LocalDate date;
        private Long applicationsReceived;
        private Long applicationsApproved;
        private Long applicationsRejected;
        private Long applicationsDisbursed;
        private BigDecimal totalAmountApplied;
        private BigDecimal totalAmountApproved;
        private BigDecimal totalAmountDisbursed;
        
        // Calculated fields
        public Double getDailyApprovalRate() {
            if (applicationsReceived == 0) return 0.0;
            return (applicationsApproved.doubleValue() / applicationsReceived.doubleValue()) * 100;
        }
    }
    
    @Data
    @Builder
    public static class MonthlyStats {
        private String monthYear; // "YYYY-MM"
        private String monthName; // "January 2024"
        private Long applicationsReceived;
        private Long applicationsApproved;
        private Long applicationsRejected;
        private Long applicationsDisbursed;
        private BigDecimal totalAmountApplied;
        private BigDecimal totalAmountApproved;
        private BigDecimal totalAmountDisbursed;
        private Double approvalRate;
        private Double disbursementRate;
    }
    
    @Data
    @Builder
    public static class OfficerPerformance {
        private Long officerId;
        private String officerName;
        private String officerCode;
        private Long totalApplications;
        private Long applicationsApproved;
        private Long applicationsRejected;
        private Long applicationsDisbursed;
        private BigDecimal totalAmountApplied;
        private BigDecimal totalAmountApproved;
        private BigDecimal totalAmountDisbursed;
        private Double approvalRate;
        private Double rejectionRate;
        private Double averageProcessingTime; // hours
        private Integer rank; // among officers
        
        // Performance indicators
        private String performanceLevel; // EXCELLENT, GOOD, AVERAGE, POOR
        private Boolean meetsTarget;
        private Double targetAchievementRate; // percentage
    }
    
    @Data
    @Builder
    public static class ProductStats {
        private Long productId;
        private String productName;
        private String productCode;
        private Long totalApplications;
        private Long approvedApplications;
        private Long disbursedApplications;
        private BigDecimal totalAmountApplied;
        private BigDecimal totalAmountApproved;
        private BigDecimal totalAmountDisbursed;
        private Double approvalRate;
        private Double popularityRate; // percentage of total applications
        
        // Risk metrics
        private Double defaultRate; // for historical data
        private Double riskScore;
    }
    
    @Data
    @Builder
    public static class RiskStats {
        private RiskDistribution riskDistribution;
        private CreditScoreStats creditScoreStats;
        private List<HighRiskApplication> highRiskApplications;
        
        @Data
        @Builder
        public static class RiskDistribution {
            private Long lowRiskCount;
            private Long mediumRiskCount;
            private Long highRiskCount;
            private Long veryHighRiskCount;
            
            public Double getLowRiskPercentage() {
                return calculatePercentage(lowRiskCount);
            }
            
            public Double getHighRiskPercentage() {
                return calculatePercentage(highRiskCount + veryHighRiskCount);
            }
            
            private Double calculatePercentage(Long count) {
                Long total = lowRiskCount + mediumRiskCount + highRiskCount + veryHighRiskCount;
                if (total == 0) return 0.0;
                return (count.doubleValue() / total.doubleValue()) * 100;
            }
        }
        
        @Data
        @Builder
        public static class CreditScoreStats {
            private Double averageCreditScore;
            private Integer minCreditScore;
            private Integer maxCreditScore;
            private Integer medianCreditScore;
            
            // Score distribution
            private Long excellentScoreCount; // 800-850
            private Long goodScoreCount; // 700-799
            private Long fairScoreCount; // 600-699
            private Long poorScoreCount; // 300-599
        }
        
        @Data
        @Builder
        public static class HighRiskApplication {
            private Long applicationId;
            private String applicationNumber;
            private String borrowerName;
            private String riskLevel;
            private Integer creditScore;
            private BigDecimal appliedAmount;
            private String riskFactors; // comma-separated risk factors
        }
    }
    
    // Helper methods for calculated fields
    public Double getOverallApprovalRate() {
        if (statusCounts == null || statusCounts.totalProcessed() == 0) return 0.0;
        return statusCounts.getApprovalRate();
    }
    
    public Double getOverallRejectionRate() {
        if (statusCounts == null || statusCounts.totalProcessed() == 0) return 0.0;
        return statusCounts.getRejectionRate();
    }
    
    public Long getTotalPendingApplications() {
        if (statusCounts == null) return 0L;
        return statusCounts.getDraftCount() + statusCounts.getPendingApprovalCount();
    }
    
    public BigDecimal getPipelineValue() {
        if (financialStats == null) return BigDecimal.ZERO;
        return financialStats.getDraftAmount()
                .add(financialStats.getPendingApprovalAmount())
                .add(financialStats.getApprovedAmount());
    }
    
    public String getPerformanceSummary() {
        Double approvalRate = getOverallApprovalRate();
        if (approvalRate >= 80) return "EXCELLENT";
        if (approvalRate >= 70) return "GOOD";
        if (approvalRate >= 60) return "AVERAGE";
        return "NEEDS_IMPROVEMENT";
    }
    
    // Static factory methods for easy creation
    public static ApplicationStatsDto empty() {
        return ApplicationStatsDto.builder()
                .totalCounts(TotalCounts.builder()
                        .totalApplications(0L)
                        .newApplicationsToday(0L)
                        .newApplicationsThisWeek(0L)
                        .newApplicationsThisMonth(0L)
                        .growthRate(0.0)
                        .build())
                .statusCounts(StatusCounts.builder()
                        .draftCount(0L)
                        .pendingApprovalCount(0L)
                        .approvedCount(0L)
                        .rejectedCount(0L)
                        .cancelledCount(0L)
                        .disbursedCount(0L)
                        .build())
                .financialStats(FinancialStats.builder()
                        .totalAppliedAmount(BigDecimal.ZERO)
                        .averageApplicationAmount(BigDecimal.ZERO)
                        .minApplicationAmount(BigDecimal.ZERO)
                        .maxApplicationAmount(BigDecimal.ZERO)
                        .totalApprovedAmount(BigDecimal.ZERO)
                        .totalDisbursedAmount(BigDecimal.ZERO)
                        .build())
                .build();
    }
    
    public static ApplicationStatsDtoBuilder builder() {
        return new ApplicationStatsDtoBuilder();
    }
}