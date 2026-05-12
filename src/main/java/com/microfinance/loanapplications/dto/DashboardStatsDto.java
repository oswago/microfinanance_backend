package com.microfinance.loanapplications.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for real-time dashboard statistics
 */
@Data
@Builder
public class DashboardStatsDto {
    private LocalDateTime lastUpdated;
    private QuickStats quickStats;
    private RecentActivity recentActivity;
    private PerformanceIndicators performanceIndicators;
    
    @Data
    @Builder
    public static class QuickStats {
        private Long pendingApproval;
        private Long approvedToday;
        private Long disbursedToday;
        private BigDecimal totalPipelineValue;
        private Long overdueApplications; // applications pending > SLA
    }
    
    @Data
    @Builder
    public static class RecentActivity {
        private List<RecentApplication> recentApplications;
        private List<RecentApproval> recentApprovals;
        private List<RecentDisbursement> recentDisbursements;
    }
    
    @Data
    @Builder
    public static class PerformanceIndicators {
        private Double todayApprovalRate;
        private Double weekToDateApprovalRate;
        private Double monthToDateApprovalRate;
        private Integer slaBreachesToday;
        private Double averageProcessingTimeToday;
    }
    
    @Data
    @Builder
    public static class RecentApplication {
        private String applicationNumber;
        private String borrowerName;
        private BigDecimal amount;
        private String status;
        private LocalDateTime submittedAt;
    }
    
    @Data
    @Builder
    public static class RecentApproval {
        private String applicationNumber;
        private String approverName;
        private BigDecimal amount;
        private LocalDateTime approvedAt;
    }
    
    @Data
    @Builder
    public static class RecentDisbursement {
        private String loanAccountNumber;
        private String borrowerName;
        private BigDecimal amount;
        private LocalDateTime disbursedAt;
    }
}