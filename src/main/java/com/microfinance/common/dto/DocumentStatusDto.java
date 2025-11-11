// src/main/java/com/microfinance/common/dto/DocumentStatusDto.java
package com.microfinance.common.dto;

import com.microfinance.common.config.DocumentConfig;
import lombok.Data;

@Data
public class DocumentStatusDto {
    private String code;
    private String displayName;
    private String description;

    public static DocumentStatusDto fromEntity(DocumentConfig.DocumentStatus status) {
        DocumentStatusDto dto = new DocumentStatusDto();
        dto.setCode(status.name());
        dto.setDisplayName(getDisplayName(status));
        dto.setDescription(getDescription(status));
        return dto;
    }

    private static String getDisplayName(DocumentConfig.DocumentStatus status) {
        switch (status) {
            case PENDING: return "Pending";
            case VERIFIED: return "Verified";
            case REJECTED: return "Rejected";
            case EXPIRED: return "Expired";
            default: return status.name();
        }
    }

    private static String getDescription(DocumentConfig.DocumentStatus status) {
        switch (status) {
            case PENDING: return "Document is awaiting verification";
            case VERIFIED: return "Document has been verified and approved";
            case REJECTED: return "Document has been rejected";
            case EXPIRED: return "Document has expired";
            default: return "Unknown status";
        }
    }
}