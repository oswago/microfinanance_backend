package com.microfinance.loanapplications.dto.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * DTO for submitting approval decisions
 */
@Data
public class ApprovalDecisionDto {

    @NotBlank(message = "Comments are required for approval decision")
    @Size(max = 1000, message = "Comments cannot exceed 1000 characters")
    private String comments;

    @NotBlank(message = "Decision is required")
    private String decision; // APPROVE, REJECT, RETURN_FOR_REVISION

    private String approvalRole;
    private Boolean recommendForNextLevel;
    private String riskAssessment;
    private List<String> conditions;
    private String rejectionReason;
    private List<String> requirements;
    private Boolean isConditionalApproval;
    private List<ApprovalConditionDto> conditionsList;
    private Boolean overrideLimits;
    private String overrideReason;
    private Boolean sendNotification;
    private Boolean sendEmail;
    private Boolean sendSms;

    public static ApprovalDecisionDtoBuilder builder() {
        return new ApprovalDecisionDtoBuilder();
    }

    public static class ApprovalDecisionDtoBuilder {
        private String comments;
        private String decision;
        private String approvalRole;
        private Boolean recommendForNextLevel;
        private String riskAssessment;
        private String conditions;
        private String rejectionReason;
        private List<String> requirements;
        private Boolean isConditionalApproval;
        private List<ApprovalConditionDto> conditionsList;
        private Boolean overrideLimits;
        private String overrideReason;
        private Boolean sendNotification;
        private Boolean sendEmail;
        private Boolean sendSms;

        public ApprovalDecisionDtoBuilder comments(String comments) {
            this.comments = comments;
            return this;
        }

        public ApprovalDecisionDtoBuilder decision(String decision) {
            this.decision = decision;
            return this;
        }

        public ApprovalDecisionDtoBuilder approvalRole(String approvalRole) {
            this.approvalRole = approvalRole;
            return this;
        }

        public ApprovalDecisionDtoBuilder recommendForNextLevel(Boolean recommendForNextLevel) {
            this.recommendForNextLevel = recommendForNextLevel;
            return this;
        }

        public ApprovalDecisionDtoBuilder riskAssessment(String riskAssessment) {
            this.riskAssessment = riskAssessment;
            return this;
        }

        public ApprovalDecisionDtoBuilder conditions(String conditions) {
            this.conditions = conditions;
            return this;
        }

        public ApprovalDecisionDtoBuilder rejectionReason(String rejectionReason) {
            this.rejectionReason = rejectionReason;
            return this;
        }

        public ApprovalDecisionDtoBuilder requirements(List<String> requirements) {
            this.requirements = requirements;
            return this;
        }

        public ApprovalDecisionDtoBuilder isConditionalApproval(Boolean isConditionalApproval) {
            this.isConditionalApproval = isConditionalApproval;
            return this;
        }

        public ApprovalDecisionDtoBuilder conditionsList(List<ApprovalConditionDto> conditionsList) {
            this.conditionsList = conditionsList;
            return this;
        }

        public ApprovalDecisionDtoBuilder overrideLimits(Boolean overrideLimits) {
            this.overrideLimits = overrideLimits;
            return this;
        }

        public ApprovalDecisionDtoBuilder overrideReason(String overrideReason) {
            this.overrideReason = overrideReason;
            return this;
        }

        public ApprovalDecisionDtoBuilder sendNotification(Boolean sendNotification) {
            this.sendNotification = sendNotification;
            return this;
        }

        public ApprovalDecisionDtoBuilder sendEmail(Boolean sendEmail) {
            this.sendEmail = sendEmail;
            return this;
        }

        public ApprovalDecisionDtoBuilder sendSms(Boolean sendSms) {
            this.sendSms = sendSms;
            return this;
        }

        public ApprovalDecisionDto build() {
            ApprovalDecisionDto dto = new ApprovalDecisionDto();
            dto.setComments(this.comments);
            dto.setDecision(this.decision);
            dto.setApprovalRole(this.approvalRole);
            dto.setRecommendForNextLevel(this.recommendForNextLevel);
            dto.setRiskAssessment(this.riskAssessment);
            dto.setConditions(Collections.singletonList(this.conditions));
            dto.setRejectionReason(this.rejectionReason);
            dto.setRequirements(this.requirements);
            dto.setIsConditionalApproval(this.isConditionalApproval);
            dto.setConditionsList(this.conditionsList);
            dto.setOverrideLimits(this.overrideLimits);
            dto.setOverrideReason(this.overrideReason);
            dto.setSendNotification(this.sendNotification);
            dto.setSendEmail(this.sendEmail);
            dto.setSendSms(this.sendSms);
            return dto;
        }
    }
}