package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDelegationDto {
    private Long id;
    private Long applicationId;
    private String applicationNumber;
    private Long delegatorId;
    private String delegatorName;
    private String delegatorUsername;
    private Long delegateId;
    private String delegateName;
    private String delegateUsername;
    private String delegateRole;
    private String reason;
    private String status; // ACTIVE, EXPIRED, REVOKED, COMPLETED
    private LocalDateTime delegatedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String revokedBy;
    private String revocationReason;
    private Boolean isActive;
    
    // Statistics
    private Integer applicationsDelegated;
    private Integer applicationsApproved;
    private Integer applicationsPending;
}