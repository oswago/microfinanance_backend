package com.microfinance.loanapplications.dto.approval;

import com.microfinance.loanapplications.dto.ApplicationApprovalDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO for displaying approval workflow summary
 */
@Data
@Builder
public class ApprovalSummaryDto {
    private Long loanApplicationId;
    private String applicationNumber;
    private String currentStatus;
    private String currentStage;
    private Integer totalApprovalLevels;
    private Integer completedApprovalLevels;
    private String nextApprovalRole;
    private Boolean requiresMultipleApprovals;
    private List<ApprovalWorkflowStepDto> workflowSteps;
    private List<ApplicationApprovalDto> approvalHistory;
    private Boolean canBeApprovedByCurrentUser;
    private String overallDecision;

    // Add the missing conditions property
    private List<ApprovalConditionDto> conditions;

    // Additional useful properties
    private Integer pendingConditions;
    private Integer completedConditions;
    private Boolean allConditionsMet;
    private String slaStatus;
    private Long slaHoursRemaining;
    private Boolean canAddConditions;
    private Boolean canCompleteConditions;

    // Progress percentage for UI
    public Integer getApprovalProgress() {
        if (totalApprovalLevels == null || totalApprovalLevels == 0) {
            return 0;
        }
        return (completedApprovalLevels * 100) / totalApprovalLevels;
    }

    // Calculate condition progress
    public Integer getConditionProgress() {
        if (conditions == null || conditions.isEmpty()) {
            return 100; // No conditions means 100% complete
        }
        long completed = conditions.stream()
                .filter(condition -> "COMPLETED".equals(condition.getStatus()))
                .count();
        return (int) ((completed * 100) / conditions.size());
    }

    public Boolean getIsFullyApproved() {
        return "APPROVED".equalsIgnoreCase(overallDecision);
    }

    public Boolean getIsRejected() {
        return "REJECTED".equalsIgnoreCase(overallDecision);
    }

    public Boolean getIsPending() {
        return "PENDING".equalsIgnoreCase(currentStatus);
    }

    // Helper to check if all mandatory conditions are met
    public Boolean areMandatoryConditionsMet() {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        return conditions.stream()
                .filter(condition -> Boolean.TRUE.equals(condition.getIsMandatory()))
                .allMatch(condition -> "COMPLETED".equals(condition.getStatus()));
    }

    // Helper to get pending conditions count
    public Integer getPendingConditionsCount() {
        if (conditions == null) return 0;
        return (int) conditions.stream()
                .filter(condition -> "PENDING".equals(condition.getStatus()))
                .count();
    }

    // Helper to get overdue conditions
    public List<ApprovalConditionDto> getOverdueConditions() {
        if (conditions == null) return List.of();
        return conditions.stream()
                .filter(condition -> {
                    if ("COMPLETED".equals(condition.getStatus())) {
                        return false;
                    }
                    if (condition.getDueDate() == null) {
                        return false;
                    }
                    return java.time.LocalDateTime.now().isAfter(
                            java.time.LocalDateTime.parse(condition.getDueDate()));
                })
                .collect(java.util.stream.Collectors.toList());
    }
}