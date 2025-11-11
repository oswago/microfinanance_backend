package com.microfinance.borrower.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class GroupLoanEligibilityDto {
    private Long groupId;
    private String groupName;
    private Long loanProductId;
    private String loanProductName;
    
    // Overall Eligibility
    private Boolean isEligible;
    private String eligibilityStatus; // ELIGIBLE, NOT_ELIGIBLE, CONDITIONAL
    private BigDecimal eligibleLoanAmount;
    private BigDecimal maximumEligibleAmount;
    private BigDecimal recommendedLoanAmount;
    
    // Eligibility Criteria Check
    private Boolean meetsMemberRequirements;
    private Boolean meetsRepaymentHistory;
    private Boolean meetsSavingsRequirements;
    private Boolean meetsAttendanceRequirements;
    private Boolean meetsKycRequirements;
    private Boolean meetsCreditScoreRequirements;
    
    // Quantitative Metrics
    private Integer eligibleMemberCount;
    private Integer totalMemberCount;
    private BigDecimal groupRepaymentRate;
    private BigDecimal groupSavingsRatio;
    private BigDecimal groupAttendanceRate;
    private BigDecimal averageMemberCreditScore;
    
    // Risk Assessment
    private String groupRiskRating;
    private BigDecimal probabilityOfDefault;
    private String riskAssessmentNotes;
    
    // Constraints and Limits
    private BigDecimal maximumLoanAmountPerMember;
    private BigDecimal maximumTotalGroupLoan;
    private Integer minimumMemberRequirement;
    private BigDecimal minimumSavingsRequirement;
    private BigDecimal minimumRepaymentRateRequirement;
    
    // Conditions and Requirements
    private List<String> eligibilityConditions = new ArrayList<>();
    private List<String> additionalRequirements = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();
    
    // Member-level Eligibility Details
    private List<MemberEligibilityDetail> memberEligibilityDetails = new ArrayList<>();
    
    // Historical Performance
    private Integer previousLoansTaken;
    private Integer successfulPreviousLoans;
    private BigDecimal previousLoanRepaymentRate;
    private Boolean hasActiveLoan;
    private BigDecimal currentOutstandingBalance;
    
    // Approval Workflow
    private String approvalLevelRequired; // OFFICER, MANAGER, COMMITTEE
    private List<String> requiredDocuments = new ArrayList<>();
    private String nextSteps;

    @Data
    public static class MemberEligibilityDetail {
        private Long memberId;
        private String memberName;
        private String borrowerNumber;
        private Boolean isEligible;
        private String eligibilityStatus;
        private List<String> eligibilityIssues = new ArrayList<>();
        private BigDecimal individualEligibleAmount;
        private String riskRating;
        private Integer creditScore;
        private Boolean kycComplete;
        private BigDecimal outstandingBalance;
        private Boolean isGroupLeader;
        
        // Helper method
        public boolean hasEligibilityIssues() {
            return eligibilityIssues != null && !eligibilityIssues.isEmpty();
        }
    }

    // Helper methods
    public BigDecimal getEligibilityPercentage() {
        return totalMemberCount > 0 ? 
            BigDecimal.valueOf(eligibleMemberCount)
                .divide(BigDecimal.valueOf(totalMemberCount), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100)) : 
            BigDecimal.ZERO;
    }
    
    public boolean hasEligibilityConditions() {
        return eligibilityConditions != null && !eligibilityConditions.isEmpty();
    }
    
    public boolean hasAdditionalRequirements() {
        return additionalRequirements != null && !additionalRequirements.isEmpty();
    }
    
    public String getSummaryStatus() {
        if (!isEligible) {
            return "NOT_ELIGIBLE";
        } else if (hasEligibilityConditions() || hasAdditionalRequirements()) {
            return "CONDITIONAL";
        } else {
            return "FULLY_ELIGIBLE";
        }
    }
    
    public BigDecimal getGroupRepaymentRatePercentage() {
        return groupRepaymentRate != null ? 
            groupRepaymentRate.multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
    }
    
    public BigDecimal getGroupAttendanceRatePercentage() {
        return groupAttendanceRate != null ? 
            groupAttendanceRate.multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
    }
}