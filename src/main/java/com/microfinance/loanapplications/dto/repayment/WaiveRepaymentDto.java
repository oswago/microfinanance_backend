package com.microfinance.loanapplications.dto.repayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaiveRepaymentDto {
    private String reason;
    private String waiverType; // FULL, PARTIAL, PENALTY_ONLY, INTEREST_ONLY
    private BigDecimal waivedAmount;
    private BigDecimal waivedPrincipal;
    private BigDecimal waivedInterest;
    private BigDecimal waivedPenalty;

    // Approval information
    private String approvalReference;
    private Long approvedBy;
    private String approvalNotes;

    // Conditions
    private Boolean requireBorrowerConsent;
    private Boolean requireManagementApproval;
    private String termsAndConditions;

    // Impact assessment
    private BigDecimal impactOnPortfolio;
    private String riskAssessment;
    private String justification;

    // Documentation
    private String supportingDocuments;
    private String legalComplianceNotes;

    private Long installmentId;

    public boolean isFullWaiver() {
        return "FULL".equalsIgnoreCase(waiverType);
    }

    public boolean isPartialWaiver() {
        return "PARTIAL".equalsIgnoreCase(waiverType);
    }

    public boolean isPenaltyOnlyWaiver() {
        return "PENALTY_ONLY".equalsIgnoreCase(waiverType);
    }

    public boolean isInterestOnlyWaiver() {
        return "INTEREST_ONLY".equalsIgnoreCase(waiverType);
    }

    public void validateWaiver() {
        if (waivedAmount == null || waivedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Waived amount must be greater than zero");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Waiver reason is required");
        }

        if (waiverType == null) {
            throw new IllegalArgumentException("Waiver type is required");
        }
    }

}

