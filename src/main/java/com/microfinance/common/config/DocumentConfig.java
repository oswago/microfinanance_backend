// src/main/java/com/microfinance/common/config/DocumentConfig.java
package com.microfinance.common.config;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DocumentConfig {

    public enum DocumentType {
        // Identity Documents
        NATIONAL_ID("National ID Card", "IDENTITY", true, "ID_CARD"),
        PASSPORT("Passport", "IDENTITY", true, "PASSPORT"),
        DRIVERS_LICENSE("Driver's License", "IDENTITY", false, "LICENSE"),
        VOTERS_ID("Voter's ID", "IDENTITY", false, "ID_CARD"),

        // Address Proof
        UTILITY_BILL("Utility Bill", "ADDRESS", true, "BILL"),
        RENTAL_AGREEMENT("Rental Agreement", "ADDRESS", false, "AGREEMENT"),
        LEASE_AGREEMENT("Lease Agreement", "ADDRESS", false, "AGREEMENT"),

        // Income Proof
        PAYSLIP("Payslip", "INCOME", true, "SALARY"),
        BANK_STATEMENT("Bank Statement", "INCOME", true, "STATEMENT"),
        TAX_RETURN("Tax Return", "INCOME", false, "TAX"),
        BUSINESS_REGISTRATION("Business Registration", "INCOME", false, "CERTIFICATE"),

        // Employment
        EMPLOYMENT_LETTER("Employment Letter", "EMPLOYMENT", true, "LETTER"),
        CONTRACT("Employment Contract", "EMPLOYMENT", false, "CONTRACT"),

        // Personal
        PHOTOGRAPH("Photograph", "PERSONAL", true, "PHOTO"),
        SIGNATURE_CARD("Signature Card", "PERSONAL", true, "SIGNATURE"),

        // Business Documents
        BUSINESS_LICENSE("Business License", "BUSINESS", false, "LICENSE"),
        PERMIT("Business Permit", "BUSINESS", false, "PERMIT");

        @Getter
        private final String displayName;
        @Getter
        private final String category;
        @Getter
        private final boolean required;
        @Getter
        private final String icon;

        DocumentType(String displayName, String category, boolean required, String icon) {
            this.displayName = displayName;
            this.category = category;
            this.required = required;
            this.icon = icon;
        }
    }

    public enum DocumentStatus {
        PENDING, VERIFIED, REJECTED, EXPIRED
    }

    // Use Case Definitions
    public enum DocumentUseCase {
        // KYC Use Cases
        BASIC_KYC("Basic KYC", Set.of(
            DocumentType.NATIONAL_ID,
            DocumentType.PHOTOGRAPH,
            DocumentType.UTILITY_BILL
        )),

        FULL_KYC("Full KYC", Set.of(
            DocumentType.NATIONAL_ID,
            DocumentType.PASSPORT,
            DocumentType.UTILITY_BILL,
            DocumentType.PAYSLIP,
            DocumentType.BANK_STATEMENT,
            DocumentType.PHOTOGRAPH,
            DocumentType.SIGNATURE_CARD
        )),

        BUSINESS_KYC("Business KYC", Set.of(
            DocumentType.BUSINESS_REGISTRATION,
            DocumentType.BUSINESS_LICENSE,
            DocumentType.PASSPORT,
            DocumentType.UTILITY_BILL,
            DocumentType.BANK_STATEMENT
        )),

        // Loan Application Use Cases
        SMALL_LOAN("Small Loan Application", Set.of(
            DocumentType.NATIONAL_ID,
            DocumentType.UTILITY_BILL,
            DocumentType.PAYSLIP
        )),

        LARGE_LOAN("Large Loan Application", Set.of(
            DocumentType.NATIONAL_ID,
            DocumentType.PASSPORT,
            DocumentType.UTILITY_BILL,
            DocumentType.PAYSLIP,
            DocumentType.BANK_STATEMENT,
            DocumentType.TAX_RETURN
        )),

        BUSINESS_LOAN("Business Loan Application", Set.of(
            DocumentType.BUSINESS_REGISTRATION,
            DocumentType.BUSINESS_LICENSE,
            DocumentType.NATIONAL_ID,
            DocumentType.BANK_STATEMENT,
            DocumentType.TAX_RETURN
        ));

        @Getter
        private final String displayName;
        @Getter
        private final Set<DocumentType> requiredDocuments;

        DocumentUseCase(String displayName, Set<DocumentType> requiredDocuments) {
            this.displayName = displayName;
            this.requiredDocuments = requiredDocuments;
        }
    }

    // Enhanced DocumentUtils with Use Case Support
    public static class DocumentUtils {
        
        // Existing utility methods
        public static boolean isRequiredDocument(DocumentType documentType) {
            return documentType.isRequired();
        }

        public static DocumentType[] getRequiredDocumentTypes() {
            return Arrays.stream(DocumentType.values())
                    .filter(DocumentType::isRequired)
                    .toArray(DocumentType[]::new);
        }

        public static DocumentType[] getDocumentTypesByCategory(String category) {
            return Arrays.stream(DocumentType.values())
                    .filter(type -> type.getCategory().equals(category))
                    .toArray(DocumentType[]::new);
        }

        // New Use Case Methods
        public static Set<DocumentType> getKYCRequiredDocuments() {
            return DocumentUseCase.FULL_KYC.getRequiredDocuments();
        }

        public static Set<DocumentType> getRequiredDocumentsForUseCase(DocumentUseCase useCase) {
            return useCase.getRequiredDocuments();
        }

        public static Set<DocumentType> getRequiredDocumentsForUseCase(String useCaseName) {
            return DocumentUseCase.valueOf(useCaseName).getRequiredDocuments();
        }

        public static List<DocumentUseCase> getAvailableUseCases() {
            return Arrays.asList(DocumentUseCase.values());
        }

        public static DocumentUseCase getUseCaseByName(String name) {
            return DocumentUseCase.valueOf(name);
        }

        // Validation methods
        public static boolean hasAllRequiredDocuments(Set<DocumentType> uploadedDocuments, DocumentUseCase useCase) {
            return uploadedDocuments.containsAll(useCase.getRequiredDocuments());
        }

        public static Set<DocumentType> getMissingDocuments(Set<DocumentType> uploadedDocuments, DocumentUseCase useCase) {
            Set<DocumentType> required = useCase.getRequiredDocuments();
            return required.stream()
                    .filter(doc -> !uploadedDocuments.contains(doc))
                    .collect(Collectors.toSet());
        }

        public static double calculateDocumentCompletion(Set<DocumentType> uploadedDocuments, DocumentUseCase useCase) {
            Set<DocumentType> required = useCase.getRequiredDocuments();
            long uploadedRequired = uploadedDocuments.stream()
                    .filter(required::contains)
                    .count();
            return (double) uploadedRequired / required.size() * 100;
        }

        // Specific use case shortcuts
        public static Set<DocumentType> getBasicKycDocuments() {
            return DocumentUseCase.BASIC_KYC.getRequiredDocuments();
        }

        public static Set<DocumentType> getFullKycDocuments() {
            return DocumentUseCase.FULL_KYC.getRequiredDocuments();
        }

        public static Set<DocumentType> getBusinessKycDocuments() {
            return DocumentUseCase.BUSINESS_KYC.getRequiredDocuments();
        }

        public static Set<DocumentType> getSmallLoanDocuments() {
            return DocumentUseCase.SMALL_LOAN.getRequiredDocuments();
        }

        public static Set<DocumentType> getLargeLoanDocuments() {
            return DocumentUseCase.LARGE_LOAN.getRequiredDocuments();
        }
    }
}