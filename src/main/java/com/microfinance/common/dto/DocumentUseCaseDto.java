// src/main/java/com/microfinance/common/dto/DocumentUseCaseDto.java
package com.microfinance.common.dto;

import com.microfinance.common.config.DocumentConfig;
import lombok.Data;

import java.util.Set;
import java.util.stream.Collectors;

@Data
public class DocumentUseCaseDto {
    private String name;
    private String displayName;
    private Set<DocumentTypeDto> requiredDocuments;

    public static DocumentUseCaseDto fromEntity(DocumentConfig.DocumentUseCase useCase) {
        DocumentUseCaseDto dto = new DocumentUseCaseDto();
        dto.setName(useCase.name());
        dto.setDisplayName(useCase.getDisplayName());
        dto.setRequiredDocuments(useCase.getRequiredDocuments().stream()
                .map(DocumentTypeDto::fromEntity)
                .collect(Collectors.toSet()));
        return dto;
    }
}