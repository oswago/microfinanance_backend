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
public class DocumentVerificationDto {
    private Long id;
    private Long borrowerId;
    private String borrowerName;
    private Long borrowerDocumentId;
    private String borrowerDocumentName;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

