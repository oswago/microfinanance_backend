package com.microfinance.loanapplications.dto;

import com.microfinance.borrower.dto.BorrowerKycSummaryDto;
import com.microfinance.loanapplications.dto.approval.ApprovalConditionDto;
import com.microfinance.loanapplications.dto.approval.ApprovalWorkflowDto;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class LoanApplicationDto {
    private Long id;
    private String applicationNumber;

    // Borrower information
    private Long borrowerId;
    private String borrowerName;
    private String borrowerNumber;

    // Reference to borrower's KYC and documents
    private BorrowerKycSummaryDto borrowerKycSummary;
    private List<BorrowerDocumentReferenceDto> borrowerDocuments;
    private DocumentComplianceSummary documentCompliance;

    // Loan application details
    private Long loanProductId;
    private String loanProductName;
    private BigDecimal appliedAmount;
    private Integer tenureMonths;
    private String purpose;
    private String status;
    private String stage;
    private String rejectionReason;
    private String officerComments;
    private String purposeCategory;

    // Financial details
    private BigDecimal approvedAmount;
    private BigDecimal interestRate;
    private BigDecimal processingFee;
    private BigDecimal insuranceFee;
    private BigDecimal disbursementAmount;

    // Branch information
    private Long branchId;
    private String branchName;
    private String branchCode;

    // Dates
    private LocalDateTime submittedDate;
    private LocalDateTime approvedDate;
    private LocalDateTime rejectedDate;
    private LocalDateTime returnedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Approval-related information
    private List<ApplicationApprovalDto> approvalHistory;
    private List<ApprovalConditionDto> approvalConditions;
    private ApprovalWorkflowDto approvalWorkflow;

    // Risk and compliance
    private Integer riskScore;
    private String riskLevel;
    private Boolean flaggedForReview;
    private String reviewNotes;
    private Boolean termsAccepted;

    // User information
    private Long createdById;
    private String createdByUsername;
    private String createdByName;
    private Long approvedById;
    private String approvedByUsername;

    //Fess


    // Status flags
    private Boolean isUnderReview;
    private Boolean requiresMultipleApprovals;
    private Integer currentApprovalLevel;
    private Integer totalApprovalLevels;
    private String nextApprovalRole;
    private Boolean canCurrentUserApprove;
    private Boolean canCurrentUserView;

    // Helper methods
    public boolean isDocumentRequirementsMet() {
        return documentCompliance != null && documentCompliance.getMeetsRequirements();
    }

    public boolean isKycComplete() {
        return borrowerKycSummary != null && borrowerKycSummary.getKycComplete();
    }

    public boolean canBeSubmitted() {
        return isDocumentRequirementsMet() && isKycComplete();
    }

    public boolean hasApprovalHistory() {
        return approvalHistory != null && !approvalHistory.isEmpty();
    }

    public boolean hasPendingConditions() {
        if (approvalConditions == null || approvalConditions.isEmpty()) {
            return false;
        }
        return approvalConditions.stream()
                .anyMatch(condition -> "PENDING".equals(condition.getStatus()));
    }

    public boolean isFullyApproved() {
        return "APPROVED".equals(status) && !hasPendingConditions();
    }

    public boolean isPendingApproval() {
        return "SUBMITTED".equals(status) ||
                "UNDER_REVIEW".equals(status) ||
                "PENDING_APPROVAL".equals(status);
    }

    public boolean isRejected() {
        return "REJECTED".equals(status);
    }

    public boolean isReturnedForRevision() {
        return "NEEDS_REVISION".equals(status) || "RETURNED".equals(status);
    }

    public long getDaysSinceSubmission() {
        if (submittedDate == null) {
            return 0;
        }
        return java.time.Duration.between(submittedDate, LocalDateTime.now()).toDays();
    }

}