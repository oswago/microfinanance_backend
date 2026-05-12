package com.microfinance.common.util;

import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.repository.BorrowerRepository;
import com.microfinance.common.config.DocumentConfig;
import com.microfinance.common.service.DocumentConfigService;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.loanproducts.repository.LoanProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class CommonUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    // ============ STATIC METHODS ============

    public static Set<DocumentConfig.DocumentType> getRequiredDocumentsForBorrower(Long borrowerId, String useCaseName) {
        // Get beans from application context
        BorrowerRepository borrowerRepository = applicationContext.getBean(BorrowerRepository.class);
        LoanProductRepository loanProductRepository = applicationContext.getBean(LoanProductRepository.class);
        DocumentConfigService documentConfigService = applicationContext.getBean(DocumentConfigService.class);

        Set<DocumentConfig.DocumentType> documentTypes = null;

        // Check if borrower has a loan product with required documents
        if (borrowerId != null) {
            Optional<Borrower> borrowerOpt = borrowerRepository.findById(borrowerId);

            if (borrowerOpt.isPresent()) {
                Borrower borrower = borrowerOpt.get();

                // Check if borrower has a loan product
                if (borrower.getLoanProduct() != null && borrower.getLoanProduct().getId() != null) {
                    Optional<LoanProduct> loanProductOpt = loanProductRepository.findById(borrower.getLoanProduct().getId());

                    if (loanProductOpt.isPresent()) {
                        LoanProduct loanProduct = loanProductOpt.get();
                        String requiredDocsJson = loanProduct.getRequiredDocuments();

                        if (requiredDocsJson != null && !requiredDocsJson.trim().isEmpty()) {
                            try {
                                documentTypes = parseRequiredDocuments(requiredDocsJson);
                                log.info("Using loan product's required documents for borrower {}: {}",
                                        borrowerId, requiredDocsJson);
                            } catch (Exception e) {
                                log.warn("Failed to parse required documents from loan product for borrower {}. " +
                                        "Falling back to use case defaults.", borrowerId, e);
                            }
                        }
                    }
                } else {
                    log.info("Borrower {} has no loan product assigned", borrowerId);
                }
            } else {
                log.warn("Borrower not found with ID: {}", borrowerId);
            }
        }

        // If no loan product documents or parsing failed, use default use case documents
        if (documentTypes == null || documentTypes.isEmpty()) {
            documentTypes = documentConfigService.getRequiredDocumentsForUseCase(useCaseName);
            log.info("Using default required documents for use case '{}'", useCaseName);
        }

        log.info("Final required documents for borrower {}: {}", borrowerId, documentTypes);
        return documentTypes;
    }

    public static Set<DocumentConfig.DocumentType> parseRequiredDocuments(String requiredDocsJson) {
        Set<DocumentConfig.DocumentType> documentTypes = new HashSet<>();

        try {
            String[] documentNames = requiredDocsJson.split(",");

            for (String docName : documentNames) {
                String trimmedName = docName.trim();
                if (!trimmedName.isEmpty()) {
                    try {
                        DocumentConfig.DocumentType docType = DocumentConfig.DocumentType.valueOf(
                                normalizeDocumentName(trimmedName)
                        );
                        documentTypes.add(docType);
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown document type in loan product: '{}'", trimmedName);
                        Optional<DocumentConfig.DocumentType> matchingType = findMatchingDocumentType(trimmedName);
                        matchingType.ifPresent(documentTypes::add);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse required documents: " + requiredDocsJson, e);
        }

        return documentTypes;
    }

    private static String normalizeDocumentName(String documentName) {
        return documentName.toUpperCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replace("CARD", "ID")
                .replace("BILL", "UTILITY_BILL");
    }

    private static Optional<DocumentConfig.DocumentType> findMatchingDocumentType(String documentName) {
        String normalized = normalizeDocumentName(documentName);
        for (DocumentConfig.DocumentType docType : DocumentConfig.DocumentType.values()) {
            if (docType.name().equalsIgnoreCase(normalized) ||
                    docType.getDisplayName().equalsIgnoreCase(documentName)) {
                return Optional.of(docType);
            }
        }
        return Optional.empty();
    }



}