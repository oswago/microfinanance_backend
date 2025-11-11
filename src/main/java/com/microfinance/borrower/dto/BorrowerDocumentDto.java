package com.microfinance.borrower.dto;

import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.common.config.DocumentConfig;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BorrowerDocumentDto {
    private Long id;
    private Long borrowerId;
    private String documentType;
    private String documentName;
    private String description;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private DocumentConfig.DocumentStatus status;
    private LocalDateTime verifiedAt;
    private Long verifiedBy;
    private String verificationNotes;
    private LocalDate expiryDate;
    private String borrowerName;
    private LocalDateTime createdAt;
    private String fileName;
    private String verifiedByName; // NEW FIELD
}