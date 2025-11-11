package com.microfinance.borrower.dto;

import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.enums.KycWorkflowState;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
@Data
public class BorrowerKycSummaryDto {
    private Long borrowerId;
    private String borrowerName;
    private Integer documentsUploaded;
    private Integer documentsVerified;
    private Integer documentsPending;
    private Integer documentsRequired;
    private Integer kycCompletionPercentage;
    private KycWorkflowState currentState;
    private String currentStep;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime estimatedCompletionDate;
    private String assignedOfficerName;
    private Integer pendingStepsCount;
    private Boolean isKycComplete;
    private Borrower.KycStatus overallStatus;
    private List<DocumentStatusDto> documentStatuses;
    private Boolean kycComplete;
    private List<String> missingDocuments;
    private LocalDateTime lastVerificationDate;
    private String verifiedByName;

    // Helper method
    public Boolean getIsKycComplete() {
        return currentState == KycWorkflowState.VERIFIED;
    }
}

