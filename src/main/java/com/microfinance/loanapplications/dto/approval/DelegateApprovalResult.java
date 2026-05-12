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
public class DelegateApprovalResult {
    private Long delegationId;
    private Long applicationId;
    private String applicationNumber;
    private Long delegatorId;
    private String delegatorName;
    private Long delegateId;
    private String delegateName;
    private String delegateRole;
    private String reason;
    private LocalDateTime delegatedAt;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private String message;
    private Boolean success;
}