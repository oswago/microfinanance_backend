package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalTimelineDto {
    private Long applicationId;
    private String applicationNumber;
    private String borrowerName;
    private BigDecimal appliedAmount;
    private List<TimelineEvent> events;
    private LocalDateTime applicationDate;
    private LocalDateTime submittedDate;
    private LocalDateTime expectedCompletionDate;
    private String currentStatus;
    private String currentStage;
    
    // Summary statistics
    private Integer totalEvents;
    private Integer totalApprovals;
    private Integer totalRejections;
    private Integer totalReturns;
    private Long totalProcessingTimeHours;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEvent {
        private LocalDateTime timestamp;
        private String eventType;
        private String eventTypeDisplay;
        private String description;
        private String actor;
        private String actorUsername;
        private String actorRole;
        private String statusBefore;
        private String statusAfter;
        private Map<String, Object> metadata;
        private String comments;
        
        // For approval events
        private String decision;
        private Integer approvalLevel;
        
        // For SLA tracking
        private Boolean isBreached;
        private Long slaHoursRemaining;
    }
}