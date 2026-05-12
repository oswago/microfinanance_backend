package com.microfinance.loanapplications.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDocumentDto {
    private Long id;
    private String documentType;
    private String documentName;
    private String filePath;
    private String fileSize;
    private String mimeType;
    private LocalDateTime uploadedAt;
    private String uploadedBy;
    private Boolean isVerified;
    private LocalDateTime verifiedAt;
    private String verifiedBy;
}