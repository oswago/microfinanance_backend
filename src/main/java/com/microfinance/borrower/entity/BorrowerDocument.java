package com.microfinance.borrower.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microfinance.base.entity.BaseEntity;
import com.microfinance.common.config.DocumentConfig;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "borrower_documents")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BorrowerDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    @JsonIgnore  // Add this
    private Borrower borrower;

    @Enumerated(EnumType.STRING)
    @NotNull
    private DocumentConfig.DocumentType documentType;

    @NotBlank
    private String documentName;

    private String description;

    @NotBlank
    private String filePath;

    private String fileType;
    private Long fileSize;
    private String fileName;

    @Enumerated(EnumType.STRING)
    @NotNull
    private DocumentConfig.DocumentStatus status = DocumentConfig.DocumentStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private Long verifiedBy;

    private String verificationNotes;

    private LocalDate expiryDate;

    @Override
    public String toString() {
        return "BorrowerDocument{" +
                "id=" + id +
                ", documentType='" + documentType + '\'' +
                ", status=" + status +
                '}';
    }


}