package com.microfinance.borrower.enums;

public enum KycWorkflowStep {
    // Initial steps
    INITIATE_KYC("Initiate KYC", "Start the KYC verification process", 1,true),
    
    // Document collection steps
    UPLOAD_ID_PROOF("Upload ID Proof", "Upload government-issued identification", 2,true),
    UPLOAD_ADDRESS_PROOF("Upload Address Proof", "Upload proof of residence", 3,true),
    UPLOAD_INCOME_PROOF("Upload Income Proof", "Upload proof of income/employment", 4,true),
    UPLOAD_PHOTOGRAPH("Upload Photograph", "Upload recent passport-sized photo", 5,true),
    
    // Verification steps
    VERIFY_PERSONAL_INFO("Verify Personal Information", "Verify borrower's personal details", 6,true),
    VERIFY_DOCUMENTS("Verify Documents", "Validate uploaded documents", 7,true),
    VERIFY_ADDRESS("Verify Address", "Confirm residential address", 8,true),
    VERIFY_INCOME("Verify Income", "Validate income sources", 9,true),

    VERIFY_ID_PROOF("Verify identification documents","Verify identification documents",10,true),
    VERIFY_ADDRESS_PROOF( "Verify address documents","Verify address documents",11,true),
    VERIFY_INCOME_PROOF( "Verify income documents","Verify income documents",12,true),
    VERIFY_PHOTOGRAPH("Verify photograph","Verify photograph",13,true),
    // Add more as needed
    
    // Approval steps
    RISK_ASSESSMENT("Risk Assessment", "Assess borrower risk level", 14,true),
    OFFICER_APPROVAL("Officer Approval", "Loan officer review and approval", 15,true),
    MANAGER_APPROVAL("Manager Approval", "Branch manager final approval", 16,true),
    
    // Completion steps
    KYC_COMPLETION("KYC Completion", "Finalize KYC process", 17,true);

    private final String displayName;
    private final String description;
    private final int order;
    private final boolean required;

    KycWorkflowStep(String displayName, String description, int order,boolean required) {
        this.displayName = displayName;
        this.description = description;
        this.order = order;
        this.required=required;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
    public int getOrder() {
        return order;
    }
    public boolean isRequired() { return required; }

    public boolean isDocumentUploadStep() {
        return this.name().startsWith("UPLOAD_");
    }

    public boolean isVerificationStep() {
        return this.name().startsWith("VERIFY_");
    }

    public boolean isApprovalStep() {
        return this.name().endsWith("_APPROVAL");
    }
}