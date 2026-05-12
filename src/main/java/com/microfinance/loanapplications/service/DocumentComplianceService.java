package com.microfinance.loanapplications.service;

import com.microfinance.borrower.dto.BorrowerDocumentDto;
import com.microfinance.borrower.dto.BorrowerKycSummaryDto;
import com.microfinance.borrower.service.BorrowerService;
import com.microfinance.common.config.DocumentConfig;
import com.microfinance.loanapplications.dto.BorrowerDocumentReferenceDto;
import com.microfinance.loanapplications.dto.DocumentComplianceSummary;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.loanproducts.service.LoanProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentComplianceService {
    
    private final BorrowerService borrowerService;
    private final LoanProductService loanProductService;
    
    /**
     * Check if borrower documents meet loan product requirements
     */
    public DocumentComplianceSummary checkDocumentCompliance(Long borrowerId, Long loanProductId) {
        log.info("================Checking document compliance for borrower: {}, product: {}", borrowerId, loanProductId);
        // Get borrower documents and KYC summary
        List<BorrowerDocumentDto> borrowerDocuments = borrowerService.getBorrowerDocuments(borrowerId);
        log.info("===============Borrower Documents: {}", borrowerDocuments);

        // Get loan product document requirements
        LoanProduct loanProduct = loanProductService.getLoanProductById(loanProductId);
        log.info("================Loan Product: {}", loanProductId);

        BorrowerKycSummaryDto kycSummary = borrowerService.getBorrowerKycSummary(borrowerId);
        // Parse and count the documents
        int requiredDocCount = countDocumentsFromString(loanProduct.getRequiredDocuments());
        kycSummary.setDocumentsRequired(requiredDocCount);

        // Handle null required documents
        String requiredDocs = loanProduct.getRequiredDocuments();
        List<String> requiredDocumentTypes;

        if (requiredDocs != null && !requiredDocs.trim().isEmpty()) {
            // Assuming requiredDocuments is a comma-separated string
            requiredDocumentTypes = Arrays.asList(requiredDocs.split(","));
            kycSummary.setDocumentsRequiredType(requiredDocumentTypes);
        } else {
            requiredDocumentTypes = new ArrayList<>();
        }

        log.info("===============Borrower kycSummary: {}", kycSummary);

        return analyzeCompliance(borrowerDocuments, kycSummary, requiredDocumentTypes);
    }


    private int countDocumentsFromString(String documentsString) {
        if (documentsString == null || documentsString.trim().isEmpty()) {
            return 0;
        }
        // Split by comma and count non-empty entries
        String[] documents = documentsString.split(",");
        int count = 0;
        for (String doc : documents) {
            if (doc != null && !doc.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }


    private DocumentComplianceSummary analyzeCompliance(
            List<BorrowerDocumentDto> borrowerDocuments,
            BorrowerKycSummaryDto kycSummary,
            List<String> requiredDocumentTypes) {

        // Convert required document strings to DocumentType enum
        Set<DocumentConfig.DocumentType> requiredDocTypes = requiredDocumentTypes.stream()
                .map(String::trim)
                .map(this::parseDocumentType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Group borrower documents by DocumentType
        Map<DocumentConfig.DocumentType, List<BorrowerDocumentDto>> documentsByType = borrowerDocuments.stream()
                .collect(Collectors.groupingBy(doc -> parseDocumentType(doc.getDocumentType())));

        // Remove null entries
        documentsByType.keySet().removeIf(Objects::isNull);

        List<String> missingDocumentTypes = new ArrayList<>();
        List<String> pendingVerificationTypes = new ArrayList<>();
        List<String> expiredDocumentTypes = new ArrayList<>();
        List<DocumentComplianceSummary.RequiredDocumentDetail> requiredDocumentDetails = new ArrayList<>();
        int verifiedCount = 0;

        for (DocumentConfig.DocumentType requiredType : requiredDocTypes) {
            List<BorrowerDocumentDto> documentsOfType = documentsByType.get(requiredType);
            String typeName = requiredType.name();
            String displayName = getDocumentDisplayName(requiredType);

            if (documentsOfType == null || documentsOfType.isEmpty()) {
                missingDocumentTypes.add(typeName);
                requiredDocumentDetails.add(createRequiredDocumentDetail(
                        typeName, displayName, "MISSING", true
                ));
                continue;
            }

            // Find the best document of this type
            BorrowerDocumentDto bestDocument = findBestDocument(documentsOfType);

            if (bestDocument != null &&
                    DocumentConfig.DocumentStatus.VERIFIED.equals(bestDocument.getStatus()) &&
                    (bestDocument.getExpiryDate() == null ||
                            !bestDocument.getExpiryDate().isBefore(java.time.LocalDate.now()))) {

                verifiedCount++;
                requiredDocumentDetails.add(createRequiredDocumentDetail(
                        typeName, displayName, "VERIFIED", true,
                        bestDocument.getId(),
                        bestDocument.getCreatedAt(),
                        bestDocument.getVerifiedAt(),
                        bestDocument.getExpiryDate()
                ));

            } else {
                // Check why it's not valid
                if (bestDocument != null && DocumentConfig.DocumentStatus.PENDING.equals(bestDocument.getStatus())) {
                    pendingVerificationTypes.add(typeName);
                    requiredDocumentDetails.add(createRequiredDocumentDetail(
                            typeName, displayName, "PENDING", true,
                            bestDocument.getId(),
                            bestDocument.getCreatedAt(),
                            null,
                            bestDocument.getExpiryDate()
                    ));
                } else if (bestDocument != null &&
                        DocumentConfig.DocumentStatus.VERIFIED.equals(bestDocument.getStatus()) &&
                        bestDocument.getExpiryDate() != null &&
                        bestDocument.getExpiryDate().isBefore(java.time.LocalDate.now())) {

                    expiredDocumentTypes.add(typeName);
                    requiredDocumentDetails.add(createRequiredDocumentDetail(
                            typeName, displayName, "EXPIRED", true,
                            bestDocument.getId(),
                            bestDocument.getCreatedAt(),
                            bestDocument.getVerifiedAt(),
                            bestDocument.getExpiryDate()
                    ));
                } else {
                    missingDocumentTypes.add(typeName);
                    requiredDocumentDetails.add(createRequiredDocumentDetail(
                            typeName, displayName, "MISSING", true
                    ));
                }
            }
        }

        int totalRequired = requiredDocTypes.size();
        double completionPercentage = totalRequired > 0 ? (verifiedCount * 100.0 / totalRequired) : 0.0;
        boolean meetsRequirements = verifiedCount >= totalRequired &&
                expiredDocumentTypes.isEmpty() &&
                missingDocumentTypes.isEmpty();

        return DocumentComplianceSummary.builder()
                .totalRequiredDocuments(totalRequired)
                .verifiedDocuments(verifiedCount)
                .pendingDocuments(pendingVerificationTypes.size())
                .expiredDocuments(expiredDocumentTypes.size())
                .missingDocuments(missingDocumentTypes.size())
                .meetsRequirements(meetsRequirements)
                .completionPercentage(completionPercentage)
                .missingDocumentTypes(missingDocumentTypes)
                .pendingVerificationTypes(pendingVerificationTypes)
                .expiredDocumentTypes(expiredDocumentTypes)
                .requiredDocuments(requiredDocumentDetails)
                .overallStatus(calculateOverallStatus(meetsRequirements, completionPercentage))
                .recommendation(generateRecommendation(
                        missingDocumentTypes, pendingVerificationTypes, expiredDocumentTypes))
                .build();
    }

    // Helper methods
    private DocumentComplianceSummary.RequiredDocumentDetail createRequiredDocumentDetail(
            String type, String name, String status, Boolean required,
            Long documentId, java.time.LocalDateTime uploadedDate,
            java.time.LocalDateTime verifiedDate, java.time.LocalDate expiryDate) {

        return DocumentComplianceSummary.RequiredDocumentDetail.builder()
                .type(type)
                .name(name)
                .status(status)
                .required(required)
                .documentId(documentId != null ? documentId.toString() : null)
                .uploadedDate(uploadedDate != null ? uploadedDate.toString() : null)
                .verifiedDate(verifiedDate != null ? verifiedDate.toString() : null)
                .expiryDate(expiryDate != null ? expiryDate.toString() : null)
                .build();
    }

    private DocumentComplianceSummary.RequiredDocumentDetail createRequiredDocumentDetail(
            String type, String name, String status, Boolean required) {
        return createRequiredDocumentDetail(type, name, status, required, null, null, null, null);
    }

    private String getDocumentDisplayName(DocumentConfig.DocumentType docType) {
        // Convert enum to display name
        return Arrays.stream(docType.name().split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private String calculateOverallStatus(boolean meetsRequirements, double completionPercentage) {
        if (meetsRequirements) {
            return "COMPLIANT";
        } else if (completionPercentage > 0) {
            return "PARTIALLY_COMPLIANT";
        } else {
            return "NON_COMPLIANT";
        }
    }

    private String generateRecommendation(List<String> missing, List<String> pending, List<String> expired) {
        if (!missing.isEmpty()) {
            return "Upload missing documents: " + String.join(", ", missing);
        } else if (!expired.isEmpty()) {
            return "Update expired documents: " + String.join(", ", expired);
        } else if (!pending.isEmpty()) {
            return "Documents pending verification: " + String.join(", ", pending);
        } else {
            return "All documents are compliant";
        }
    }


    private BorrowerDocumentDto findBestDocument(List<BorrowerDocumentDto> documents) {
        if (documents == null || documents.isEmpty()) {
            return null;
        }

        // Sort documents by priority:
        // 1. VERIFIED + not expired
        // 2. VERIFIED + expired
        // 3. PENDING
        // 4. REJECTED
        // 5. Latest upload date as tie-breaker

        return documents.stream()
                .sorted(Comparator
                        // First: sort by status priority
                        .comparing((BorrowerDocumentDto doc) ->
                                getStatusPriority(doc.getStatus()))
                        // Second: sort by expiration (non-expired first)
                        .thenComparing(doc ->
                                isExpired(doc) ? 1 : 0)
                        // Third: sort by upload date (newest first)
                        .thenComparing(BorrowerDocumentDto::getCreatedAt, Comparator.reverseOrder()))
                .findFirst()
                .orElse(null);
    }

    private int getStatusPriority(DocumentConfig.DocumentStatus status) {
        if (status == null) return 999;

        switch (status) {
            case VERIFIED: return 1;    // Highest priority
            case PENDING: return 2;     // Medium priority
            case REJECTED: return 3;    // Lowest priority
            default: return 999;
        }
    }

    private boolean isExpired(BorrowerDocumentDto doc) {
        return doc.getExpiryDate() != null &&
                doc.getExpiryDate().isBefore(java.time.LocalDate.now());
    }



    private DocumentConfig.DocumentType parseDocumentType(String typeName) {
        if (typeName == null) return null;

        // Normalize the string first
        String normalized = typeName.trim()
                .toUpperCase()
                .replace(" ", "_")
                .replace("-", "_");

        try {
            return DocumentConfig.DocumentType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try to map common variations
            if (normalized.contains("NATIONAL_ID") || normalized.contains("ID_CARD") || normalized.contains("IDCARD")) {
                return DocumentConfig.DocumentType.NATIONAL_ID;
            } else if (normalized.contains("PASSPORT")) {
                return DocumentConfig.DocumentType.PASSPORT;
            } else if (normalized.contains("DRIVING_LICENSE") || normalized.contains("DRIVINGLICENSE")) {
                return DocumentConfig.DocumentType.DRIVERS_LICENSE;
            }
            log.warn("Could not parse document type: {}", typeName);
            return null;
        }
    }

    private DocumentComplianceSummary analyzeComplianceORG(
            List<BorrowerDocumentDto> borrowerDocuments,
            BorrowerKycSummaryDto kycSummary,
            List<String> requiredDocumentTypes) {
        
        // Group documents by type for easy lookup
        Map<String, List<BorrowerDocumentDto>> documentsByType = borrowerDocuments.stream()
                .collect(Collectors.groupingBy(BorrowerDocumentDto::getDocumentType));
        
        List<String> missingDocumentTypes = new ArrayList<>();
        List<String> pendingVerificationTypes = new ArrayList<>();
        List<String> expiredDocumentTypes = new ArrayList<>();
        int verifiedCount = 0;
        
        for (String requiredType : requiredDocumentTypes) {
            List<BorrowerDocumentDto> documentsOfType = documentsByType.get(requiredType);
            
            if (documentsOfType == null || documentsOfType.isEmpty()) {
                missingDocumentTypes.add(requiredType);
                continue;
            }
            
            // Check if any document of this type is verified and not expired
            boolean hasValidDocument = documentsOfType.stream()
                    .anyMatch(doc -> 
                        DocumentConfig.DocumentStatus.VERIFIED.equals(doc.getStatus()) &&
                        (doc.getExpiryDate() == null || !doc.getExpiryDate().isBefore(java.time.LocalDate.now()))
                    );
            
            if (hasValidDocument) {
                verifiedCount++;
            } else {
                // Check why it's not valid
                boolean hasPendingDocument = documentsOfType.stream()
                        .anyMatch(doc -> DocumentConfig.DocumentStatus.PENDING.equals(doc.getStatus()));
                boolean hasExpiredDocument = documentsOfType.stream()
                        .anyMatch(doc -> 
                            DocumentConfig.DocumentStatus.VERIFIED.equals(doc.getStatus()) &&
                            doc.getExpiryDate() != null && doc.getExpiryDate().isBefore(java.time.LocalDate.now())
                        );
                
                if (hasPendingDocument) {
                    pendingVerificationTypes.add(requiredType);
                } else if (hasExpiredDocument) {
                    expiredDocumentTypes.add(requiredType);
                } else {
                    missingDocumentTypes.add(requiredType);
                }
            }
        }

        // In your DocumentComplianceService - FIX the return statement
        return DocumentComplianceSummary.builder()
                .totalRequiredDocuments(requiredDocumentTypes.size())
                .verifiedDocuments(verifiedCount)
                .pendingDocuments(pendingVerificationTypes.size())
                .expiredDocuments(expiredDocumentTypes.size())
                .missingDocuments(missingDocumentTypes.size())
                .missingDocumentTypes(missingDocumentTypes != null ? missingDocumentTypes : new ArrayList<>())
                .pendingVerificationTypes(pendingVerificationTypes != null ? pendingVerificationTypes : new ArrayList<>())
                .expiredDocumentTypes(expiredDocumentTypes != null ? expiredDocumentTypes : new ArrayList<>())
                .meetsRequirements(missingDocumentTypes.isEmpty() &&
                        pendingVerificationTypes.isEmpty() &&
                        expiredDocumentTypes.isEmpty())
                .completionPercentage(calculateCompletionPercentage(verifiedCount, requiredDocumentTypes.size()))
                .build();
    }
    
    private Double calculateCompletionPercentage(int verifiedCount, int totalRequired) {
        if (totalRequired == 0) return 100.0;
        return (verifiedCount * 100.0) / totalRequired;
    }
    
    /**
     * Convert BorrowerDocumentDto to BorrowerDocumentReferenceDto
     */
    public List<BorrowerDocumentReferenceDto> convertToReferenceDtos(List<BorrowerDocumentDto> borrowerDocuments) {
        return borrowerDocuments.stream()
                .map(this::convertToReferenceDto)
                .collect(Collectors.toList());
    }


    private BorrowerDocumentReferenceDto convertToReferenceDto(BorrowerDocumentDto borrowerDoc) {
        return BorrowerDocumentReferenceDto.builder()
                .documentId(borrowerDoc.getId())
                .documentType(borrowerDoc.getDocumentType())
                .documentName(borrowerDoc.getDocumentName())
                .description(borrowerDoc.getDescription())
                .fileName(borrowerDoc.getFileName())
                .filePath(borrowerDoc.getFilePath())
                .status(borrowerDoc.getStatus())
                .verifiedAt(borrowerDoc.getVerifiedAt())
                .verifiedByName(borrowerDoc.getVerifiedByName())
                .expiryDate(borrowerDoc.getExpiryDate())
                .createdAt(borrowerDoc.getCreatedAt())
                .build();
    }
}