package com.microfinance.loanapplications.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Application Approval records
 */
@Data
public class ApplicationApprovalDto {
    private Long id;
    private Long loanApplicationId;
    private String applicationNumber;
    private Long approverId;
    private String approverName;
    private String approverUsername;
    private String decision;
    private String comments;
    private Integer approvalLevel;
    private String approvalRole;
    private LocalDateTime decisionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional fields for better UI representation
    private String decisionDisplay;
    private String decisionDateFormatted;
    private String approverRole;
    private Boolean isFinalApproval;
    private String nextApprovalRole;


    private long processingTimeHours;

    boolean isSlaCompliant;

    // Static factory methods for convenience
    public static ApplicationApprovalDtoBuilder builder() {
        return new ApplicationApprovalDtoBuilder();
    }



    // Builder class for fluent creation
    public static class ApplicationApprovalDtoBuilder {
        private Long id;
        private Long loanApplicationId;
        private String applicationNumber;
        private Long approverId;
        private String approverName;
        private String approverUsername;
        private String decision;
        private String comments;
        private Integer approvalLevel;
        private String approvalRole;
        private LocalDateTime decisionDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ApplicationApprovalDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ApplicationApprovalDtoBuilder loanApplicationId(Long loanApplicationId) {
            this.loanApplicationId = loanApplicationId;
            return this;
        }

        public ApplicationApprovalDtoBuilder applicationNumber(String applicationNumber) {
            this.applicationNumber = applicationNumber;
            return this;
        }

        public ApplicationApprovalDtoBuilder approverId(Long approverId) {
            this.approverId = approverId;
            return this;
        }

        public ApplicationApprovalDtoBuilder approverName(String approverName) {
            this.approverName = approverName;
            return this;
        }

        public ApplicationApprovalDtoBuilder approverUsername(String approverUsername) {
            this.approverUsername = approverUsername;
            return this;
        }

        public ApplicationApprovalDtoBuilder decision(String decision) {
            this.decision = decision;
            return this;
        }

        public ApplicationApprovalDtoBuilder comments(String comments) {
            this.comments = comments;
            return this;
        }

        public ApplicationApprovalDtoBuilder approvalLevel(Integer approvalLevel) {
            this.approvalLevel = approvalLevel;
            return this;
        }

        public ApplicationApprovalDtoBuilder approvalRole(String approvalRole) {
            this.approvalRole = approvalRole;
            return this;
        }

        public ApplicationApprovalDtoBuilder decisionDate(LocalDateTime decisionDate) {
            this.decisionDate = decisionDate;
            return this;
        }

        public ApplicationApprovalDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ApplicationApprovalDtoBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ApplicationApprovalDto build() {
            ApplicationApprovalDto dto = new ApplicationApprovalDto();
            dto.setId(this.id);
            dto.setLoanApplicationId(this.loanApplicationId);
            dto.setApplicationNumber(this.applicationNumber);
            dto.setApproverId(this.approverId);
            dto.setApproverName(this.approverName);
            dto.setApproverUsername(this.approverUsername);
            dto.setDecision(this.decision);
            dto.setComments(this.comments);
            dto.setApprovalLevel(this.approvalLevel);
            dto.setApprovalRole(this.approvalRole);
            dto.setDecisionDate(this.decisionDate);
            dto.setCreatedAt(this.createdAt);
            dto.setUpdatedAt(this.updatedAt);
            return dto;
        }
    }

    // Helper methods for business logic
    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(decision);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(decision);
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(decision);
    }

    public boolean isReturnedForRevision() {
        return "RETURNED_FOR_REVISION".equalsIgnoreCase(decision);
    }

    public String getDecisionDisplay() {
        if (decision == null) return "Pending";
        
        switch (decision.toUpperCase()) {
            case "APPROVED":
                return "Approved";
            case "REJECTED":
                return "Rejected";
            case "PENDING":
                return "Pending Review";
            case "RETURNED_FOR_REVISION":
                return "Returned for Revision";
            default:
                return decision;
        }
    }

    public String getDecisionColor() {
        if (decision == null) return "warning";
        
        switch (decision.toUpperCase()) {
            case "APPROVED":
                return "success";
            case "REJECTED":
                return "danger";
            case "PENDING":
                return "warning";
            case "RETURNED_FOR_REVISION":
                return "info";
            default:
                return "secondary";
        }
    }

    public String getApprovalRoleDisplay() {
        if (approvalRole == null) return "Approver";
        
        switch (approvalRole.toUpperCase()) {
            case "BRANCH_MANAGER":
                return "Branch Manager";
            case "CREDIT_COMMITTEE":
                return "Credit Committee";
            case "REGIONAL_MANAGER":
                return "Regional Manager";
            case "LOAN_OFFICER":
                return "Loan Officer";
            case "RISK_MANAGER":
                return "Risk Manager";
            default:
                return approvalRole.replace("_", " ");
        }
    }

    // Getters for derived properties
    public Boolean getIsFinalApproval() {
        // Logic to determine if this is the final approval in the chain
        // This could be based on approval level or role
        return "CREDIT_COMMITTEE".equalsIgnoreCase(approvalRole) || 
               (approvalLevel != null && approvalLevel >= 3);
    }

    public String getNextApprovalRole() {
        if (isApproved() && !getIsFinalApproval()) {
            // Determine next approval role based on current level/role
            if ("LOAN_OFFICER".equalsIgnoreCase(approvalRole)) {
                return "BRANCH_MANAGER";
            } else if ("BRANCH_MANAGER".equalsIgnoreCase(approvalRole)) {
                return "CREDIT_COMMITTEE";
            } else if ("CREDIT_COMMITTEE".equalsIgnoreCase(approvalRole)) {
                return "FINAL_APPROVAL";
            }
        }
        return null;
    }
}