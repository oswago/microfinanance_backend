package com.microfinance.loanapplications.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microfinance.common.config.DocumentConfig;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.repository.LoanRepository;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.loanproducts.repository.LoanProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class LoanProductDocumentService {
    
    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanProductRepository loanProductRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Get required document types for a borrower based on their active loan applications
     */

    public Set<DocumentConfig.DocumentType> getRequiredDocumentTypesORG(Long borrowerId) {
        // Get required documents directly using projection
       // List<String> requiredDocStrings = loanRepository.findRequiredDocumentStringsByBorrowerId(borrowerId);
        List<String> requiredDocStrings = loanRepository.findRequiredDocumentStringsByBorrowerIdNative(borrowerId);

        return requiredDocStrings.stream()
                .filter(Objects::nonNull)
                .map(this::parseDocumentTypesFromString)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }


    public Set<DocumentConfig.DocumentType> getRequiredDocumentTypesORG2(Long borrowerId) {
        List<Long> loanProductIds = loanRepository.findLoanProductIdsByBorrowerId(borrowerId);
        log.warn("getRequiredDocumentTypes 1 for borrower: {} loanProductIds: {}", borrowerId,loanProductIds);
        if (loanProductIds == null || loanProductIds.isEmpty()) {
            log.warn("No loan product found for borrower: {}", borrowerId);
            return Collections.emptySet();
        }
        Long loanProductId = loanProductIds.get(0);

        log.warn("getRequiredDocumentType 2 for borrower: {} loanProductId: {}", borrowerId,loanProductId);

        return getRequiredDocumentTypesByLoanProductId(loanProductId);
    }

    public Set<DocumentConfig.DocumentType> getRequiredDocumentTypes(Long borrowerId) {
        List<Long> loanProductIds = loanRepository.findLoanProductIdsByBorrowerId(borrowerId);
        log.warn("getRequiredDocumentTypes 1 for borrower: {} loanProductIds: {}", borrowerId, loanProductIds);

        if (loanProductIds == null || loanProductIds.isEmpty()) {
            log.warn("No loan product found for borrower: {}", borrowerId);
            return Collections.emptySet();
        }

        // Filter out null values and take the first non-null loan product ID
        Long loanProductId = loanProductIds.stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        log.warn("getRequiredDocumentType 2 for borrower: {} loanProductId: {}", borrowerId, loanProductId);

        if (loanProductId == null) {
            log.warn("No valid (non-null) loan product found for borrower: {}", borrowerId);
            // Return default required documents or empty set
            return getDefaultRequiredDocumentTypes();
        }

        return getRequiredDocumentTypesByLoanProductId(loanProductId);
    }

    // Add a method to return default required documents
    private Set<DocumentConfig.DocumentType> getDefaultRequiredDocumentTypes() {
        // Return a default set of required documents
        return Set.of(
                DocumentConfig.DocumentType.NATIONAL_ID,
                DocumentConfig.DocumentType.PHOTOGRAPH
        );
    }


    public Set<DocumentConfig.DocumentType> getRequiredDocumentTypesByLoanProductId(Long loanProductId) {
        LoanProduct loanProduct = loanProductRepository.findById(loanProductId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Loan product not found: " + loanProductId));
        return parseDocumentTypesFromString(loanProduct.getRequiredDocuments());
    }



    private Set<DocumentConfig.DocumentType> parseDocumentTypesFromString(String requiredDocs) {
        if (requiredDocs == null || requiredDocs.trim().isEmpty()) {
            return new HashSet<>();
        }
        return Arrays.stream(requiredDocs.split(","))
                .map(String::trim)
                .map(this::parseDocumentType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }


  /*
    private DocumentConfig.DocumentType parseDocumentType(String typeName) {
        try {
            return DocumentConfig.DocumentType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            //log.warn("Unknown document type: {}", typeName);
            return null;
        }
    }*/


    private DocumentConfig.DocumentType parseDocumentType(String docTypeStr) {
        if (docTypeStr == null || docTypeStr.trim().isEmpty()) {
            return null;
        }

        String trimmed = docTypeStr.trim();
        log.info("==== ParseDocumentType called with: '{}'", trimmed);

        DocumentConfig.DocumentType result = DocumentConfig.DocumentType.fromDisplayName(trimmed);
        if (result == null) {
            log.warn("Unknown document type: {}", trimmed);
        }
        return result;
    }

    
    /**
     * Parse required documents from LoanProduct JSON field
     */
    private Set<DocumentConfig.DocumentType> parseRequiredDocumentsFromProduct(LoanProduct loanProduct) {
        if (loanProduct.getRequiredDocuments() == null || loanProduct.getRequiredDocuments().trim().isEmpty()) {
            return getDefaultRequiredDocuments();
        }
        
        try {
            List<String> documentTypes = objectMapper.readValue(
                loanProduct.getRequiredDocuments(), 
                new TypeReference<List<String>>() {}
            );
            
            return documentTypes.stream()
                    .map(this::safeParseDocumentType)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
                    
        } catch (Exception e) {
            log.warn("Failed to parse required documents for loan product {}: {}", 
                    loanProduct.getId(), e.getMessage());
            return getDefaultRequiredDocuments();
        }
    }
    
    /**
     * Fallback to default required documents if parsing fails
     */
    private Set<DocumentConfig.DocumentType> getDefaultRequiredDocuments() {
        return Set.of(
            DocumentConfig.DocumentType.NATIONAL_ID,
            DocumentConfig.DocumentType.PASSPORT,
            DocumentConfig.DocumentType.UTILITY_BILL,
            DocumentConfig.DocumentType.BANK_STATEMENT
        );
    }
    
    private DocumentConfig.DocumentType safeParseDocumentType(String type) {
        try {
            return DocumentConfig.DocumentType.valueOf(type);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown document type in loan product: {}", type);
            return null;
        }
    }
}