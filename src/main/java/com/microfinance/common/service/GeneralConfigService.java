package com.microfinance.common.service;

import com.microfinance.common.config.GeneralConfig;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class GeneralConfigService {

    // 1. Borrower related enums
    public List<GeneralConfig.ClientType> getClientTypes() {
        return Arrays.asList(GeneralConfig.ClientType.values());
    }

    public List<GeneralConfig.RiskRating> getRiskRatings() {
        return Arrays.asList(GeneralConfig.RiskRating.values());
    }

    public List<GeneralConfig.Gender> getGenders() {
        return Arrays.asList(GeneralConfig.Gender.values());
    }

    public List<GeneralConfig.MaritalStatus> getMaritalStatuses() {
        return Arrays.asList(GeneralConfig.MaritalStatus.values());
    }

    public List<GeneralConfig.BorrowerStatus> getBorrowerStatuses() {
        return Arrays.asList(GeneralConfig.BorrowerStatus.values());
    }

    public List<GeneralConfig.KycStatus> getKycStatuses() {
        return Arrays.asList(GeneralConfig.KycStatus.values());
    }

    // 2. Borrower Activity enums
    public List<GeneralConfig.BorrowerActivityType> getBorrowerActivityTypes() {
        return Arrays.asList(GeneralConfig.BorrowerActivityType.values());
    }

    // Filtered activity types by category
    public List<GeneralConfig.BorrowerActivityType> getBorrowerManagementActivities() {
        return Arrays.asList(
            GeneralConfig.BorrowerActivityType.BORROWER_CREATED,
            GeneralConfig.BorrowerActivityType.BORROWER_UPDATED,
            GeneralConfig.BorrowerActivityType.BORROWER_STATUS_CHANGED,
            GeneralConfig.BorrowerActivityType.BORROWER_KYC_INITIATED,
            GeneralConfig.BorrowerActivityType.BORROWER_KYC_VERIFIED,
            GeneralConfig.BorrowerActivityType.BORROWER_KYC_REJECTED,
            GeneralConfig.BorrowerActivityType.BORROWER_KYC_EXPIRED
        );
    }

    public List<GeneralConfig.BorrowerActivityType> getDocumentActivities() {
        return Arrays.asList(
            GeneralConfig.BorrowerActivityType.DOCUMENT_UPLOADED,
            GeneralConfig.BorrowerActivityType.DOCUMENT_VERIFIED,
            GeneralConfig.BorrowerActivityType.DOCUMENT_REJECTED,
            GeneralConfig.BorrowerActivityType.DOCUMENT_DELETED
        );
    }

    public List<GeneralConfig.BorrowerActivityType> getLoanApplicationActivities() {
        return Arrays.asList(
            GeneralConfig.BorrowerActivityType.LOAN_APPLICATION_SUBMITTED,
            GeneralConfig.BorrowerActivityType.LOAN_APPLICATION_APPROVED,
            GeneralConfig.BorrowerActivityType.LOAN_APPLICATION_REJECTED,
            GeneralConfig.BorrowerActivityType.LOAN_APPLICATION_WITHDRAWN
        );
    }

    public List<GeneralConfig.BorrowerActivityType> getRepaymentActivities() {
        return Arrays.asList(
            GeneralConfig.BorrowerActivityType.REPAYMENT_MADE,
            GeneralConfig.BorrowerActivityType.REPAYMENT_SCHEDULED,
            GeneralConfig.BorrowerActivityType.REPAYMENT_OVERDUE,
            GeneralConfig.BorrowerActivityType.REPAYMENT_PARTIAL,
            GeneralConfig.BorrowerActivityType.REPAYMENT_BOUNCE
        );
    }

    // 3. Borrower Group enums
    public List<GeneralConfig.GroupType> getGroupTypes() {
        return Arrays.asList(GeneralConfig.GroupType.values());
    }

    public List<GeneralConfig.GroupStatus> getGroupStatuses() {
        return Arrays.asList(GeneralConfig.GroupStatus.values());
    }

    public List<GeneralConfig.JointLiabilityType> getJointLiabilityTypes() {
        return Arrays.asList(GeneralConfig.JointLiabilityType.values());
    }

    // 4. Document Verification enums
    public List<GeneralConfig.VerificationStatus> getVerificationStatuses() {
        return Arrays.asList(GeneralConfig.VerificationStatus.values());
    }

    // 5. KYC Workflow Step Status enums
    public List<GeneralConfig.StepStatus> getStepStatuses() {
        return Arrays.asList(GeneralConfig.StepStatus.values());
    }

    // Utility methods for specific enum values
    public GeneralConfig.ClientType getIndividualClientType() {
        return GeneralConfig.ClientType.INDIVIDUAL;
    }

    public GeneralConfig.ClientType getGroupMemberClientType() {
        return GeneralConfig.ClientType.GROUP_MEMBER;
    }

    public GeneralConfig.ClientType getSMEClientType() {
        return GeneralConfig.ClientType.SME;
    }

    public GeneralConfig.ClientType getCorporateClientType() {
        return GeneralConfig.ClientType.CORPORATE;
    }

    public GeneralConfig.GroupType getJointLiabilityGroupType() {
        return GeneralConfig.GroupType.JOINT_LIABILITY;
    }

    public GeneralConfig.GroupType getSavingsGroupType() {
        return GeneralConfig.GroupType.SAVINGS;
    }

    public GeneralConfig.RiskRating getLowRiskRating() {
        return GeneralConfig.RiskRating.LOW;
    }

    public GeneralConfig.RiskRating getHighRiskRating() {
        return GeneralConfig.RiskRating.HIGH;
    }

    // Method to get enum by name
    public GeneralConfig.ClientType getClientTypeByName(String name) {
        return GeneralConfig.ClientType.valueOf(name.toUpperCase());
    }

    public GeneralConfig.GroupType getGroupTypeByName(String name) {
        return GeneralConfig.GroupType.valueOf(name.toUpperCase());
    }

    public GeneralConfig.RiskRating getRiskRatingByName(String name) {
        return GeneralConfig.RiskRating.valueOf(name.toUpperCase());
    }

    // Method to check if value exists in enum
    public boolean isValidClientType(String value) {
        try {
            GeneralConfig.ClientType.valueOf(value.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isValidGroupType(String value) {
        try {
            GeneralConfig.GroupType.valueOf(value.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Method to get all enum values as strings
    public List<String> getClientTypeNames() {
        return Arrays.stream(GeneralConfig.ClientType.values())
                .map(Enum::name)
                .toList();
    }

    public List<String> getGroupTypeNames() {
        return Arrays.stream(GeneralConfig.GroupType.values())
                .map(Enum::name)
                .toList();
    }

    public List<String> getRiskRatingNames() {
        return Arrays.stream(GeneralConfig.RiskRating.values())
                .map(Enum::name)
                .toList();
    }


}