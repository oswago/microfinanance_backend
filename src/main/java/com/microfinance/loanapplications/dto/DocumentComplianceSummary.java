package com.microfinance.loanapplications.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Document compliance summary for loan application
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentComplianceSummary {
    // Basic counts
    private Integer totalRequiredDocuments;
    private Integer verifiedDocuments;
    private Integer pendingDocuments;
    private Integer expiredDocuments;
    private Integer missingDocuments;

    // Overall status
    private Boolean meetsRequirements;
    private Double completionPercentage;

    // Detailed lists
    private List<String> missingDocumentTypes;
    private List<String> pendingVerificationTypes;
    private List<String> expiredDocumentTypes;

    // NEW FIELDS to match frontend
    private List<RequiredDocumentDetail> requiredDocuments;
    private String errorMessage;

    // Optional: Add overall status summary
    private String overallStatus; // e.g., "COMPLIANT", "NON_COMPLIANT", "PARTIALLY_COMPLIANT"
    private String recommendation; // e.g., "Upload missing documents", "Submit for verification"

    private int compliancePercentage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequiredDocumentDetail {
        private String type; // Document type (e.g., "NATIONAL_ID")
        private String name; // Display name (e.g., "National ID Card")
        private String status; // "MISSING", "PENDING", "VERIFIED", "EXPIRED"
        private Boolean required;
        private String documentId; // Optional: ID if document exists
        private String uploadedDate; // Optional: when it was uploaded
        private String verifiedDate; // Optional: when it was verified
        private String expiryDate; // Optional: when it expires
    }

    // Helper method to populate requiredDocuments list
    public void populateRequiredDocuments() {
        if (this.requiredDocuments == null) {
            this.requiredDocuments = new ArrayList<>();
        }

        // Add missing documents
        if (missingDocumentTypes != null) {
            missingDocumentTypes.forEach(docType ->
                    requiredDocuments.add(RequiredDocumentDetail.builder()
                            .type(docType)
                            .name(getDisplayName(docType))
                            .status("MISSING")
                            .required(true)
                            .build())
            );
        }

        // Add pending documents
        if (pendingVerificationTypes != null) {
            pendingVerificationTypes.forEach(docType ->
                    requiredDocuments.add(RequiredDocumentDetail.builder()
                            .type(docType)
                            .name(getDisplayName(docType))
                            .status("PENDING")
                            .required(true)
                            .build())
            );
        }

        // Add expired documents
        if (expiredDocumentTypes != null) {
            expiredDocumentTypes.forEach(docType ->
                    requiredDocuments.add(RequiredDocumentDetail.builder()
                            .type(docType)
                            .name(getDisplayName(docType))
                            .status("EXPIRED")
                            .required(true)
                            .build())
            );
        }
    }

    private String getDisplayName(String docType) {
        // Convert enum-style type to display name
        return docType.replace("_", " ").toLowerCase();
    }

    public Double getCompletionPercentage() {
        if (totalRequiredDocuments == null || verifiedDocuments == null || totalRequiredDocuments == 0) {
            return 0.0;
        }
        return (verifiedDocuments.doubleValue() / totalRequiredDocuments.doubleValue()) * 100;
    }

    public Boolean getMeetsRequirements() {
        if (verifiedDocuments == null || totalRequiredDocuments == null ||
                expiredDocuments == null || missingDocuments == null) {
            return false;
        }
        return verifiedDocuments >= totalRequiredDocuments &&
                expiredDocuments == 0 &&
                missingDocuments == 0;
    }

    // Helper method to calculate overall status
    public String calculateOverallStatus() {
        if (Boolean.TRUE.equals(meetsRequirements)) {
            return "COMPLIANT";
        } else if (completionPercentage != null && completionPercentage > 0) {
            return "PARTIALLY_COMPLIANT";
        } else {
            return "NON_COMPLIANT";
        }
    }
}