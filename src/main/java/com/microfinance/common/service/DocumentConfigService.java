// src/main/java/com/microfinance/common/service/DocumentConfigService.java
package com.microfinance.common.service;

import com.microfinance.borrower.enums.KycWorkflowStep;
import com.microfinance.common.config.DocumentConfig;
import com.microfinance.common.dto.DocumentStatusDto;
import com.microfinance.common.dto.DocumentTypeDto;
import com.microfinance.common.dto.DocumentUseCaseDto;
import com.microfinance.common.util.CommonUtil;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.loanproducts.repository.LoanProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentConfigService {

    private static final Logger log = LoggerFactory.getLogger(DocumentConfigService.class);
    private final LoanProductRepository loanProductRepository;


    public DocumentConfigService(LoanProductRepository loanProductRepository) {
        this.loanProductRepository = loanProductRepository;
    }

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

    public Map<String, List<KycWorkflowStep>> getDocumentStepMap() {
        return DocumentConfig.DocumentUtils.getDocumentStepMap();
    }

   public  Set<KycWorkflowStep> getCompulsorySteps(){
        return DocumentConfig.DocumentUtils.getCompulsorySteps();
    }


    public Map<String, List<KycWorkflowStep>> getDocumentStepMapForUseCase(String useCaseName, Long borrowerId) {
        log.info("Generating document-step mapping for use case: {}", useCaseName);
        try {
            // Get required documents for the use case
           // Set<DocumentConfig.DocumentType> requiredDocuments = getRequiredDocumentsForUseCase(useCaseName);

            Set<DocumentConfig.DocumentType> requiredDocuments = null;
            // Check if borrower has a loan product with required documents
            if (borrowerId != null) {
                Optional<LoanProduct> loanProductOpt = loanProductRepository.findById(borrowerId);

                if (loanProductOpt.isPresent()) {
                    LoanProduct loanProduct = loanProductOpt.get();
                    String requiredDocsJson = loanProduct.getRequiredDocuments();

                    if (requiredDocsJson != null && !requiredDocsJson.trim().isEmpty()) {
                        try {
                            // Parse the JSON string to get required documents
                            requiredDocuments = CommonUtil.parseRequiredDocuments(requiredDocsJson);
                            log.info("Using loan product's required documents: {}", requiredDocsJson);
                        } catch (Exception e) {
                            log.warn("Failed to parse required documents from loan product. Falling back to use case defaults.", e);
                        }
                    }
                }
            }

            // If no loan product documents or parsing failed, use default use case documents
            if (requiredDocuments == null || requiredDocuments.isEmpty()) {
                requiredDocuments = getRequiredDocumentsForUseCase(useCaseName);
                log.info("Using default required documents for use case '{}'", useCaseName);
            }

            if (requiredDocuments == null || requiredDocuments.isEmpty()) {
                log.warn("No required documents found for use case: {}", useCaseName);
                return Collections.emptyMap();
            }

            log.info("Required documents for '{}': {}", useCaseName, requiredDocuments);
            // Convert to document type strings
            Set<String> requiredDocumentTypes = requiredDocuments.stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            // Get the full document-step mapping
            Map<String, List<KycWorkflowStep>> fullDocumentStepMap = getDocumentStepMap();

            // Filter to only include required documents for this use case
            Map<String, List<KycWorkflowStep>> filteredMap = fullDocumentStepMap.entrySet().stream()
                    .filter(entry -> requiredDocumentTypes.contains(entry.getKey()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));

            log.info("Generated document-step mapping for '{}': {} documents with {} total steps",
                    useCaseName, filteredMap.size(),
                    filteredMap.values().stream().mapToInt(List::size).sum());

            // Log detailed mapping
            filteredMap.forEach((docType, steps) ->
                    log.debug("Document '{}' maps to steps: {}", docType, steps));

            return filteredMap;

        } catch (Exception e) {
            log.error("Error generating document-step mapping for use case '{}': {}", useCaseName, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }


}