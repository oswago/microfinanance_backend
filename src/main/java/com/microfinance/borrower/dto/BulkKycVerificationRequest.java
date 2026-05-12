package com.microfinance.borrower.dto;

import com.microfinance.borrower.entity.Borrower;
import com.microfinance.common.config.GeneralConfig;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkKycVerificationRequest {
    
    @NotNull(message = "Borrower IDs are required")
    private List<Long> borrowerIds;
    
    @NotNull(message = "KYC status is required")
    private GeneralConfig.KycStatus kycStatus;
    
    private String verificationNotes;
    
    private Boolean sendNotification = false;
    
    private String notificationTemplate;
    
    // Optional: If you want to specify different notes per borrower
    private List<BorrowerVerificationDetail> borrowerDetails;

    @Data
    public static class BorrowerVerificationDetail {
        @NotNull
        private Long borrowerId;
        
        private String specificNotes;
        
        private Boolean requiresFollowUp = false;
        
        private String followUpAction;
    }

    // Validation method
    public boolean isValid() {
        return borrowerIds != null && !borrowerIds.isEmpty() && kycStatus != null;
    }

    // Helper method to check if this is a verification (approval) action
    public boolean isVerificationAction() {
        return kycStatus == GeneralConfig.KycStatus.VERIFIED;
    }

    // Helper method to check if this is a rejection action
    public boolean isRejectionAction() {
        return kycStatus == GeneralConfig.KycStatus.REJECTED;
    }
}