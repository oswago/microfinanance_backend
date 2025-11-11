package com.microfinance.common.dto;

import com.microfinance.common.config.DocumentConfig;
import lombok.Data;

@Data
public class DocumentTypeDto {
    private String code;
    private String displayName;
    private String category;
    private boolean required;
    private String icon;

    // Make sure this accepts the correct type
    public static DocumentTypeDto fromEntity(DocumentConfig.DocumentType documentType) {
        DocumentTypeDto dto = new DocumentTypeDto();
        dto.setCode(documentType.name());
        dto.setDisplayName(documentType.getDisplayName());
        dto.setCategory(documentType.getCategory());
        dto.setRequired(documentType.isRequired());
        dto.setIcon(documentType.getIcon());
        return dto;
    }
}