package com.microfinance.loanapplications.dto.approval;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkApprovalRequestDto {

    @NotEmpty(message = "Application IDs cannot be empty")
    private List<Long> applicationIds;

    private String comments;

    @NotNull(message = "Send notifications flag is required")
    private Boolean sendNotifications;

    // Additional options
    private Boolean overrideLimits;
    private String overrideReason;
    private List<ApprovalConditionDto> conditions;
}