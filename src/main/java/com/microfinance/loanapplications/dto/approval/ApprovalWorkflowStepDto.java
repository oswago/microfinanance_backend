package com.microfinance.loanapplications.dto.approval;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for individual approval workflow steps
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalWorkflowStepDto {
    private Integer stepNumber;
    private String role;
    private String stepName;
    private String stepDescription;
    private String roleDisplay;
    private String status; // PENDING, APPROVED, REJECTED, SKIPPED
    private String decision;
    private String approverName;
    private LocalDateTime decisionDate;
    private String comments;
    private Boolean isCurrentStep;
    private Boolean isCompleted;
    private Boolean isRequired;
    private Boolean isOverdue;
    private String approvalRole;
    private String approverUsername;
    private String stepCode;
    private long processedAt;
    private long slaDeadline;
    private boolean overdue;
    private boolean currentStep;

    public String getStatusColor() {
        if (status == null) return "secondary";

        switch (status.toUpperCase()) {
            case "APPROVED":
                return "success";
            case "REJECTED":
                return "danger";
            case "PENDING":
                return "warning";
            case "SKIPPED":
                return "info";
            default:
                return "secondary";
        }
    }

    public String getStatusDisplay() {
        if (status == null) return "Not Started";

        switch (status.toUpperCase()) {
            case "APPROVED":
                return "Approved";
            case "REJECTED":
                return "Rejected";
            case "PENDING":
                return isCurrentStep != null && isCurrentStep ? "Current Step" : "Pending";
            case "SKIPPED":
                return "Skipped";
            default:
                return status;
        }
    }

    public void setStepCode(String stepCode) {
        this.stepCode = stepCode;
    }

    public String getStepCode() {
        return stepCode;
    }
}