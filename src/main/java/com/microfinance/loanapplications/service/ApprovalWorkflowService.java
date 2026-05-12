package com.microfinance.loanapplications.service;

import com.microfinance.base.entity.User;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanapplications.dto.approval.ApprovalWorkflowDto;
import com.microfinance.loanapplications.dto.approval.ApprovalWorkflowStepDto;
import com.microfinance.loanapplications.entity.ApplicationApproval;
import com.microfinance.loanapplications.entity.LoanApplication;
import com.microfinance.loanapplications.repository.ApplicationApprovalRepository;
import com.microfinance.loanapplications.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final ApplicationApprovalRepository approvalRepository;

    public ApprovalWorkflowDto getApprovalWorkflow(Long applicationId, User currentUser) {
        log.info("Getting approval workflow for application: {}", applicationId);

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        return buildApprovalWorkflow(application, currentUser);
    }

    private ApprovalWorkflowDto buildApprovalWorkflow(LoanApplication application, User currentUser) {
        // Get existing approvals for this application
        List<ApplicationApproval> approvals = approvalRepository
                .findByLoanApplicationIdOrderByCreatedAtAsc(application.getId());

        // Determine workflow steps based on amount and product
        List<ApprovalWorkflowStepDto> workflowSteps = determineWorkflowSteps(application, approvals);

        // Calculate completed steps
        int completedSteps = (int) workflowSteps.stream()
                .filter(step -> "COMPLETED".equals(step.getStatus()) || "APPROVED".equals(step.getStatus()))
                .count();

        // Determine current stage and next approval role
        String currentStage = determineCurrentStage(workflowSteps);
        String nextApprovalRole = determineNextApprovalRole(workflowSteps);

        // Check if current user can approve
        boolean canCurrentUserApprove = canUserApproveCurrentStep(workflowSteps, currentUser);

        return ApprovalWorkflowDto.builder()
                .applicationId(application.getId())
                .applicationNumber(application.getApplicationNumber())
                .currentStatus(application.getStatus().name())
                .currentStage(currentStage)
                .totalSteps(workflowSteps.size())
                .completedSteps(completedSteps)
                .nextApprovalRole(nextApprovalRole)
                .workflowSteps(workflowSteps)
                .canCurrentUserApprove(canCurrentUserApprove)
                .currentUserRole(currentUser.getRole().name())
                .build();
    }

    private List<ApprovalWorkflowStepDto> determineWorkflowSteps(LoanApplication application,
                                                                 List<ApplicationApproval> approvals) {
        BigDecimal amount = application.getAppliedAmount();
        List<ApprovalWorkflowStepDto> steps = new ArrayList<>();

        // Define workflow steps based on amount thresholds
        if (amount.compareTo(BigDecimal.valueOf(5000)) <= 0) {
            // Small loans: Simple workflow
            steps.add(createWorkflowStep(1, "Document Verification", "Verify borrower documents",
                    "LOAN_OFFICER", application, approvals));
            steps.add(createWorkflowStep(2, "Branch Manager Approval", "Final approval by branch manager",
                    "BRANCH_MANAGER", application, approvals));

        } else if (amount.compareTo(BigDecimal.valueOf(25000)) <= 0) {
            // Medium loans: Enhanced workflow
            steps.add(createWorkflowStep(1, "Document Verification", "Verify borrower documents",
                    "LOAN_OFFICER", application, approvals));
            steps.add(createWorkflowStep(2, "Credit Assessment", "Assess creditworthiness",
                    "CREDIT_OFFICER", application, approvals));
            steps.add(createWorkflowStep(3, "Branch Manager Approval", "Final approval by branch manager",
                    "BRANCH_MANAGER", application, approvals));

        } else {
            // Large loans: Comprehensive workflow
            steps.add(createWorkflowStep(1, "Document Verification", "Verify borrower documents",
                    "LOAN_OFFICER", application, approvals));
            steps.add(createWorkflowStep(2, "Credit Assessment", "Assess creditworthiness",
                    "CREDIT_OFFICER", application, approvals));
            steps.add(createWorkflowStep(3, "Risk Assessment", "Evaluate risk factors",
                    "RISK_MANAGER", application, approvals));
            steps.add(createWorkflowStep(4, "Branch Manager Review", "Initial review by branch manager",
                    "BRANCH_MANAGER", application, approvals));
            steps.add(createWorkflowStep(5, "Credit Committee Approval", "Final approval by credit committee",
                    "CREDIT_COMMITTEE", application, approvals));
        }

        return steps;
    }

    private ApprovalWorkflowStepDto createWorkflowStep(int stepNumber, String stepName,
                                                       String stepDescription, String approvalRole,
                                                       LoanApplication application,
                                                       List<ApplicationApproval> approvals) {

        ApprovalWorkflowStepDto step = ApprovalWorkflowStepDto.builder()
                .stepNumber(stepNumber)
                .stepName(stepName)
                .stepDescription(stepDescription)
                .approvalRole(approvalRole)
                .status("PENDING")
                .build();

        // Find if there's an approval for this step
        approvals.stream()
                .filter(approval -> approval.getApprovalRole() != null &&
                        approval.getApprovalRole().equals(approvalRole))
                .findFirst()
                .ifPresent(approval -> {
                    step.setStatus(approval.getDecision().name());
                    step.setApproverName(approval.getApprover().getFullName());
                    step.setApproverUsername(approval.getApprover().getUsername());
                    step.setDecision(approval.getDecision().name());
                    step.setComments(approval.getComments());
                    if (approval.getDecisionDate() != null) {
                        step.setProcessedAt(approval.getDecisionDate().toEpochSecond(ZoneOffset.UTC));
                    }
                });

        // Set SLA deadline (2 days from submission for each step)
        if (application.getSubmittedDate() != null) {
            LocalDateTime deadline = application.getSubmittedDate()
                    .plusDays(2L * stepNumber);
            step.setSlaDeadline(deadline.toEpochSecond(ZoneOffset.UTC));

            // Check if overdue
            step.setOverdue(LocalDateTime.now().isAfter(deadline) &&
                    !"COMPLETED".equals(step.getStatus()) &&
                    !"APPROVED".equals(step.getStatus()));
        }

        // Determine if this is the current step
        step.setCurrentStep(isCurrentStep(step, approvals));

        return step;
    }

    private boolean isCurrentStep(ApprovalWorkflowStepDto step, List<ApplicationApproval> approvals) {
        // If step is completed, it's not current
        if ("COMPLETED".equals(step.getStatus()) || "APPROVED".equals(step.getStatus())) {
            return false;
        }

        // Find the highest completed step number
        int highestCompletedStep = approvals.stream()
                .filter(approval -> approval.getDecision() == GeneralConfig.ApprovalDecision.APPROVED)
                .mapToInt(approval -> {
                    // Map approval role to step number (simplified)
                    String role = approval.getApprovalRole();
                    if ("LOAN_OFFICER".equals(role)) return 1;
                    if ("CREDIT_OFFICER".equals(role)) return 2;
                    if ("RISK_MANAGER".equals(role)) return 3;
                    if ("BRANCH_MANAGER".equals(role)) return 4;
                    if ("CREDIT_COMMITTEE".equals(role)) return 5;
                    return 0;
                })
                .max()
                .orElse(0);

        // Current step is the next one after highest completed
        return step.getStepNumber() == highestCompletedStep + 1;
    }

    private String determineCurrentStage(List<ApprovalWorkflowStepDto> steps) {
        return steps.stream()
                .filter(ApprovalWorkflowStepDto::isCurrentStep)
                .findFirst()
                .map(ApprovalWorkflowStepDto::getStepName)
                .orElse(steps.stream()
                        .filter(step -> "PENDING".equals(step.getStatus()))
                        .findFirst()
                        .map(ApprovalWorkflowStepDto::getStepName)
                        .orElse("Completed"));
    }

    private String determineNextApprovalRole(List<ApprovalWorkflowStepDto> steps) {
        return steps.stream()
                .filter(step -> "PENDING".equals(step.getStatus()) || "IN_PROGRESS".equals(step.getStatus()))
                .findFirst()
                .map(ApprovalWorkflowStepDto::getApprovalRole)
                .orElse(null);
    }

    private boolean canUserApproveCurrentStep(List<ApprovalWorkflowStepDto> steps, User user) {
        return steps.stream()
                .filter(ApprovalWorkflowStepDto::isCurrentStep)
                .findFirst()
                .map(step -> user.getRole().name().equals(step.getApprovalRole()))
                .orElse(false);
    }

    public void updateWorkflowAfterApproval(Long applicationId, String approvalRole,
                                            String decision, String comments, User approver) {
        log.info("Updating workflow after approval for application {} by {}",
                applicationId, approver.getUsername());

        // Implementation would update workflow status
        // This could involve updating a workflow tracking entity
    }
}