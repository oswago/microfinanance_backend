package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.base.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loan_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false, length = 50)
    private String documentType; // LOAN_AGREEMENT, DISBURSEMENT_RECEIPT, REPAYMENT_RECEIPT, COLLATERAL, etc.

    @Column(nullable = false, length = 255)
    private String documentName;

    @Column(nullable = false, unique = true, length = 50)
    private String documentReference;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(length = 50)
    private String fileSize; // in bytes or human readable

    @Column(length = 100)
    private String mimeType; // application/pdf, image/jpeg, etc.

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column(nullable = false)
    private Boolean isVerified = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_id")
    private User verifiedBy;

    private LocalDateTime verifiedAt;

    @Column(columnDefinition = "TEXT")
    private String verificationNotes;

    @Column(nullable = false)
    private Boolean isActive = true;

    private LocalDateTime expiryDate; // For documents that expire

    @Column(nullable = false)
    private Integer version = 1;

    private Long previousVersionId; // For document versioning

    @Column(length = 255)
    private String checksum; // For file integrity verification

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON field for additional document metadata

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        if (documentReference == null) {
            documentReference = generateDocumentReference();
        }
    }

    private String generateDocumentReference() {
        return "DOC-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    // Helper methods
    public void verify(User verifier, String notes) {
        this.isVerified = true;
        this.verifiedBy = verifier;
        this.verifiedAt = LocalDateTime.now();
        this.verificationNotes = notes;
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDateTime.now());
    }

    public String getFileExtension() {
        if (documentName != null && documentName.contains(".")) {
            return documentName.substring(documentName.lastIndexOf(".") + 1);
        }
        return "";
    }

    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    public boolean isPdf() {
        return "application/pdf".equals(mimeType);
    }

    public LoanDocument createNewVersion() {
        return LoanDocument.builder()
                .loan(this.loan)
                .documentType(this.documentType)
                .documentName(this.documentName)
                .documentReference(generateDocumentReference())
                .description(this.description)
                .isVerified(false)
                .isActive(true)
                .version(this.version + 1)
                .previousVersionId(this.id)
                .build();
    }
}