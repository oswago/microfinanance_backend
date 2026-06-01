package com.microfinance.loanapplications.dto.approval;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApprovalWorkflowStepDto {
    
    // Basic identification
    private Integer stepNumber;
    private String stepCode;
    private String stepName;
    private String stepDescription;
    
    // Role information
    private String role;
    private String approvalRole;
    private String roleDisplay;
    
    // Status - this is the ONLY field you should set manually
    private String status;            // PENDING, APPROVED, REJECTED, SKIPPED, COMPLETED
    private String decision;
    
    // Completion tracking
    private Boolean isCompleted;
    private Boolean isCurrentStep;
    private Boolean isRequired;
    private Boolean isOverdue;
    
    // Approver information
    private String approverName;
    private String approverUsername;
    private String comments;
    private LocalDateTime decisionDate;
    
    // Timing information
    private Long processedAt;
    private Long slaDeadline;
    
    // ========== COMPUTED FIELDS (DO NOT SET MANUALLY) ==========
    
    /**
     * Computed from status and isCurrentStep
     * DO NOT SET THIS DIRECTLY - it's derived
     */
    @JsonIgnore
    public String getStatusDisplay() {
        if (status == null) return "Not Started";
        
        switch (status.toUpperCase()) {
            case "APPROVED":
                return "Approved";
            case "REJECTED":
                return "Rejected";
            case "PENDING":
                return (isCurrentStep != null && isCurrentStep) ? "Current Step" : "Pending";
            case "SKIPPED":
                return "Skipped";
            case "COMPLETED":
                return "Completed";
            case "IN_PROGRESS":
                return "In Progress";
            default:
                return status;
        }
    }
    
    /**
     * Computed from status and isCurrentStep
     * DO NOT SET THIS DIRECTLY - it's derived
     */
    @JsonIgnore
    public String getStatusColor() {
        if (status == null) return "secondary";
        
        switch (status.toUpperCase()) {
            case "APPROVED":
            case "COMPLETED":
                return "success";
            case "REJECTED":
                return "danger";
            case "PENDING":
                return (isCurrentStep != null && isCurrentStep) ? "info" : "warning";
            case "SKIPPED":
                return "secondary";
            case "IN_PROGRESS":
                return "info";
            default:
                return "secondary";
        }
    }
    
    // ========== CONVENIENCE METHODS ==========
    
    public boolean isCompleted() {
        return isCompleted != null && isCompleted;
    }
    
    public boolean isCurrentStep() {
        return isCurrentStep != null && isCurrentStep;
    }
    
    public boolean isOverdue() {
        return isOverdue != null && isOverdue;
    }
    
    public void setOverdue(boolean overdue) {
        this.isOverdue = overdue;
    }
    
    public void setCurrentStep(boolean currentStep) {
        this.isCurrentStep = currentStep;
    }
    
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
        if (completed && status == null) {
            this.status = "COMPLETED";
        }
    }
    
    // Helper method to set status consistently
    public void setStatusAndDerived(String status, boolean isCurrentStep) {
        this.status = status;
        this.isCurrentStep = isCurrentStep;
        this.isCompleted = "APPROVED".equals(status) || "COMPLETED".equals(status);
        // statusDisplay and statusColor are computed automatically
    }
}