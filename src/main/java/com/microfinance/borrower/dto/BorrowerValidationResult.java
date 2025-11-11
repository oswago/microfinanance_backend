package com.microfinance.borrower.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BorrowerValidationResult extends ValidationResult {
    private Long borrowerId;
    private String borrowerNumber;
    private String borrowerName;
    private ValidationType validationType;

    public enum ValidationType {
        CREATION,
        UPDATE,
        KYC,
        LOAN_ELIGIBILITY,
        GROUP_ASSIGNMENT,
        STATUS_CHANGE
    }

    public BorrowerValidationResult() {
        super();
    }

    public BorrowerValidationResult(ValidationType validationType) {
        super();
        this.validationType = validationType;
    }

    public static BorrowerValidationResult forBorrowerCreation() {
        return new BorrowerValidationResult(ValidationType.CREATION);
    }

    public static BorrowerValidationResult forLoanEligibility(Long borrowerId) {
        BorrowerValidationResult result = new BorrowerValidationResult(ValidationType.LOAN_ELIGIBILITY);
        result.setBorrowerId(borrowerId);
        return result;
    }

    // Specific validation methods for borrowers
    public void addKycValidationError(String error) {
        addError("KYC Validation: " + error);
    }

    public void addDocumentValidationWarning(String warning) {
        addWarning("Document Validation: " + warning);
    }

    public void addEligibilityInfo(String info) {
        addInfoMessage("Eligibility: " + info);
    }
}