package com.microfinance.borrower.enums;

public enum KycWorkflowState {
    // Initial states
    NOT_STARTED("Not Started", "KYC process has not been initiated"),
    INITIATED("Initiated", "KYC process has been started"),
    
    // Document collection states
    DOCUMENT_COLLECTION("Document Collection", "Collecting required documents"),
    DOCUMENT_UPLOAD_PENDING("Document Upload Pending", "Waiting for document uploads"),
    DOCUMENT_UPLOAD_COMPLETED("Documents Uploaded", "All required documents have been uploaded"),
    
    // Verification states
    UNDER_REVIEW("Under Review", "Documents are being reviewed by officer"),
    VERIFICATION_IN_PROGRESS("Verification in Progress", "Documents are being verified"),
    ADDITIONAL_INFO_REQUIRED("Additional Info Required", "Need more information or documents"),
    
    // Approval states
    PENDING_APPROVAL("Pending Approval", "Waiting for manager approval"),
    APPROVED("Approved", "KYC has been approved"),
    
    // Final states
    VERIFIED("Verified", "KYC verification completed successfully"),
    REJECTED("Rejected", "KYC verification failed"),
    EXPIRED("Expired", "KYC verification has expired"),
    SUSPENDED("Suspended", "KYC verification temporarily suspended");

    private final String displayName;
    private final String description;

    KycWorkflowState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminalState() {
        return this == VERIFIED || this == REJECTED || this == EXPIRED;
    }

    public boolean canTransitionTo(KycWorkflowState nextState) {
        // Define valid state transitions
        switch (this) {
            case NOT_STARTED:
                return nextState == INITIATED;
            case INITIATED:
                return nextState == DOCUMENT_COLLECTION;
            case DOCUMENT_COLLECTION:
                return nextState == DOCUMENT_UPLOAD_PENDING;
            case DOCUMENT_UPLOAD_PENDING:
                return nextState == DOCUMENT_UPLOAD_COMPLETED;
            case DOCUMENT_UPLOAD_COMPLETED:
                return nextState == UNDER_REVIEW;
            case UNDER_REVIEW:
                return nextState == VERIFICATION_IN_PROGRESS || nextState == ADDITIONAL_INFO_REQUIRED;
            case VERIFICATION_IN_PROGRESS:
                return nextState == PENDING_APPROVAL || nextState == ADDITIONAL_INFO_REQUIRED;
            case ADDITIONAL_INFO_REQUIRED:
                return nextState == DOCUMENT_UPLOAD_PENDING || nextState == UNDER_REVIEW;
            case PENDING_APPROVAL:
                return nextState == VERIFIED || nextState == REJECTED;
            case VERIFIED:
                return nextState == EXPIRED || nextState == SUSPENDED;
            case REJECTED:
                return nextState == INITIATED; // Can restart process
            case SUSPENDED:
                return nextState == VERIFIED || nextState == REJECTED;
            case EXPIRED:
                return nextState == INITIATED; // Can restart process
            default:
                return false;
        }
    }
}