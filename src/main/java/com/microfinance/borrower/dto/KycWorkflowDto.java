package com.microfinance.borrower.dto;

import com.microfinance.borrower.enums.KycWorkflowState;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class KycWorkflowDto {
    private Long id;
    private Long borrowerId;
    private String borrowerName;
    private KycWorkflowState currentState;
    private KycWorkflowState previousState;
    private String currentStep;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime estimatedCompletionDate;
    private String notes;
    private String assignedOfficerName;
    private List<KycWorkflowStepStatusDto> stepStatuses;
    private List<KycWorkflowHistoryDto> recentHistory;
    private int completionPercentage;
    private int daysInProgress;
}