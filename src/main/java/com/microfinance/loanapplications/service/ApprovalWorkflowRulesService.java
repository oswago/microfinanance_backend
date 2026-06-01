package com.microfinance.loanapplications.service;

import com.microfinance.base.entity.User;
import com.microfinance.loanapplications.dto.approval.ApprovalWorkflowStepDto;
import com.microfinance.loanapplications.dto.approval.SLAStatusDto;
import com.microfinance.loanapplications.entity.ApplicationApproval;
import com.microfinance.loanapplications.entity.LoanApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure business rules for approval workflow - No database access
 * Single source of truth for all workflow logic
 */
@Slf4j
@Service
public class ApprovalWorkflowRulesService {

    // ========== CONSTANTS - SINGLE SOURCE OF TRUTH ==========
    private static final BigDecimal BRANCH_MANAGER_THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal REGIONAL_MANAGER_THRESHOLD = new BigDecimal("500000");
    private static final BigDecimal CREDIT_COMMITTEE_THRESHOLD = new BigDecimal("1000000");
    
    private static final BigDecimal LOAN_OFFICER_MAX_AMOUNT = new BigDecimal("50000");
    private static final BigDecimal CREDIT_OFFICER_MAX_AMOUNT = new BigDecimal("250000");
    private static final BigDecimal BRANCH_MANAGER_MAX_AMOUNT = new BigDecimal("1000000");
    private static final BigDecimal REGIONAL_MANAGER_MAX_AMOUNT = new BigDecimal("5000000");


    // Escalation thresholds
    private static final BigDecimal ESCALATION_CREDIT_APPROVER_THRESHOLD = new BigDecimal("1000000");
    private static final BigDecimal ESCALATION_REGIONAL_MANAGER_THRESHOLD = new BigDecimal("5000000");
    private static final BigDecimal ESCALATION_SUPER_ADMIN_THRESHOLD = new BigDecimal("10000000");

    
    private static final int APPROVAL_SLA_HOURS = 24;
    private static final int HIGH_RISK_SCORE_THRESHOLD = 70;
    private static final int LARGE_AMOUNT_PRIORITY_THRESHOLD = 500000;
    private static final int OVERDUE_DAYS_THRESHOLD = 3;

    // Escalation constants
    private static final int MAX_ESCALATION_LEVEL = 3;
    private static final int ESCALATION_WAIT_HOURS = 24;

    @Value("${superadmin.approval.bypass.enabled:false}")
    private boolean superAdminBypassEnabled;

    @Value("${superadmin.approval.bypass.min-level:5}")
    private int superAdminBypassMinLevel;

    // ========== APPROVAL LEVEL CALCULATIONS ==========
    
    /**
     * Get total number of approval levels required for this application
     */
    public int getTotalApprovalLevels(LoanApplication application) {
        BigDecimal amount = application.getAppliedAmount();
        int levels = 2; // Base: Document Verification + Credit Assessment

        if (amount.compareTo(BRANCH_MANAGER_THRESHOLD) > 0) {
            levels++; // Branch Manager
        }
        if (amount.compareTo(REGIONAL_MANAGER_THRESHOLD) > 0) {
            levels++; // Regional Manager
        }
        if (amount.compareTo(CREDIT_COMMITTEE_THRESHOLD) > 0) {
            levels++; // Credit Committee
        }

        // Adjust for risk score
        Integer riskScore = application.getRiskScore();
        if (riskScore != null && riskScore > HIGH_RISK_SCORE_THRESHOLD) {
            levels = Math.min(levels + 1, 6);
        }

        log.debug("Total approval levels for amount {}: {}", amount, levels);
        return levels;
    }

    /**
     * Get the role required for a specific approval level
     */
    public String getRoleForLevel(int level, BigDecimal amount) {
        switch (level) {
            case 1: return "LOAN_OFFICER";
            case 2: return "CREDIT_OFFICER";
            case 3: return amount.compareTo(BRANCH_MANAGER_THRESHOLD) > 0 ? "BRANCH_MANAGER" : null;
            case 4: return amount.compareTo(REGIONAL_MANAGER_THRESHOLD) > 0 ? "REGIONAL_MANAGER" : null;
            case 5: return amount.compareTo(CREDIT_COMMITTEE_THRESHOLD) > 0 ? "CREDIT_COMMITTEE" : null;
            default: return null;
        }
    }


    public boolean hasAmountAuthority(LoanApplication application, User user) {
        BigDecimal amount = application.getAppliedAmount();
        switch (user.getRole()) {
            case BRANCH_MANAGER:
                return amount.compareTo(BRANCH_MANAGER_MAX_AMOUNT) <= 0;
            case CREDIT_APPROVER:
                return amount.compareTo(CREDIT_OFFICER_MAX_AMOUNT) <= 0;
            case REGIONAL_MANAGER:
                return amount.compareTo(REGIONAL_MANAGER_MAX_AMOUNT) <= 0;
            case SUPER_ADMIN:
                return true;
            default:
                return false;
        }
    }


    /**
     * Get the role required for the next approval level
     */
    public String getNextApprovalRole(LoanApplication application, int currentLevel) {
        BigDecimal amount = application.getAppliedAmount();
        String role = getRoleForLevel(currentLevel + 1, amount);
        return role != null ? role : "COMPLETED";
    }

    /**
     * Get the role required for the next approval level (using existing approvals)
     */
    public String getNextApprovalRoleFromApprovals(LoanApplication application, List<ApplicationApproval> approvals) {
        if (application.getStatus() != null && 
            (application.getStatus().name().equals("APPROVED") || application.getStatus().name().equals("REJECTED"))) {
            return "COMPLETED";
        }

        int maxCompletedLevel = approvals.stream()
                .filter(a -> a.getDecision() != null && a.getDecision().name().equals("APPROVED"))
                .mapToInt(ApplicationApproval::getApprovalLevel)
                .max()
                .orElse(0);

        int totalLevels = getTotalApprovalLevels(application);
        
        if (maxCompletedLevel >= totalLevels) {
            return "COMPLETED";
        }
        
        return getNextApprovalRole(application, maxCompletedLevel);
    }

    // ========== FINAL APPROVAL CHECK ==========
    
    /**
     * Check if this is the final approval
     */
    /*
    public boolean isFinalApproval(LoanApplication application, int currentLevel, User approver) {
        int totalLevels = getTotalApprovalLevels(application);
        // Check if this is the last required approval level
        if (currentLevel >= totalLevels) {
            return true;
        }
        // SUPER_ADMIN can always give final approval
        if (approver.getRole() == User.UserRole.SUPER_ADMIN) {
            return true;
        }
        return false;
    }
  */

    /**
     * Check if SUPER_ADMIN bypass is enabled
     */
    private boolean isSuperAdminBypassEnabled() {
        return superAdminBypassEnabled;
    }

    /**
     * Get minimum level required for SUPER_ADMIN bypass
     */
    private int getSuperAdminBypassMinLevel() {
        return superAdminBypassMinLevel;
    }

    /**
     * Check if this is the final approval
     */
    public boolean isFinalApproval(LoanApplication application, int currentLevel, User approver) {
        int totalLevels = getTotalApprovalLevels(application);
        // SUPER_ADMIN can approve any level, but still needs to go through each level
        if (approver.getRole() == User.UserRole.SUPER_ADMIN) {
            // Don't mark as final until the last level
            return currentLevel >= totalLevels;
        }
        // Normal users follow standard rules
        return currentLevel >= totalLevels;
    }


    // ========== AMOUNT AUTHORITY CHECKS ==========
    
    /**
     * Check if user can approve based on amount limits
     */
    public boolean canUserApproveAtAmountLevel(BigDecimal amount, User.UserRole role) {
        BigDecimal maxAmount = getMaxApprovalAmountForRole(role);
        return maxAmount == null || amount.compareTo(maxAmount) <= 0;
    }

    /**
     * Get maximum approval amount for a user role
     */
    public BigDecimal getMaxApprovalAmountForRole(User.UserRole role) {
        switch (role) {
            case LOAN_OFFICER:
                return LOAN_OFFICER_MAX_AMOUNT;
            case CREDIT_OFFICER:
                return CREDIT_OFFICER_MAX_AMOUNT;
            case BRANCH_MANAGER:
                return BRANCH_MANAGER_MAX_AMOUNT;
            case REGIONAL_MANAGER:
                return REGIONAL_MANAGER_MAX_AMOUNT;
            case SUPER_ADMIN:
                return null; // No limit
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * Check if user has the required role for the current approval level
     */
    public boolean hasCorrectRoleForLevel(User.UserRole userRole, int requiredLevel, BigDecimal amount) {
        String requiredRole = getRoleForLevel(requiredLevel, amount);
        if (requiredRole == null) return false;
        return userRole.name().equals(requiredRole);
    }

    // ========== PRIORITY CALCULATIONS ==========
    
    /**
     * Calculate priority score for sorting
     * 0 = Highest (large amount ≥ 500,000)
     * 1 = Medium (overdue, submitted 3+ days ago)
     * 2 = Low (others)
     */
    public int calculatePriorityScore(LoanApplication application) {
        // Priority 0: Large amounts (≥ 500,000)
        if (application.getAppliedAmount() != null &&
                application.getAppliedAmount().compareTo(BigDecimal.valueOf(LARGE_AMOUNT_PRIORITY_THRESHOLD)) >= 0) {
            return 0;
        }

        // Priority 1: Overdue applications (submitted 3+ days ago)
        Long daysSinceSubmission = calculateDaysSinceSubmission(application);
        if (daysSinceSubmission != null && daysSinceSubmission >= OVERDUE_DAYS_THRESHOLD) {
            return 1;
        }

        // Priority 2: Others
        return 2;
    }

    /**
     * Get priority level label
     */
    public String getPriorityLevel(int priorityScore) {
        switch (priorityScore) {
            case 0: return "CRITICAL";
            case 1: return "HIGH";
            case 2: return "MEDIUM";
            case 3: return "LOW";
            default: return "VERY_LOW";
        }
    }

    /**
     * Get priority reason
     */
    public String getPriorityReason(LoanApplication application, int priorityScore) {
        if (priorityScore == 0) return "Large amount (≥ 500,000)";
        if (priorityScore == 1) return "Overdue (submitted 3+ days ago)";
        return "Standard priority";
    }

    /**
     * Check if application is large amount
     */
    public boolean isLargeAmount(LoanApplication application) {
        return application.getAppliedAmount() != null &&
                application.getAppliedAmount().compareTo(BigDecimal.valueOf(LARGE_AMOUNT_PRIORITY_THRESHOLD)) >= 0;
    }

    /**
     * Check if application is overdue
     */
    public boolean isOverdue(LoanApplication application) {
        if (application.getSubmittedDate() == null) {
            return false;
        }
        long daysSinceSubmission = ChronoUnit.DAYS.between(
                application.getSubmittedDate().toLocalDate(),
                LocalDate.now()
        );
        return daysSinceSubmission >= OVERDUE_DAYS_THRESHOLD;
    }

    /**
     * Calculate days since submission
     */
    public Long calculateDaysSinceSubmission(LoanApplication application) {
        if (application.getSubmittedDate() == null) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(
                application.getSubmittedDate().toLocalDate(),
                LocalDate.now()
        );
    }

    // ========== WORKFLOW STEP BUILDING ==========
    
    /**
     * Build workflow steps for frontend display
     */
    public List<ApprovalWorkflowStepDto> buildWorkflowSteps(LoanApplication application,
                                                            List<ApplicationApproval> approvals,
                                                            List<?> conditions) {
        List<ApprovalWorkflowStepDto> steps = new ArrayList<>();
        BigDecimal amount = application.getAppliedAmount();

        // Step 1: Document Verification (Always present)
        steps.add(buildWorkflowStep(1, "DOCUMENT_VERIFICATION", "Document Verification",
                "Verify borrower documents and application completeness",
                "LOAN_OFFICER", application, approvals));

        // Step 2: Credit Assessment (Always present)
        steps.add(buildWorkflowStep(2, "CREDIT_ASSESSMENT", "Credit Assessment",
                "Assess borrower creditworthiness and repayment capacity",
                "CREDIT_OFFICER", application, approvals));

        // Step 3: Branch Manager Approval (Only for amounts > 50,000)
        if (amount.compareTo(BRANCH_MANAGER_THRESHOLD) > 0) {
            steps.add(buildWorkflowStep(3, "BRANCH_MANAGER_APPROVAL", "Branch Manager Approval",
                    "Review and approve at branch level",
                    "BRANCH_MANAGER", application, approvals));
        }

        // Step 4: Regional Manager Approval (Only for amounts > 500,000)
        if (amount.compareTo(REGIONAL_MANAGER_THRESHOLD) > 0) {
            steps.add(buildWorkflowStep(4, "REGIONAL_MANAGER_APPROVAL", "Regional Manager Approval",
                    "Regional level review for large amounts",
                    "REGIONAL_MANAGER", application, approvals));
        }

        // Step 5: Credit Committee (Only for amounts > 1,000,000)
        if (amount.compareTo(CREDIT_COMMITTEE_THRESHOLD) > 0) {
            steps.add(buildWorkflowStep(5, "CREDIT_COMMITTEE", "Credit Committee Approval",
                    "Final approval by credit committee for very large amounts",
                    "CREDIT_COMMITTEE", application, approvals));
        }

        // Re-number steps sequentially
        for (int i = 0; i < steps.size(); i++) {
            steps.get(i).setStepNumber(i + 1);
        }

        log.debug("Built {} workflow steps for amount: {}", steps.size(), amount);
        return steps;
    }

    private ApprovalWorkflowStepDto buildWorkflowStep(int stepNumber, String stepCode, String stepName,
                                                      String stepDescription, String approvalRole,
                                                      LoanApplication application,
                                                      List<ApplicationApproval> approvals) {
        
        ApprovalWorkflowStepDto step = new ApprovalWorkflowStepDto();
        step.setStepNumber(stepNumber);
        step.setStepCode(stepCode);
        step.setStepName(stepName);
        step.setStepDescription(stepDescription);
        step.setApprovalRole(approvalRole);
        step.setRole(approvalRole);
        step.setRoleDisplay(getRoleDisplayName(approvalRole));
        step.setStatus("PENDING");
       // step.setStatusDisplay("Pending");
       // step.setStatusColor("warning");
        step.setIsCompleted(false);
        step.setIsCurrentStep(false);
        step.setIsRequired(true);
        step.setIsOverdue(false);

        // Find if there's an approval for this role
        approvals.stream()
                .filter(approval -> approval.getApprovalRole() != null &&
                        approval.getApprovalRole().equals(approvalRole))
                .findFirst()
                .ifPresent(approval -> {
                    step.setStatus(approval.getDecision().name());
                    //step.setStatusDisplay(getStatusDisplayName(approval.getDecision().name()));
                   // step.setStatusColor(getStatusColor(approval.getDecision().name()));
                    step.setDecision(approval.getDecision().name());
                    step.setComments(approval.getComments());
                    step.setApproverName(approval.getApprover() != null ? 
                            approval.getApprover().getFirstName() + " " + approval.getApprover().getLastName() : null);
                    step.setApproverUsername(approval.getApprover() != null ? 
                            approval.getApprover().getUsername() : null);
                    step.setIsCompleted(approval.getDecision() != null && 
                            approval.getDecision().name().equals("APPROVED"));
                    if (approval.getDecisionDate() != null) {
                        step.setDecisionDate(approval.getDecisionDate());
                        step.setProcessedAt(approval.getDecisionDate().toEpochSecond(ZoneOffset.UTC));
                    }
                });

        // Set SLA deadline (24 hours per level from submission)
        if (application.getSubmittedDate() != null) {
            LocalDateTime deadline = application.getSubmittedDate()
                    .plusHours((long) APPROVAL_SLA_HOURS * stepNumber);
            step.setSlaDeadline(deadline.toEpochSecond(ZoneOffset.UTC));

            boolean isOverdue = LocalDateTime.now().isAfter(deadline) &&
                    !step.isCompleted() &&
                    !"REJECTED".equals(step.getStatus());
            step.setOverdue(isOverdue);
            step.setIsOverdue(isOverdue);
        }

        // Determine if this is the current step
        step.setCurrentStep(isCurrentStep(stepNumber, approvals));
        step.setIsCurrentStep(step.isCurrentStep());

        return step;
    }

    private boolean isCurrentStep(int stepNumber, List<ApplicationApproval> approvals) {
        int highestCompleted = approvals.stream()
                .filter(a -> a.getDecision() != null && a.getDecision().name().equals("APPROVED"))
                .mapToInt(ApplicationApproval::getApprovalLevel)
                .max()
                .orElse(0);
        return stepNumber == highestCompleted + 1;
    }



    // ========== ESCALATION RULES ==========

    /**
     * Determine the target role for escalation based on application amount
     *
     * @param application The loan application
     * @return The role to escalate to (SUPER_ADMIN, REGIONAL_MANAGER, CREDIT_APPROVER, or BRANCH_MANAGER)
     */
    public String determineEscalationTargetRole(LoanApplication application) {
        BigDecimal amount = application.getAppliedAmount();
        return determineEscalationTargetRole(amount);
    }

    /**
     * Determine the target role for escalation based on amount
     *
     * @param amount The loan amount
     * @return The role to escalate to
     */
    public String determineEscalationTargetRole(BigDecimal amount) {
        if (amount.compareTo(ESCALATION_SUPER_ADMIN_THRESHOLD) > 0) {
            return "SUPER_ADMIN";
        } else if (amount.compareTo(ESCALATION_REGIONAL_MANAGER_THRESHOLD) > 0) {
            return "REGIONAL_MANAGER";
        } else if (amount.compareTo(ESCALATION_CREDIT_APPROVER_THRESHOLD) > 0) {
            return "CREDIT_APPROVER";
        } else {
            return "BRANCH_MANAGER";
        }
    }

    /**
     * Determine escalation target role with risk adjustment
     *
     * @param application The loan application
     * @return The role to escalate to (may be escalated one level higher for high risk)
     */
    public String determineEscalationTargetRoleWithRisk(LoanApplication application) {
        String baseRole = determineEscalationTargetRole(application);
        Integer riskScore = application.getRiskScore();

        // For high-risk applications, escalate to higher authority
        if (riskScore != null && riskScore > HIGH_RISK_SCORE_THRESHOLD) {
            switch (baseRole) {
                case "BRANCH_MANAGER":
                    return "CREDIT_APPROVER";
                case "CREDIT_APPROVER":
                    return "REGIONAL_MANAGER";
                case "REGIONAL_MANAGER":
                    return "SUPER_ADMIN";
                default:
                    return baseRole;
            }
        }

        return baseRole;
    }

    /**
     * Get the next escalation level based on current level
     *
     * @param currentEscalationLevel Current escalation level (1, 2, or 3)
     * @return Next escalation level, or null if max reached
     */
    public Integer getNextEscalationLevel(Integer currentEscalationLevel) {
        if (currentEscalationLevel == null) {
            return 1;
        }
        if (currentEscalationLevel < MAX_ESCALATION_LEVEL) {
            return currentEscalationLevel + 1;
        }
        return null; // Max escalation level reached
    }

    /**
     * Get the target role for a specific escalation level
     *
     * @param escalationLevel The escalation level (1, 2, or 3)
     * @param application The loan application
     * @return The role to escalate to
     */
    public String getRoleForEscalationLevel(int escalationLevel, LoanApplication application) {
        BigDecimal amount = application.getAppliedAmount();

        switch (escalationLevel) {
            case 1:
                // First escalation: one level above current approver
                return determineEscalationTargetRole(amount);
            case 2:
                // Second escalation: two levels above
                String firstRole = determineEscalationTargetRole(amount);
                switch (firstRole) {
                    case "BRANCH_MANAGER":
                        return "REGIONAL_MANAGER";
                    case "CREDIT_APPROVER":
                        return "SUPER_ADMIN";
                    case "REGIONAL_MANAGER":
                        return "SUPER_ADMIN";
                    default:
                        return "SUPER_ADMIN";
                }
            case 3:
                // Third escalation: always SUPER_ADMIN
                return "SUPER_ADMIN";
            default:
                return "SUPER_ADMIN";
        }
    }

    /**
     * Check if escalation is needed based on waiting time
     *
     * @param submittedDate When the application was submitted
     * @param currentLevel Current approval level
     * @return True if escalation is needed
     */
    public boolean isEscalationNeeded(LocalDateTime submittedDate, int currentLevel) {
        if (submittedDate == null) {
            return false;
        }

        long hoursWaiting = ChronoUnit.HOURS.between(submittedDate, LocalDateTime.now());
        // Escalate after 48 hours at level 1, 24 hours at higher levels
        int waitThreshold = currentLevel == 1 ? 48 : ESCALATION_WAIT_HOURS;

        return hoursWaiting >= waitThreshold;
    }

    /**
     * Get escalation priority based on amount and risk
     *
     * @param application The loan application
     * @return Priority level: "URGENT", "HIGH", "MEDIUM", "LOW"
     */
    public String getEscalationPriority(LoanApplication application) {
        BigDecimal amount = application.getAppliedAmount();
        Integer riskScore = application.getRiskScore();

        if (amount.compareTo(ESCALATION_SUPER_ADMIN_THRESHOLD) > 0) {
            return "URGENT";
        } else if (amount.compareTo(ESCALATION_REGIONAL_MANAGER_THRESHOLD) > 0) {
            return "HIGH";
        } else if (riskScore != null && riskScore > HIGH_RISK_SCORE_THRESHOLD) {
            return "HIGH";
        } else if (amount.compareTo(ESCALATION_CREDIT_APPROVER_THRESHOLD) > 0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Get escalation message template based on priority and role
     *
     * @param targetRole The role being escalated to
     * @param priority The escalation priority
     * @return Message template
     */
    public String getEscalationMessage(String targetRole, String priority) {
        switch (priority) {
            case "URGENT":
                return String.format("URGENT: Application requires immediate %s approval", targetRole);
            case "HIGH":
                return String.format("HIGH PRIORITY: Application requires %s approval within 4 hours", targetRole);
            case "MEDIUM":
                return String.format("Application requires %s approval within 24 hours", targetRole);
            default:
                return String.format("Application pending %s approval", targetRole);
        }
    }


    // ========== HELPER METHODS ==========
    
    public String getRoleDisplayName(String role) {
        switch (role) {
            case "LOAN_OFFICER": return "Loan Officer";
            case "CREDIT_OFFICER": return "Credit Officer";
            case "BRANCH_MANAGER": return "Branch Manager";
            case "REGIONAL_MANAGER": return "Regional Manager";
            case "RISK_MANAGER": return "Risk Manager";
            case "CREDIT_COMMITTEE": return "Credit Committee";
            case "SUPER_ADMIN": return "Super Administrator";
            default: return role;
        }
    }

    public String getStatusDisplayName(String status) {
        switch (status) {
            case "APPROVED": return "Approved";
            case "REJECTED": return "Rejected";
            case "PENDING": return "Pending";
            case "RETURNED_FOR_REVISION": return "Returned for Revision";
            default: return status;
        }
    }

    public String getStatusColor(String status) {
        switch (status) {
            case "APPROVED": return "success";
            case "REJECTED": return "danger";
            case "PENDING": return "warning";
            case "RETURNED_FOR_REVISION": return "info";
            default: return "secondary";
        }
    }

    /**
     * Calculate SLA status
     */
    public SLAStatusDto calculateSLAStatus(LoanApplication application) {
        LocalDateTime submissionDate = application.getSubmittedDate();
        LocalDateTime now = LocalDateTime.now();

        long totalSlaHours = APPROVAL_SLA_HOURS;
        long hoursElapsed = ChronoUnit.HOURS.between(submissionDate, now);
        long hoursRemaining = Math.max(0, totalSlaHours - hoursElapsed);
        double completionPercentage = Math.min(100.0, (double) hoursElapsed / totalSlaHours * 100);

        String slaStatus;
        if (hoursRemaining <= 0) {
            slaStatus = "BREACHED";
        } else if (hoursRemaining <= 8) {
            slaStatus = "AT_RISK";
        } else {
            slaStatus = "ON_TRACK";
        }

        LocalDateTime slaDueDate = submissionDate.plusHours(totalSlaHours);

        return SLAStatusDto.builder()
                .applicationId(application.getId())
                .applicationNumber(application.getApplicationNumber())
                .slaLevel("STANDARD")
                .slaStartDate(submissionDate)
                .slaDueDate(slaDueDate)
                .hoursRemaining(hoursRemaining)
                .hoursElapsed(hoursElapsed)
                .status(slaStatus)
                .completionPercentage(completionPercentage)
                .nextAction("APPROVAL_DECISION")
                .nextActionDue(slaDueDate)
                .build();
    }

}