package com.microfinance.borrower.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.common.config.DocumentConfig;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_verifications")
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentVerification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_document_id")
    private BorrowerDocument borrowerDocument;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "document_type", nullable = false)
    private DocumentConfig.DocumentType documentType;

    @NotBlank
    @Column(name = "document_number", nullable = false)
    private String documentNumber;

    @NotBlank
    @Column(name = "issuing_authority", nullable = false)
    private String issuingAuthority;

    @NotNull
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verification_date")
    private LocalDateTime verificationDate;

    @Column(name = "verification_notes", length = 1000)
    private String verificationNotes;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "additional_data", columnDefinition = "TEXT")
    private String additionalData;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public enum VerificationStatus {
        PENDING, VERIFIED, REJECTED, EXPIRED
    }

    @PrePersist
    @PreUpdate
    private void updateVerificationTimestamp() {
        if (verificationStatus == VerificationStatus.VERIFIED && verificationDate == null) {
            verificationDate = LocalDateTime.now();
        }
    }
}