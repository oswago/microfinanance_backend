package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationResult {
    private Long escalationId;
    private Long applicationId;
    private String applicationNumber;
    private String escalatedBy;
    private String escalatedByUsername;
    private String reason;
    private String priority;
    private String status; // PENDING, APPROVED, REJECTED, COMPLETED
    private LocalDateTime escalatedAt;
    private String escalatedToRole;
    private List<EscalationTarget> targets;
    private String message;
    private Boolean success;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EscalationTarget {
        private Long userId;
        private String userName;
        private String userRole;
        private String userEmail;
        private Boolean notified;
    }
}