package com.microfinance.borrower.dto;

import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.borrower.entity.DocumentVerification;
import com.microfinance.common.config.DocumentConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DocumentVerificationCreateRequest {
    @NotNull
    private Long borrowerId;
    
    private Long borrowerDocumentId;
    
    @NotNull
    private DocumentConfig.DocumentType documentType;
    
    @NotBlank
    private String documentNumber;
    
    @NotBlank
    private String issuingAuthority;
    
    @NotNull
    private LocalDate issueDate;
    
    private LocalDate expiryDate;
    
    @NotNull
    private DocumentVerification.VerificationStatus verificationStatus = DocumentVerification.VerificationStatus.PENDING;
    
    private String verifiedBy;
    private LocalDateTime verificationDate;
    private String verificationNotes;
    private String rejectionReason;
    private String additionalData;
    
    @NotNull
    private Boolean isActive = true;
}
