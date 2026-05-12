package com.microfinance.borrower.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * Enhanced KYC summary that wraps the existing DTO and adds workflow information
 * This is an ADDITION only - doesn't modify existing DTOs
 */
@Data
@Builder
public class EnhancedKycSummaryDto {
    private BorrowerKycSummaryDto basicSummary;
    private List<KycWorkflowStepDto> workflowSteps;
    private WorkflowProgress workflowProgress;
    private String currentStep;
    private String nextAction;
    
    @Data
    @Builder
    public static class WorkflowProgress {
        private Integer completedSteps;
        private Integer totalSteps;
        private Integer completionPercentage;
        private Integer pendingSteps;
    }
    
    // Helper methods
    public Boolean isKycComplete() {
        return basicSummary != null && basicSummary.getKycComplete();
    }
    
    public String getOverallStatus() {
        return basicSummary != null ? basicSummary.getOverallStatus().name() : "UNKNOWN";
    }
}