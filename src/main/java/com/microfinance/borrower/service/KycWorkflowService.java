package com.microfinance.borrower.service;

import com.microfinance.borrower.dto.*;
import com.microfinance.borrower.entity.*;
import com.microfinance.borrower.enums.KycWorkflowState;
import com.microfinance.borrower.enums.KycWorkflowStep;
import com.microfinance.borrower.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycWorkflowService {
    
    private final KycWorkflowRepository kycWorkflowRepository;
    private final KycWorkflowHistoryRepository kycWorkflowHistoryRepository;
    private final KycWorkflowStepStatusRepository kycWorkflowStepStatusRepository;
    private final BorrowerRepository borrowerRepository;
    private final BorrowerDocumentRepository borrowerDocumentRepository;

    // START KYC PROCESS
    @Transactional
    public KycWorkflowDto startKycProcess(Long borrowerId, Long initiatedBy, String initiatedByName) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new RuntimeException("Borrower not found: " + borrowerId));

        // Check if workflow already exists
        if (kycWorkflowRepository.findByBorrowerId(borrowerId).isPresent()) {
            throw new RuntimeException("KYC workflow already exists for borrower: " + borrowerId);
        }

        // Create new workflow
        KycWorkflow workflow = new KycWorkflow();
        workflow.setBorrower(borrower);
        workflow.setCurrentState(KycWorkflowState.INITIATED);
        workflow.setStartedAt(LocalDateTime.now());
        workflow.setWorkflowVersion("1.0");
        
        KycWorkflow savedWorkflow = kycWorkflowRepository.save(workflow);

        // Create workflow steps
        createWorkflowSteps(savedWorkflow);

        // Add to history
        addWorkflowHistory(savedWorkflow, KycWorkflowState.NOT_STARTED, KycWorkflowState.INITIATED,
                          "KYC Process Started", initiatedBy, initiatedByName, "Initial KYC process initiation");

        return convertToDto(savedWorkflow);
    }

    // UPDATE KYC STATUS
    @Transactional
    public KycWorkflowDto updateKycStatus(Long borrowerId, KycWorkflowUpdateRequest request, 
                                         Long updatedBy, String updatedByName) {
        KycWorkflow workflow = kycWorkflowRepository.findByBorrowerId(borrowerId)
                .orElseThrow(() -> new RuntimeException("KYC workflow not found for borrower: " + borrowerId));

        KycWorkflowState newState = KycWorkflowState.valueOf(request.getNewState());
        KycWorkflowState oldState = workflow.getCurrentState();

        // Validate state transition
        validateStateTransition(oldState, newState);

        // Update workflow
        workflow.setPreviousState(oldState);
        workflow.setCurrentState(newState);
        
        if (request.getAssignedOfficerName() != null) {
            workflow.setAssignedOfficerName(request.getAssignedOfficerName());
        }
        if (request.getEstimatedCompletion() != null) {
            workflow.setEstimatedCompletionDate(request.getEstimatedCompletion());
        }
        if (request.getNotes() != null) {
            workflow.setNotes(request.getNotes());
        }

        // If moving to terminal state, set completion date
        if (newState.isTerminalState()) {
            workflow.setCompletedAt(LocalDateTime.now());
        }

        KycWorkflow updatedWorkflow = kycWorkflowRepository.save(workflow);

        // Add to history
        addWorkflowHistory(updatedWorkflow, oldState, newState, 
                          "Status Updated", updatedBy, updatedByName, request.getNotes());

        return convertToDto(updatedWorkflow);
    }

    // UPDATE WORKFLOW STEP
    @Transactional
    public KycWorkflowStepStatusDto updateWorkflowStep(Long borrowerId, Long stepId, 
                                                      StepUpdateRequest request, 
                                                      Long updatedBy, String updatedByName) {
        KycWorkflowStepStatus step = kycWorkflowStepStatusRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Workflow step not found: " + stepId));

        // Validate step belongs to borrower's workflow
        if (!step.getKycWorkflow().getBorrower().getId().equals(borrowerId)) {
            throw new RuntimeException("Step does not belong to borrower's workflow");
        }

        // Update step status
        KycWorkflowStepStatus.StepStatus newStatus = KycWorkflowStepStatus.StepStatus.valueOf(request.getNewStatus());
        KycWorkflowStepStatus.StepStatus oldStatus = step.getStatus();

        step.setStatus(newStatus);
        
        if (newStatus == KycWorkflowStepStatus.StepStatus.IN_PROGRESS && step.getStartedAt() == null) {
            step.setStartedAt(LocalDateTime.now());
        }
        
        if (newStatus == KycWorkflowStepStatus.StepStatus.COMPLETED) {
            step.setCompletedAt(LocalDateTime.now());
            step.setCompletedBy(updatedBy);
            step.setCompletedByName(updatedByName);
        }
        
        if (request.getNotes() != null) {
            step.setNotes(request.getNotes());
        }

        KycWorkflowStepStatus updatedStep = kycWorkflowStepStatusRepository.save(step);

        // Check if we can auto-progress workflow state
        autoProgressWorkflowState(step.getKycWorkflow());

        return convertToStepStatusDto(updatedStep);
    }

    // GET KYC WORKFLOW DETAILS
    @Transactional(readOnly = true)
    public KycWorkflowDto getKycWorkflow(Long borrowerId) {
        KycWorkflow workflow = kycWorkflowRepository.findByBorrowerId(borrowerId)
                .orElseThrow(() -> new RuntimeException("KYC workflow not found for borrower: " + borrowerId));
        
        return convertToDto(workflow);
    }

    // GET KYC WORKFLOW STEPS
    @Transactional(readOnly = true)
    public List<KycWorkflowStepStatusDto> getKycWorkflowSteps(Long borrowerId) {
        KycWorkflow workflow = kycWorkflowRepository.findByBorrowerId(borrowerId)
                .orElseThrow(() -> new RuntimeException("KYC workflow not found for borrower: " + borrowerId));
        
        return workflow.getStepStatuses().stream()
                .sorted((s1, s2) -> Integer.compare(s1.getStep().getOrder(), s2.getStep().getOrder()))
                .map(this::convertToStepStatusDto)
                .collect(Collectors.toList());
    }

    // HELPER METHODS
    private void createWorkflowSteps(KycWorkflow workflow) {
        List<KycWorkflowStepStatus> steps = Arrays.stream(KycWorkflowStep.values())
                .map(step -> {
                    KycWorkflowStepStatus stepStatus = new KycWorkflowStepStatus();
                    stepStatus.setKycWorkflow(workflow);
                    stepStatus.setStep(step);
                    stepStatus.setStatus(KycWorkflowStepStatus.StepStatus.PENDING);
                    stepStatus.setIsRequired(step.isRequired());
                    
                    // Set due dates based on step order (e.g., 2 days per step)
                    if (step.getOrder() > 1) {
                        stepStatus.setDueDate(LocalDateTime.now().plusDays(step.getOrder() * 2L));
                    }
                    
                    return stepStatus;
                })
                .collect(Collectors.toList());
        
        kycWorkflowStepStatusRepository.saveAll(steps);
    }

    private void addWorkflowHistory(KycWorkflow workflow, KycWorkflowState fromState, 
                                   KycWorkflowState toState, String action, 
                                   Long performedBy, String performedByName, String notes) {
        KycWorkflowHistory history = new KycWorkflowHistory();
        history.setKycWorkflow(workflow);
        history.setFromState(fromState);
        history.setToState(toState);
        history.setActionPerformed(action);
        history.setPerformedBy(performedBy);
        history.setPerformedByName(performedByName);
        history.setNotes(notes);
        history.setTransitionDate(LocalDateTime.now());
        
        kycWorkflowHistoryRepository.save(history);
    }

    private void validateStateTransition(KycWorkflowState fromState, KycWorkflowState toState) {
        // Implement your business rules for state transitions
        if (fromState.isTerminalState()) {
            throw new RuntimeException("Cannot transition from terminal state: " + fromState);
        }
        
        // Add more validation rules as needed
    }

    private void autoProgressWorkflowState(KycWorkflow workflow) {
        List<KycWorkflowStepStatus> steps = workflow.getStepStatuses();
        
        // Check if all required steps are completed
        boolean allRequiredStepsCompleted = steps.stream()
                .filter(step -> step.getIsRequired())
                .allMatch(step -> step.getStatus() == KycWorkflowStepStatus.StepStatus.COMPLETED);
        
        if (allRequiredStepsCompleted && workflow.getCurrentState() == KycWorkflowState.INITIATED) {
            // Auto-progress to UNDER_REVIEW
            workflow.setPreviousState(workflow.getCurrentState());
            workflow.setCurrentState(KycWorkflowState.UNDER_REVIEW);
            kycWorkflowRepository.save(workflow);
            
            addWorkflowHistory(workflow, KycWorkflowState.INITIATED, KycWorkflowState.UNDER_REVIEW,
                    "Auto-progress to Under Review", 0L, "System", 
                    "All required workflow steps completed");
        }
    }

    // DTO CONVERSION METHODS
    private KycWorkflowDto convertToDto(KycWorkflow workflow) {
        KycWorkflowDto dto = new KycWorkflowDto();
        dto.setId(workflow.getId());
        dto.setBorrowerId(workflow.getBorrower().getId());
        dto.setBorrowerName(workflow.getBorrower().getFirstName() + " " + workflow.getBorrower().getLastName());
        dto.setCurrentState(workflow.getCurrentState());
        dto.setPreviousState(workflow.getPreviousState());
        dto.setStartedAt(workflow.getStartedAt());
        dto.setCompletedAt(workflow.getCompletedAt());
        dto.setEstimatedCompletionDate(workflow.getEstimatedCompletionDate());
        dto.setNotes(workflow.getNotes());
        dto.setAssignedOfficerName(workflow.getAssignedOfficerName());
        dto.setDaysInProgress((int) workflow.getDaysInProgress());
        
        // Convert step statuses
        List<KycWorkflowStepStatusDto> stepDtos = workflow.getStepStatuses().stream()
                .sorted((s1, s2) -> Integer.compare(s1.getStep().getOrder(), s2.getStep().getOrder()))
                .map(this::convertToStepStatusDto)
                .collect(Collectors.toList());
        dto.setStepStatuses(stepDtos);
        
        // Calculate completion percentage
        long completedSteps = stepDtos.stream()
                .filter(step -> "COMPLETED".equals(step.getStatus()))
                .count();
       // dto.setCompletionPercentage((int) ((completedSteps * 100) / stepDtos.size()));
        dto.setCompletionPercentage(stepDtos.isEmpty() ? 0 : (int) Math.round((completedSteps * 100.0) / stepDtos.size()));
        
        // Get recent history (last 5 entries)
        List<KycWorkflowHistory> recentHistory = kycWorkflowHistoryRepository
                .findTop5ByKycWorkflowIdOrderByTransitionDateDesc(workflow.getId());
        dto.setRecentHistory(recentHistory.stream()
                .map(this::convertToHistoryDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private KycWorkflowStepStatusDto convertToStepStatusDto(KycWorkflowStepStatus step) {
        KycWorkflowStepStatusDto dto = new KycWorkflowStepStatusDto();
        dto.setId(step.getId());
        dto.setStep(step.getStep());
        dto.setStatus(step.getStatus().name());
        dto.setStartedAt(step.getStartedAt());
        dto.setCompletedAt(step.getCompletedAt());
        dto.setCompletedByName(step.getCompletedByName());
        dto.setNotes(step.getNotes());
        dto.setDueDate(step.getDueDate());
        dto.setIsRequired(step.getIsRequired());
        dto.setIsOverdue(step.isOverdue());
        dto.setRetryCount(step.getRetryCount());
        return dto;
    }

    private KycWorkflowHistoryDto convertToHistoryDto(KycWorkflowHistory history) {
        KycWorkflowHistoryDto dto = new KycWorkflowHistoryDto();
        dto.setId(history.getId());
        dto.setFromState(history.getFromState());
        dto.setToState(history.getToState());
        dto.setActionPerformed(history.getActionPerformed());
        dto.setPerformedByName(history.getPerformedByName());
        dto.setNotes(history.getNotes());
        dto.setTransitionDate(history.getTransitionDate());
        return dto;
    }

    // Request DTOs
    @Data
    public static class KycWorkflowUpdateRequest {
        private String newState;
        private String assignedOfficerName;
        private LocalDateTime estimatedCompletion;
        private String notes;
    }

    @Data
    public static class StepUpdateRequest {
        private String newStatus;
        private String notes;
    }
}