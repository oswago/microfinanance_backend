package com.microfinance.loanapplications.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateLoanApplicationDto {

    // Client Information
    @NotNull(message = "Borrower ID is required")
    private Long borrowerId;

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    // Loan Product Information
    @NotNull(message = "Loan product ID is required")
    private Long loanProductId;

    // Loan Amount and Terms
    @NotNull(message = "Applied amount is required")
    @DecimalMin(value = "1000.0", message = "Minimum loan amount is 1000")
    @DecimalMax(value = "1000000.0", message = "Maximum loan amount is 1,000,000")
    private BigDecimal appliedAmount;

    @NotNull(message = "Tenure is required")
    @Min(value = 1, message = "Tenure must be at least 1 month")
    @Max(value = 60, message = "Maximum tenure is 60 months")
    private Integer tenureMonths;

    @NotBlank(message = "Tenure unit is required")
    @Pattern(regexp = "DAYS|WEEKS|MONTHS|YEARS", message = "Tenure unit must be DAYS, WEEKS, MONTHS, or YEARS")
    private String tenureUnit = "MONTHS";

    // Purpose and Category
    @NotBlank(message = "Loan purpose is required")
    @Size(max = 500, message = "Purpose cannot exceed 500 characters")
    private String purpose;

    @NotBlank(message = "Purpose category is required")
    @Pattern(regexp = "BUSINESS_EXPANSION|WORKING_CAPITAL|ASSET_PURCHASE|EDUCATION|MEDICAL_EXPENSES|HOME_IMPROVEMENT|DEBT_CONSOLIDATION|AGRICULTURE|EMERGENCY|OTHER",
            message = "Invalid purpose category")
    private String purposeCategory;

    // Fees
    @NotNull(message = "Processing fee is required")
    @DecimalMin(value = "0.0", message = "Processing fee cannot be negative")
    private BigDecimal processingFee;

    @NotNull(message = "Insurance fee is required")
    @DecimalMin(value = "0.0", message = "Insurance fee cannot be negative")
    private BigDecimal insuranceFee;

    // Risk Assessment (Optional during creation, can be calculated later)
    private Integer riskScore;

   // @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "Risk level must be LOW, MEDIUM, or HIGH")
    private String riskLevel;

    private Boolean recommendedForApproval;

    // Additional Information
    @Size(max = 1000, message = "Additional notes cannot exceed 1000 characters")
    private String additionalNotes;

    @Size(max = 1000, message = "Officer comments cannot exceed 1000 characters")
    private String officerComments;

    // Approval/Submission Flags
    @NotNull(message = "Submit for approval flag is required")
    private Boolean submitForApproval = false;

    // Document Compliance (Optional - can be set based on borrower documents)
    private Boolean documentRequirementsMet = false;

    // Additional metadata (Optional)
    private String revisionNotes;

    private String currentApprovalLevel;

    // Credit Information (Optional - can be pulled from borrower profile)
    private Integer creditScore;

    private String riskCategory;

    // Flags for special handling
    private Boolean flaggedForReview = false;

    @Size(max = 500, message = "Review notes cannot exceed 500 characters")
    private String reviewNotes;

    // Guarantor Information (Optional for now, can be added separately)
    private Boolean hasGuarantor = false;

    // Collateral Information (Optional)
    private Boolean hasCollateral = false;

    private String collateralDescription;

    // Application Source/Channel
    private String applicationSource = "WEB_PORTAL"; // WEB_PORTAL, MOBILE_APP, BRANCH, AGENT

    // Preferred Disbursement Method
    @Pattern(regexp = "BANK_TRANSFER|MOBILE_MONEY|CASH|CHEQUE",
            message = "Invalid disbursement method")
    private String preferredDisbursementMethod = "BANK_TRANSFER";

    // Repayment Preferences
    @Pattern(regexp = "MONTHLY|BI_WEEKLY|WEEKLY|DAILY",
            message = "Invalid repayment frequency")
    private String preferredRepaymentFrequency = "MONTHLY";

    // Loan Officer Assignment (Optional - can be auto-assigned)
    private Long assignedLoanOfficerId;

    // Terms and Conditions Acceptance
    //@AssertTrue(message = "Terms and conditions must be accepted")
    private Boolean termsAccepted = false;

    // Co-applicant Information (Optional)
    private Boolean hasCoApplicant = false;

    private String coApplicantName;

    private String coApplicantRelationship;

    private String coApplicantIdNumber;

    // Business Information (If applicable)
    private String businessName;

    private String businessRegistrationNumber;

    private String businessType;

    private Integer businessYears;

    // Employment Information
    private String employmentStatus;

    private String employerName;

    private String employmentYears;

    private BigDecimal monthlyIncome;

    // Existing Debt Information
    private BigDecimal existingDebtAmount;

    private BigDecimal monthlyDebtObligations;

    // Family Information
    private Integer dependentsCount;

    // Communication Preferences
    private Boolean sendSmsUpdates = true;

    private Boolean sendEmailUpdates = true;

    // Custom Fields (for future extensibility)
    private String customField1;

    private String customField2;

    private String customField3;

    // Security/Validation Fields
    @JsonIgnore
    private Long createdByUserId; // Will be set from authentication context

    @JsonIgnore
    private String ipAddress; // Will be set from request context

    @JsonIgnore
    private String userAgent; // Will be set from request context

    // Helper methods
    public BigDecimal getTotalFees() {
        if (processingFee == null || insuranceFee == null) {
            return BigDecimal.ZERO;
        }
        return processingFee.add(insuranceFee);
    }

    public BigDecimal getNetLoanAmount() {
        if (appliedAmount == null || processingFee == null || insuranceFee == null) {
            return appliedAmount != null ? appliedAmount : BigDecimal.ZERO;
        }
        return appliedAmount.subtract(getTotalFees());
    }

    public boolean isCompleteForSubmission() {
        return submitForApproval != null && submitForApproval
                && borrowerId != null
                && branchId != null
                && loanProductId != null
                && appliedAmount != null
                && tenureMonths != null
                && purpose != null
                && purposeCategory != null
                && processingFee != null
                && insuranceFee != null
                && termsAccepted != null && termsAccepted;
    }

    public void setDefaultValues() {
        if (tenureUnit == null) {
            tenureUnit = "MONTHS";
        }
        if (submitForApproval == null) {
            submitForApproval = false;
        }
        if (processingFee == null) {
            processingFee = BigDecimal.ZERO;
        }
        if (insuranceFee == null) {
            insuranceFee = BigDecimal.ZERO;
        }
        if (applicationSource == null) {
            applicationSource = "WEB_PORTAL";
        }
        if (preferredDisbursementMethod == null) {
            preferredDisbursementMethod = "BANK_TRANSFER";
        }
        if (preferredRepaymentFrequency == null) {
            preferredRepaymentFrequency = "MONTHLY";
        }
    }
}