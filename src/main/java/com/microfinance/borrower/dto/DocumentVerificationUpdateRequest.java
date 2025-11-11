package com.microfinance.borrower.dto;

import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.borrower.entity.DocumentVerification;
import com.microfinance.common.config.DocumentConfig;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DocumentVerificationUpdateRequest {
    private DocumentConfig.DocumentType documentType;
    private String documentNumber;
    private String issuingAuthority;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private DocumentVerification.VerificationStatus verificationStatus;
    private String verifiedBy;
    private LocalDateTime verificationDate;
    private String verificationNotes;
    private String rejectionReason;
    private String additionalData;
    private Boolean isActive;
}
