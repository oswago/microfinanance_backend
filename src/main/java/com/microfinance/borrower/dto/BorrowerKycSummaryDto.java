package com.microfinance.borrower.dto;

import com.microfinance.borrower.enums.KycWorkflowState;
import com.microfinance.common.config.DocumentConfig;
import com.microfinance.common.config.GeneralConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor  // ← Add this
@AllArgsConstructor // ← Add this
public class BorrowerKycSummaryDto {
    private Long borrowerId;
    private String borrowerName;
    private String borrowerPhoneNumber;
    private String borrowerEmail;
    private String borrowerOccupation;
    private Double borrowerMonthlyIncome;
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
    private GeneralConfig.KycStatus overallStatus;
    private List<DocumentStatusDto> documentStatuses;
    private Boolean kycComplete;
    private List<String> missingDocuments;
    private LocalDateTime lastVerificationDate;
    private String verifiedByName;
    private List<String>  documentsRequiredType;


    // Helper method
    public Boolean getIsKycComplete() {
        return currentState == KycWorkflowState.VERIFIED;
    }

}