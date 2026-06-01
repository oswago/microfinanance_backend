package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SLA Status DTO for tracking approval Service Level Agreement compliance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SLAStatusDto {
    
    // Application information
    private Long applicationId;
    private String applicationNumber;
    
    // SLA configuration
    private String slaLevel;           // STANDARD, EXPRESS, URGENT
    private LocalDateTime slaStartDate;
    private LocalDateTime slaDueDate;
    private Long slaTotalHours;         // Total SLA hours allocated
    
    // Time tracking
    private Long hoursRemaining;
    private Long hoursElapsed;
    
    // Status information
    private String status;              // ON_TRACK, AT_RISK, BREACHED
    private Double completionPercentage;
    
    // Next actions
    private String nextAction;          // APPROVAL_DECISION, DOCUMENT_UPLOAD, etc.
    private LocalDateTime nextActionDue;
    
    // Additional fields for detailed tracking
    private String slaBreachReason;     // Reason if SLA is breached
    private LocalDateTime breachDetectedAt;  // When breach was detected
    private Integer escalationLevel;    // Escalation level (1, 2, 3)
    private Boolean isNotified;         // Whether SLA breach notification was sent
    private LocalDateTime lastNotificationSent;
    
    /**
     * Check if SLA is breached
     * @return true if status is BREACHED
     */
    public boolean isBreached() {
        return "BREACHED".equals(status);
    }
    
    /**
     * Check if SLA is at risk
     * @return true if status is AT_RISK
     */
    public boolean isAtRisk() {
        return "AT_RISK".equals(status);
    }
    
    /**
     * Check if SLA is on track
     * @return true if status is ON_TRACK
     */
    public boolean isOnTrack() {
        return "ON_TRACK".equals(status);
    }
    
    /**
     * Get formatted time remaining (e.g., "2h 30m" or "1d 5h")
     */
    public String getFormattedTimeRemaining() {
        if (hoursRemaining == null) return "N/A";
        
        if (hoursRemaining < 24) {
            return String.format("%dh %dm", hoursRemaining, 0L);
        } else {
            long days = hoursRemaining / 24;
            long remainingHours = hoursRemaining % 24;
            if (remainingHours == 0) {
                return String.format("%dd", days);
            }
            return String.format("%dd %dh", days, remainingHours);
        }
    }
    
    /**
     * Get formatted time elapsed (e.g., "2h 30m" or "1d 5h")
     */
    public String getFormattedTimeElapsed() {
        if (hoursElapsed == null) return "N/A";
        
        if (hoursElapsed < 24) {
            return String.format("%dh", hoursElapsed);
        } else {
            long days = hoursElapsed / 24;
            long remainingHours = hoursElapsed % 24;
            if (remainingHours == 0) {
                return String.format("%dd", days);
            }
            return String.format("%dd %dh", days, remainingHours);
        }
    }
    
    /**
     * Get urgency color for UI
     */
    public String getUrgencyColor() {
        if (isBreached()) return "danger";
        if (isAtRisk()) return "warning";
        return "success";
    }
    
    /**
     * Get urgency icon for UI
     */
    public String getUrgencyIcon() {
        if (isBreached()) return "pi pi-exclamation-triangle";
        if (isAtRisk()) return "pi pi-clock";
        return "pi pi-check-circle";
    }
    
    /**
     * Get urgency label for UI
     */
    public String getUrgencyLabel() {
        if (isBreached()) return "SLA Breached";
        if (isAtRisk()) return "SLA At Risk";
        return "SLA On Track";
    }
    
    /**
     * Calculate SLA health score (0-100)
     * 100 = Excellent, 0 = Critical breach
     */
    public Integer getSlaHealthScore() {
        if (completionPercentage == null) return 100;
        
        if (isBreached()) {
            return Math.max(0, 50 - (int)(completionPercentage / 2));
        } else if (isAtRisk()) {
            return (int)(50 + (completionPercentage / 2));
        } else {
            return (int)(75 + (completionPercentage / 4));
        }
    }
    
    /**
     * Builder class with additional convenience methods
     */
    public static class SLAStatusDtoBuilder {
        public SLAStatusDtoBuilder breached(boolean breached) {
            if (breached) {
                this.status = "BREACHED";
            }
            return this;
        }
        
        public SLAStatusDtoBuilder fromStatus(String status) {
            this.status = status;
            return this;
        }
    }
    
    // Static factory methods
    public static SLAStatusDto onTrack(Long applicationId, String applicationNumber) {
        return SLAStatusDto.builder()
                .applicationId(applicationId)
                .applicationNumber(applicationNumber)
                .status("ON_TRACK")
                .slaLevel("STANDARD")
                .build();
    }
    
    public static SLAStatusDto atRisk(Long applicationId, String applicationNumber) {
        return SLAStatusDto.builder()
                .applicationId(applicationId)
                .applicationNumber(applicationNumber)
                .status("AT_RISK")
                .slaLevel("STANDARD")
                .build();
    }
    
    public static SLAStatusDto breached(Long applicationId, String applicationNumber, String reason) {
        return SLAStatusDto.builder()
                .applicationId(applicationId)
                .applicationNumber(applicationNumber)
                .status("BREACHED")
                .slaLevel("STANDARD")
                .slaBreachReason(reason)
                .breachDetectedAt(LocalDateTime.now())
                .build();
    }
}