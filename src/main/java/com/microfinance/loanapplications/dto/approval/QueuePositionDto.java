package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueuePositionDto {
    private Long applicationId;
    private String applicationNumber;
    private Integer positionInQueue;
    private Integer totalPendingInQueue;
    private Integer estimatedWaitTimeMinutes;
    private String estimatedWaitTimeDisplay;
    private Integer applicationsAheadCount;
    private List<ApplicationAhead> applicationsAhead;
    private String message;
    
    // Priority information
    private Integer priorityScore;
    private String priorityLevel;
    private Boolean isPriority;
    private String priorityReason;
    
    // SLA information
    private LocalDateTime submittedAt;
    private Long hoursSinceSubmission;
    private LocalDateTime slaDeadline;
    private Boolean isOverdue;
    private Long hoursRemaining;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationAhead {
        private Long applicationId;
        private String applicationNumber;
        private String borrowerName;
        private BigDecimal amount;
        private Integer priorityScore;
        private String priorityLevel;
        private LocalDateTime submittedAt;
        private Long waitingHours;
        private String message;
    }
}