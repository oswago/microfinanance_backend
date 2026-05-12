// src/main/java/com/microfinance/common/config/DocumentConfig.java
package com.microfinance.common.config;

import com.microfinance.borrower.enums.KycWorkflowStep;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

public class DocumentConfig {

    public enum DocumentType {
        // Identity Documents
        NATIONAL_ID("National ID Card", "IDENTITY", true, "ID_CARD"),
        PASSPORT("Passport", "IDENTITY", true, "PASSPORT"),
        DRIVERS_LICENSE("Driver's License", "IDENTITY", false, "LICENSE"),
        VOTERS_ID("Voter's ID", "IDENTITY", false, "ID_CARD"),

        // Address Proof
        UTILITY_BILL("Utility Bill", "ADDRESS", false, "BILL"),
        RENTAL_AGREEMENT("Rental Agreement", "ADDRESS", false, "AGREEMENT"),
        LEASE_AGREEMENT("Lease Agreement", "ADDRESS", false, "AGREEMENT"),

        // Income Proof
        PAYSLIP("Payslip", "INCOME", false, "SALARY"),
        BANK_STATEMENT("Bank Statement", "INCOME", false, "STATEMENT"),
        TAX_RETURN("Tax Return", "INCOME", false, "TAX"),
        BUSINESS_REGISTRATION("Business Registration", "INCOME", false, "CERTIFICATE"),

        // Employment
        EMPLOYMENT_LETTER("Employment Letter", "EMPLOYMENT", false, "LETTER"),
        CONTRACT("Employment Contract", "EMPLOYMENT", false, "CONTRACT"),

        // Personal
        PHOTOGRAPH("Photograph", "PERSONAL", false, "PHOTO"),
        SIGNATURE_CARD("Signature Card", "PERSONAL", false, "SIGNATURE"),

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

        public static DocumentType fromDisplayName(String displayName) {
            if (displayName == null) return null;

            for (DocumentType type : DocumentType.values()) {
                // Access displayName through the instance 'type'
                if (type.displayName.equalsIgnoreCase(displayName)) {
                    return type;
                }
            }

            // Try direct enum name match as fallback
            try {
                return DocumentType.valueOf(displayName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
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
            DocumentType.PHOTOGRAPH
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

        public static Set<KycWorkflowStep> getCompulsorySteps() {
            return Set.of(
                    KycWorkflowStep.INITIATE_KYC,
                    KycWorkflowStep.RISK_ASSESSMENT,
                    KycWorkflowStep.OFFICER_APPROVAL,
                    KycWorkflowStep.MANAGER_APPROVAL,
                    KycWorkflowStep.KYC_COMPLETION
            );
        }

        public static Map<String, List<KycWorkflowStep>> getDocumentStepMap() {
            return Map.of(
                    "PASSPORT", List.of(KycWorkflowStep.UPLOAD_ID_PROOF, KycWorkflowStep.VERIFY_ID_PROOF),
                    "NATIONAL_ID", List.of(KycWorkflowStep.UPLOAD_ID_PROOF, KycWorkflowStep.VERIFY_ID_PROOF),
                    "DRIVERS_LICENSE", List.of(KycWorkflowStep.UPLOAD_ID_PROOF, KycWorkflowStep.VERIFY_ID_PROOF),
                    "UTILITY_BILL", List.of(KycWorkflowStep.UPLOAD_ADDRESS_PROOF, KycWorkflowStep.VERIFY_ADDRESS_PROOF),
                    "BANK_STATEMENT", List.of(
                            KycWorkflowStep.UPLOAD_ADDRESS_PROOF,
                            KycWorkflowStep.UPLOAD_INCOME_PROOF,
                            KycWorkflowStep.VERIFY_ADDRESS_PROOF,
                            KycWorkflowStep.VERIFY_INCOME_PROOF
                    ),
                    "RENTAL_AGREEMENT", List.of(KycWorkflowStep.UPLOAD_ADDRESS_PROOF, KycWorkflowStep.VERIFY_ADDRESS_PROOF),
                    "PAYSLIP", List.of(KycWorkflowStep.UPLOAD_INCOME_PROOF, KycWorkflowStep.VERIFY_INCOME_PROOF),
                    "TAX_RETURN", List.of(KycWorkflowStep.UPLOAD_INCOME_PROOF, KycWorkflowStep.VERIFY_INCOME_PROOF),
                    "PHOTOGRAPH", List.of(KycWorkflowStep.UPLOAD_PHOTOGRAPH, KycWorkflowStep.VERIFY_PHOTOGRAPH)

            );
        }


    }
}