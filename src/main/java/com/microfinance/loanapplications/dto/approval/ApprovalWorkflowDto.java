package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalWorkflowDto {
    private Long applicationId;
    private String applicationNumber;
    private String currentStatus;
    private String currentStage;
    private Integer totalSteps;
    private Integer completedSteps;
    private String nextApprovalRole;
    private List<ApprovalWorkflowStepDto> workflowSteps;
    private Boolean canCurrentUserApprove;
    private String currentUserRole;

    public Integer getProgressPercentage() {
        if (totalSteps == null || totalSteps == 0) return 0;
        return (completedSteps * 100) / totalSteps;
    }
}

