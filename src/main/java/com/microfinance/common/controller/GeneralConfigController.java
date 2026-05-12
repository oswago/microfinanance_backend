package com.microfinance.common.controller;

import com.microfinance.common.config.GeneralConfig;
import com.microfinance.common.service.GeneralConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/config/general-config")
@RequiredArgsConstructor
public class GeneralConfigController {

    private final GeneralConfigService generalConfigService;

    // 1. Borrower related endpoints
    @GetMapping("/client-types")
    public ResponseEntity<List<Map<String, String>>> getClientTypes() {
        List<Map<String, String>> clientTypes = generalConfigService.getClientTypes().stream()
                .map(enumValue -> Map.of(
                        "name", enumValue.name(),
                        "description", getClientTypeDescription(enumValue)
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(clientTypes);
    }

    @GetMapping("/risk-ratings")
    public ResponseEntity<List<String>> getRiskRatings() {
        List<String> riskRatings = generalConfigService.getRiskRatingNames();
        return ResponseEntity.ok(riskRatings);
    }

    @GetMapping("/genders")
    public ResponseEntity<List<String>> getGenders() {
        List<String> genders = generalConfigService.getGenders().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(genders);
    }

    @GetMapping("/marital-statuses")
    public ResponseEntity<List<String>> getMaritalStatuses() {
        List<String> maritalStatuses = generalConfigService.getMaritalStatuses().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(maritalStatuses);
    }

    @GetMapping("/borrower-statuses")
    public ResponseEntity<List<String>> getBorrowerStatuses() {
        List<String> borrowerStatuses = generalConfigService.getBorrowerStatuses().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(borrowerStatuses);
    }

    @GetMapping("/kyc-statuses")
    public ResponseEntity<List<String>> getKycStatuses() {
        List<String> kycStatuses = generalConfigService.getKycStatuses().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(kycStatuses);
    }

    // 2. Borrower Activity endpoints
    @GetMapping("/borrower-activities")
    public ResponseEntity<List<Map<String, String>>> getBorrowerActivityTypes() {
        List<Map<String, String>> activities = generalConfigService.getBorrowerActivityTypes().stream()
                .map(enumValue -> Map.of(
                        "name", enumValue.name(),
                        "category", getActivityCategory(enumValue)
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/borrower-activities/borrower-management")
    public ResponseEntity<List<String>> getBorrowerManagementActivities() {
        List<String> activities = generalConfigService.getBorrowerManagementActivities().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/borrower-activities/document")
    public ResponseEntity<List<String>> getDocumentActivities() {
        List<String> activities = generalConfigService.getDocumentActivities().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/borrower-activities/loan-application")
    public ResponseEntity<List<String>> getLoanApplicationActivities() {
        List<String> activities = generalConfigService.getLoanApplicationActivities().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/borrower-activities/repayment")
    public ResponseEntity<List<String>> getRepaymentActivities() {
        List<String> activities = generalConfigService.getRepaymentActivities().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(activities);
    }

    // 3. Borrower Group endpoints
    @GetMapping("/group-types")
    public ResponseEntity<List<Map<String, String>>> getGroupTypes() {
        List<Map<String, String>> groupTypes = generalConfigService.getGroupTypes().stream()
                .map(enumValue -> Map.of(
                        "name", enumValue.name(),
                        "description", getGroupTypeDescription(enumValue)
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(groupTypes);
    }

    @GetMapping("/group-statuses")
    public ResponseEntity<List<String>> getGroupStatuses() {
        List<String> groupStatuses = generalConfigService.getGroupStatuses().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(groupStatuses);
    }

    @GetMapping("/joint-liability-types")
    public ResponseEntity<List<String>> getJointLiabilityTypes() {
        List<String> liabilityTypes = generalConfigService.getJointLiabilityTypes().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(liabilityTypes);
    }

    // 4. Document Verification endpoints
    @GetMapping("/verification-statuses")
    public ResponseEntity<List<String>> getVerificationStatuses() {
        List<String> verificationStatuses = generalConfigService.getVerificationStatuses().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(verificationStatuses);
    }

    // 5. KYC Workflow Step Status endpoints
    @GetMapping("/step-statuses")
    public ResponseEntity<List<String>> getStepStatuses() {
        List<String> stepStatuses = generalConfigService.getStepStatuses().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stepStatuses);
    }

    // Specific enum value endpoints
    @GetMapping("/specific/client-types/individual")
    public ResponseEntity<String> getIndividualClientType() {
        return ResponseEntity.ok(generalConfigService.getIndividualClientType().name());
    }

    @GetMapping("/specific/client-types/group-member")
    public ResponseEntity<String> getGroupMemberClientType() {
        return ResponseEntity.ok(generalConfigService.getGroupMemberClientType().name());
    }

    @GetMapping("/specific/client-types/sme")
    public ResponseEntity<String> getSMEClientType() {
        return ResponseEntity.ok(generalConfigService.getSMEClientType().name());
    }

    @GetMapping("/specific/client-types/corporate")
    public ResponseEntity<String> getCorporateClientType() {
        return ResponseEntity.ok(generalConfigService.getCorporateClientType().name());
    }

    @GetMapping("/specific/group-types/joint-liability")
    public ResponseEntity<String> getJointLiabilityGroupType() {
        return ResponseEntity.ok(generalConfigService.getJointLiabilityGroupType().name());
    }

    @GetMapping("/specific/group-types/savings")
    public ResponseEntity<String> getSavingsGroupType() {
        return ResponseEntity.ok(generalConfigService.getSavingsGroupType().name());
    }

    @GetMapping("/specific/risk-ratings/low")
    public ResponseEntity<String> getLowRiskRating() {
        return ResponseEntity.ok(generalConfigService.getLowRiskRating().name());
    }

    @GetMapping("/specific/risk-ratings/high")
    public ResponseEntity<String> getHighRiskRating() {
        return ResponseEntity.ok(generalConfigService.getHighRiskRating().name());
    }

    // Validation endpoints
    @GetMapping("/validate/client-type/{value}")
    public ResponseEntity<Map<String, Boolean>> isValidClientType(@PathVariable String value) {
        boolean isValid = generalConfigService.isValidClientType(value);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    @GetMapping("/validate/group-type/{value}")
    public ResponseEntity<Map<String, Boolean>> isValidGroupType(@PathVariable String value) {
        boolean isValid = generalConfigService.isValidGroupType(value);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    // Lookup endpoints
    @GetMapping("/lookup/client-type/{name}")
    public ResponseEntity<Map<String, String>> getClientTypeByName(@PathVariable String name) {
        try {
            GeneralConfig.ClientType clientType = generalConfigService.getClientTypeByName(name);
            return ResponseEntity.ok(Map.of(
                    "name", clientType.name(),
                    "description", getClientTypeDescription(clientType)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/lookup/group-type/{name}")
    public ResponseEntity<Map<String, String>> getGroupTypeByName(@PathVariable String name) {
        try {
            GeneralConfig.GroupType groupType = generalConfigService.getGroupTypeByName(name);
            return ResponseEntity.ok(Map.of(
                    "name", groupType.name(),
                    "description", getGroupTypeDescription(groupType)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/lookup/risk-rating/{name}")
    public ResponseEntity<String> getRiskRatingByName(@PathVariable String name) {
        try {
            GeneralConfig.RiskRating riskRating = generalConfigService.getRiskRatingByName(name);
            return ResponseEntity.ok(riskRating.name());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Bulk endpoints for frontend initialization
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllConfigurations() {
        Map<String, Object> allConfigs = Map.ofEntries(
                Map.entry("clientTypes", generalConfigService.getClientTypeNames()),
                Map.entry("riskRatings", generalConfigService.getRiskRatingNames()),
                Map.entry("genders", generalConfigService.getGenders().stream().map(Enum::name).collect(Collectors.toList())),
                Map.entry("maritalStatuses", generalConfigService.getMaritalStatuses().stream().map(Enum::name).collect(Collectors.toList())),
                Map.entry("borrowerStatuses", generalConfigService.getBorrowerStatuses().stream().map(Enum::name).collect(Collectors.toList())),
                Map.entry("kycStatuses", generalConfigService.getKycStatuses().stream().map(Enum::name).collect(Collectors.toList())),
                Map.entry("groupTypes", generalConfigService.getGroupTypeNames()),
                Map.entry("groupStatuses", generalConfigService.getGroupStatuses().stream().map(Enum::name).collect(Collectors.toList())),
                Map.entry("jointLiabilityTypes", generalConfigService.getJointLiabilityTypes().stream().map(Enum::name).collect(Collectors.toList())),
                Map.entry("verificationStatuses", generalConfigService.getVerificationStatuses().stream().map(Enum::name).collect(Collectors.toList())),
                Map.entry("stepStatuses", generalConfigService.getStepStatuses().stream().map(Enum::name).collect(Collectors.toList()))
        );
        return ResponseEntity.ok(allConfigs);
    }

    // Helper methods for descriptions
    private String getClientTypeDescription(GeneralConfig.ClientType clientType) {
        return switch (clientType) {
            case INDIVIDUAL -> "Individual borrower";
            case GROUP_MEMBER -> "Member of a borrower group";
            case SME -> "Small and Medium Enterprise";
            case CORPORATE -> "Corporate entity";
        };
    }

    private String getGroupTypeDescription(GeneralConfig.GroupType groupType) {
        return switch (groupType) {
            case JOINT_LIABILITY -> "Group with joint liability for loans";
            case SAVINGS -> "Savings-focused group";
            case AGRICULTURAL -> "Agriculture-focused group";
            case WOMEN -> "Women-only group";
            case YOUTH -> "Youth-focused group";
            case COMMUNITY -> "Community-based group";
        };
    }

    private String getActivityCategory(GeneralConfig.BorrowerActivityType activityType) {
        if (activityType.name().startsWith("BORROWER_")) return "Borrower Management";
        if (activityType.name().startsWith("DOCUMENT_")) return "Document Management";
        if (activityType.name().startsWith("GROUP_")) return "Group Management";
        if (activityType.name().startsWith("LOAN_APPLICATION_")) return "Loan Application";
        if (activityType.name().startsWith("LOAN_DISBURSEMENT_")) return "Loan Disbursement";
        if (activityType.name().startsWith("REPAYMENT_")) return "Repayment";
        if (activityType.name().startsWith("SAVINGS_")) return "Savings";
        if (activityType.name().startsWith("GUARANTOR_")) return "Guarantor";
        if (activityType.name().startsWith("MEETING_")) return "Meeting";
        return "System";
    }
}