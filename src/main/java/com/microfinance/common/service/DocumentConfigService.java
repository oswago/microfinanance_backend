// src/main/java/com/microfinance/common/service/DocumentConfigService.java
package com.microfinance.common.service;

import com.microfinance.common.config.DocumentConfig;
import com.microfinance.common.dto.DocumentStatusDto;
import com.microfinance.common.dto.DocumentTypeDto;
import com.microfinance.common.dto.DocumentUseCaseDto;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DocumentConfigService {

    public List<DocumentTypeDto> getAllDocumentTypes() {
        return Arrays.stream(DocumentConfig.DocumentType.values())
                .map(documentType -> DocumentTypeDto.fromEntity(documentType))
                .collect(Collectors.toList());
    }

    public List<DocumentTypeDto> getRequiredDocumentTypes() {
        return Arrays.stream(DocumentConfig.DocumentType.values())
                .filter(DocumentConfig.DocumentType::isRequired)
                .map(documentType -> DocumentTypeDto.fromEntity(documentType))
                .collect(Collectors.toList());
    }

    public List<DocumentTypeDto> getDocumentTypesByCategory(String category) {
        return Arrays.stream(DocumentConfig.DocumentType.values())
                .filter(type -> type.getCategory().equalsIgnoreCase(category))
                .map(documentType -> DocumentTypeDto.fromEntity(documentType))
                .collect(Collectors.toList());
    }

    public DocumentConfig.DocumentStatus getDocumentStatus(String status) {
        return DocumentConfig.DocumentStatus.valueOf(status);
    }

    public List<DocumentStatusDto> getAllDocumentStatuses() {
        return Arrays.stream(DocumentConfig.DocumentStatus.values())
                .map(DocumentStatusDto::fromEntity)
                .collect(Collectors.toList());
    }

    // New methods for use cases
    public List<DocumentUseCaseDto> getAllUseCases() {
        return Arrays.stream(DocumentConfig.DocumentUseCase.values())
                .map(useCase -> DocumentUseCaseDto.fromEntity(useCase))
                .collect(Collectors.toList());
    }

    public Set<DocumentConfig.DocumentType> getRequiredDocumentsForUseCase(String useCaseName) {
        return DocumentConfig.DocumentUtils.getRequiredDocumentsForUseCase(useCaseName);
    }

    public Set<DocumentConfig.DocumentType> getKYCRequiredDocuments() {
        return DocumentConfig.DocumentUtils.getKYCRequiredDocuments();
    }

    public Set<DocumentConfig.DocumentType> getFullKycDocuments() {
        return DocumentConfig.DocumentUtils.getFullKycDocuments();
    }

    public Set<DocumentConfig.DocumentType> getBasicKycDocuments() {
        return DocumentConfig.DocumentUtils.getBasicKycDocuments();
    }

    public boolean validateDocumentsForUseCase(Set<DocumentConfig.DocumentType> uploadedDocuments, String useCaseName) {
        DocumentConfig.DocumentUseCase useCase = DocumentConfig.DocumentUseCase.valueOf(useCaseName);
        return DocumentConfig.DocumentUtils.hasAllRequiredDocuments(uploadedDocuments, useCase);
    }

    public Set<DocumentConfig.DocumentType> getMissingDocuments(Set<DocumentConfig.DocumentType> uploadedDocuments, String useCaseName) {
        DocumentConfig.DocumentUseCase useCase = DocumentConfig.DocumentUseCase.valueOf(useCaseName);
        return DocumentConfig.DocumentUtils.getMissingDocuments(uploadedDocuments, useCase);
    }

    public double calculateCompletionPercentage(Set<DocumentConfig.DocumentType> uploadedDocuments, String useCaseName) {
        DocumentConfig.DocumentUseCase useCase = DocumentConfig.DocumentUseCase.valueOf(useCaseName);
        return DocumentConfig.DocumentUtils.calculateDocumentCompletion(uploadedDocuments, useCase);
    }
}