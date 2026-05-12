package com.microfinance.loanapplications.dto;

import com.microfinance.common.config.DocumentConfig;
import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reference to existing borrower documents for loan application context
 */
@Data
@Builder
public class BorrowerDocumentReferenceDto {
    private Long documentId;
    private String documentType;
    private String documentName;
    private String description;
    private String fileName;
    private String filePath;
    private DocumentConfig.DocumentStatus status;
    private LocalDateTime verifiedAt;
    private String verifiedByName;
    private LocalDate expiryDate;
    private LocalDateTime createdAt;
    
    // Helper methods for loan application context
    public boolean isVerified() {
        return DocumentConfig.DocumentStatus.VERIFIED.equals(status);
    }
    
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }
    
    public boolean isValidForLoanApplication() {
        return isVerified() && !isExpired();
    }
    
    public String getStatusColor() {
        if (status == null) return "secondary";
        switch (status) {
            case VERIFIED: return "success";
            case REJECTED: return "danger";
            case PENDING: return "warning";
            case EXPIRED: return "info";
            default: return "secondary";
        }
    }
}

