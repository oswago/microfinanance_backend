package com.microfinance.borrower.service;

import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.borrower.dto.*;
import com.microfinance.borrower.entity.*;
import com.microfinance.borrower.enums.KycWorkflowState;
import com.microfinance.borrower.enums.KycWorkflowStep;
import com.microfinance.borrower.repository.*;
import com.microfinance.common.config.DocumentConfig;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.common.service.DocumentConfigService;
import com.microfinance.common.util.CommonUtil;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.loanproducts.repository.LoanProductRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.microfinance.base.utils.SecurityUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.microfinance.borrower.dto.KycWorkflowStepDto.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycWorkflowService {

    @Value("${app.system.default-use-case:BASIC-KYC}")
    private String def_usecase_name;
    
    private final KycWorkflowRepository kycWorkflowRepository;
    private final KycWorkflowHistoryRepository kycWorkflowHistoryRepository;
    private final KycWorkflowStepStatusRepository kycWorkflowStepStatusRepository;
    private final BorrowerRepository borrowerRepository;
    private final BorrowerDocumentRepository borrowerDocumentRepository;
    private final DocumentConfigService documentConfigService;
    private final SecurityUtils securityUtils;
    private final LoanProductRepository loanProductRepository;

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
        createWorkflowSteps(savedWorkflow,def_usecase_name);

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

        //update steps RISK_ASSESSMENT,OFFICER_APPROVAL,MANAGER_APPROVAL,KYC_COMPLETION
        updateWorkflowStepRemainingStatus(workflow.getId(), String.valueOf(newState));

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
       // autoProgressWorkflowState(step.getKycWorkflow());
        boolean progressed = enhancedAutoProgressWorkflowState(step.getKycWorkflow(),
                securityUtils.getCurrentUserId(), securityUtils.getCurrentUsername());

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

    // HELPER METHOD

    private void createWorkflowSteps(KycWorkflow workflow, String useCaseName) {
        log.info("=== Creating workflow steps for use case: {} ===", useCaseName);
        // Get required documents for the use case
       // Set<DocumentConfig.DocumentType> requiredDocuments = documentConfigService.getRequiredDocumentsForUseCase(useCaseName);

        Set<DocumentConfig.DocumentType> requiredDocuments = null;
        // Check if borrower has a loan product with required documents
        if (workflow.getBorrower() != null && workflow.getBorrower().getLoanProductId() != null) {
            Optional<LoanProduct> loanProductOpt = loanProductRepository.findById(workflow.getBorrower().getLoanProductId());

            if (loanProductOpt.isPresent()) {
                LoanProduct loanProduct = loanProductOpt.get();
                String requiredDocsJson = loanProduct.getRequiredDocuments();

                if (requiredDocsJson != null && !requiredDocsJson.trim().isEmpty()) {
                    try {
                        // Parse the JSON string to get required documents
                        requiredDocuments = CommonUtil.parseRequiredDocuments(requiredDocsJson);
                        log.info("Using loan product's required documents: {}", requiredDocsJson);
                    } catch (Exception e) {
                        log.warn("Failed to parse required documents from loan product. Falling back to use case defaults.", e);
                    }
                }
            }
        }

        // If no loan product documents or parsing failed, use default use case documents
        if (requiredDocuments == null || requiredDocuments.isEmpty()) {
            requiredDocuments = documentConfigService.getRequiredDocumentsForUseCase(useCaseName);
            log.info("Using default required documents for use case '{}'", useCaseName);
        }

        log.info("Required documents for use case '{}': {}", useCaseName, requiredDocuments);
        // Convert DocumentType to string keys used in your mapping
        Set<String> requiredDocumentTypes = requiredDocuments.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        log.info("Required document types: {}", requiredDocumentTypes);

        // Get document-step mapping
        Map<String, List<KycWorkflowStep>> documentStepMap = documentConfigService.getDocumentStepMap();

        // Collect all steps required by the documents
        Set<KycWorkflowStep> documentRelatedSteps = requiredDocumentTypes.stream()
                .filter(documentStepMap::containsKey)
                .flatMap(docType -> {
                    List<KycWorkflowStep> stepsForDoc = documentStepMap.get(docType);
                    log.info("Document '{}' requires steps: {}", docType, stepsForDoc);
                    return stepsForDoc.stream();
                })
                .collect(Collectors.toSet());
        log.info("Document-related steps: {}", documentRelatedSteps);
        // Define compulsory steps that should always be included
        Set<KycWorkflowStep> compulsorySteps =documentConfigService.getCompulsorySteps();
        log.info("Compulsory steps: {}", compulsorySteps);

        // Combine document-related steps and compulsory steps
        Set<KycWorkflowStep> allRequiredSteps = new HashSet<>();
        allRequiredSteps.addAll(compulsorySteps);
        allRequiredSteps.addAll(documentRelatedSteps);
        log.info("All required steps: {}", allRequiredSteps);

        // Create step statuses only for required steps
        List<KycWorkflowStepStatus> steps = allRequiredSteps.stream()
                .sorted(Comparator.comparingInt(KycWorkflowStep::getOrder))
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

                    log.debug("Created step: {} (Order: {}, Required: {})",
                            step, step.getOrder(), step.isRequired());
                    return stepStatus;
                })
                .collect(Collectors.toList());

        kycWorkflowStepStatusRepository.saveAll(steps);

        log.info("=== Created {} workflow steps for use case '{}' ===", steps.size(), useCaseName);
        steps.forEach(step ->
                log.info("Step: {} (Order: {}, Required: {})",
                        step.getStep(), step.getStep().getOrder(), step.getIsRequired()));
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

    /**
     * Get all workflow steps required for the given document types
     */
    public Set<KycWorkflowStep> getWorkflowStepsForDocumentTypes(Set<DocumentConfig.DocumentType> documentTypes) {
        Set<KycWorkflowStep> allSteps = new HashSet<>();
        // Your existing mapping - convert to use DocumentType if needed, or keep as string mapping
        Map<String, List<KycWorkflowStep>> documentStepMap = documentConfigService.getDocumentStepMap();
        for (DocumentConfig.DocumentType docType : documentTypes) {
            String docTypeName = docType.name(); // or get the string representation
            List<KycWorkflowStep> steps = documentStepMap.get(docTypeName);
            if (steps != null) {
                allSteps.addAll(steps);
            }
        }
        return allSteps;
    }

    // SYNC DOCUMENTS WITH WORKFLOW
    @Transactional
    public DocumentSyncResponse syncDocumentsWithWorkflow(Long borrowerId, List<Long> documentIds,
                                                          Long userId, String userName) {
        try {
            KycWorkflow workflow = kycWorkflowRepository.findByBorrowerId(borrowerId)
                    .orElseThrow(() -> new RuntimeException("KYC workflow not found for borrower: " + borrowerId));

            // Get all borrowers odocuments
            List<BorrowerDocument> documents = borrowerDocumentRepository.findAllById(documentIds);

            // Update UPLOAD step status for newly uploaded documents
            updateUploadStepsForNewDocumentsSimple(workflow, documents, userId, userName);

            List<BorrowerDocument> verifiedDocuments = documents.stream()
                    .filter(doc -> DocumentConfig.DocumentStatus.VERIFIED.equals(doc.getStatus()))
                    .collect(Collectors.toList());

            log.info("Syncing {} verified documents with workflow for borrower {}", verifiedDocuments.size(), borrowerId);

            int stepsCompleted = 0;
            int stepsSkipped = 0;
            List<String> completedStepNames = new ArrayList<>();

            // Get all workflow steps
            List<KycWorkflowStepStatus> steps = workflow.getStepStatuses();

            for (KycWorkflowStepStatus step : steps) {
                if (step.getStatus() == KycWorkflowStepStatus.StepStatus.PENDING) {
                    boolean shouldComplete = shouldCompleteStep(step, verifiedDocuments);

                    if (shouldComplete) {
                        // Complete the step
                        step.setStatus(KycWorkflowStepStatus.StepStatus.COMPLETED);
                        step.setCompletedAt(LocalDateTime.now());
                        step.setCompletedBy(userId);
                        step.setCompletedByName(userName);
                        step.setNotes("Auto-completed based on verified documents: " +
                                verifiedDocuments.stream()
                                        .map(BorrowerDocument::getDocumentName)
                                        .collect(Collectors.joining(", ")));

                        kycWorkflowStepStatusRepository.save(step);
                        stepsCompleted++;
                        completedStepNames.add(step.getStep().name());

                        log.info("Auto-completed step {} for borrower {}", step.getStep(), borrowerId);
                    } else {
                        stepsSkipped++;
                    }
                }
            }

            // Check for auto-progression after step completion
            if (stepsCompleted > 0) {
               // autoProgressWorkflowState(workflow);
                boolean progressed = enhancedAutoProgressWorkflowState(workflow,
                        securityUtils.getCurrentUserId(), securityUtils.getCurrentUsername());
            }

            DocumentSyncResponse response = new DocumentSyncResponse();
            response.setSuccess(true);
            response.setMessage(String.format("Synced %d documents, completed %d steps",
                    verifiedDocuments.size(), stepsCompleted));
            response.setStepsCompleted(stepsCompleted);
            response.setStepsSkipped(stepsSkipped);
            response.setCompletedStepNames(completedStepNames);

            return response;

        } catch (Exception e) {
            log.error("Error syncing documents with workflow for borrower {}: {}", borrowerId, e.getMessage());

            DocumentSyncResponse response = new DocumentSyncResponse();
            response.setSuccess(false);
            response.setMessage("Failed to sync documents: " + e.getMessage());
            response.setStepsCompleted(0);
            response.setStepsSkipped(0);
            response.setCompletedStepNames(new ArrayList<>());

            return response;
        }
    }

    // AUTO-COMPLETE STEPS
    @Transactional
    public AutoCompleteResponse autoCompleteSteps(Long borrowerId, Long userId, String userName) {
        try {
            KycWorkflow workflow = kycWorkflowRepository.findByBorrowerId(borrowerId)
                    .orElseThrow(() -> new RuntimeException("KYC workflow not found for borrower: " + borrowerId));

            // Get all verified documents for this borrower
            List<BorrowerDocument> verifiedDocuments = borrowerDocumentRepository
                    .findByBorrowerIdAndStatus(borrowerId, DocumentConfig.DocumentStatus.VERIFIED);

            log.info("Auto-completing steps for borrower {} with {} verified documents",
                    borrowerId, verifiedDocuments.size());

            int stepsCompleted = 0;
            List<String> completedSteps = new ArrayList<>();

            List<KycWorkflowStepStatus> steps = workflow.getStepStatuses();

            for (KycWorkflowStepStatus step : steps) {
                if (step.getStatus() == KycWorkflowStepStatus.StepStatus.PENDING &&
                        shouldCompleteStep(step, verifiedDocuments)) {

                    step.setStatus(KycWorkflowStepStatus.StepStatus.COMPLETED);
                    step.setCompletedAt(LocalDateTime.now());
                    step.setCompletedBy(userId);
                    step.setCompletedByName(userName);
                    step.setNotes("Auto-completed via bulk auto-complete");

                    kycWorkflowStepStatusRepository.save(step);
                    stepsCompleted++;
                    completedSteps.add(step.getStep().name());
                }
            }

            // Auto-progress workflow if steps were completed
            if (stepsCompleted > 0) {
                //autoProgressWorkflowState(workflow);
                boolean progressed = enhancedAutoProgressWorkflowState(workflow,
                        securityUtils.getCurrentUserId(), securityUtils.getCurrentUsername());
            }

            AutoCompleteResponse response = new AutoCompleteResponse();
            response.setSuccess(true);
            response.setMessage(String.format("Auto-completed %d steps", stepsCompleted));
            response.setStepsCompleted(stepsCompleted);
            response.setCompletedSteps(completedSteps);

            return response;

        } catch (Exception e) {
            log.error("Error auto-completing steps for borrower {}: {}", borrowerId, e.getMessage());

            AutoCompleteResponse response = new AutoCompleteResponse();
            response.setSuccess(false);
            response.setMessage("Failed to auto-complete steps: " + e.getMessage());
            response.setStepsCompleted(0);
            response.setCompletedSteps(new ArrayList<>());

            return response;
        }
    }

    // CHECK AUTO-PROGRESS
    @Transactional
    public AutoProgressResponse checkAutoProgress(Long borrowerId, Long userId, String userName) {
        try {
            KycWorkflow workflow = kycWorkflowRepository.findByBorrowerId(borrowerId)
                    .orElseThrow(() -> new RuntimeException("KYC workflow not found for borrower: " + borrowerId));

            KycWorkflowState oldState = workflow.getCurrentState();

            // Enhanced auto-progression logic
            boolean progressed = enhancedAutoProgressWorkflowState(workflow, userId, userName);

            AutoProgressResponse response = new AutoProgressResponse();
            response.setProgressed(progressed);
            response.setFromState(oldState.name());
            response.setToState(workflow.getCurrentState().name());
            response.setMessage(progressed ?
                    "Workflow auto-progressed from " + oldState + " to " + workflow.getCurrentState() :
                    "No auto-progression possible at this time");

            return response;

        } catch (Exception e) {
            log.error("Error checking auto-progress for borrower {}: {}", borrowerId, e.getMessage());

            AutoProgressResponse response = new AutoProgressResponse();
            response.setProgressed(false);
            response.setMessage("Error checking auto-progress: " + e.getMessage());

            return response;
        }
    }

    // GET DOCUMENT STEP MAPPING
    public Map<String, List<String>> getDocumentStepMapping(Long borrowerId) {
        Map<String, List<String>> mapping = new HashMap<>();

        documentConfigService.getDocumentStepMap().forEach((docType, steps) -> {
            List<String> stepNames = steps.stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
            mapping.put(docType, stepNames);
        });

        return mapping;
    }

// ENHANCED STEP COMPLETION LOGIC
    private boolean shouldCompleteStep(KycWorkflowStepStatus step, List<BorrowerDocument> verifiedDocuments) {
        log.debug("Checking if step {} should be completed", step.getStep());

        // Get the document step map once
        Map<String, List<KycWorkflowStep>> documentStepMap = documentConfigService.getDocumentStepMap();

        // Find document types that can complete this step
        List<String> documentTypesForStep = documentStepMap.entrySet().stream()
                .filter(entry -> entry.getValue().contains(step.getStep()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        log.debug("Step {} can be completed by document types: {}", step.getStep(), documentTypesForStep);
        log.debug("Available verified documents: {}", verifiedDocuments.stream()
                .map(doc -> doc.getDocumentType().name())
                .collect(Collectors.toList()));

        // Check if we have verified documents that match any of the required document types
        boolean hasRequiredDocuments = verifiedDocuments.stream()
                .anyMatch(doc -> documentTypesForStep.contains(doc.getDocumentType().name()));

        log.debug("Has required documents for step {}: {}", step.getStep(), hasRequiredDocuments);

        if (!hasRequiredDocuments) {
            return false;
        }
        // For verification steps, ensure corresponding upload step is completed
        if (step.getStep().name().startsWith("VERIFY_")) {
            String uploadStepName = step.getStep().name().replace("VERIFY_", "UPLOAD_");
            KycWorkflowStep uploadStep = Arrays.stream(KycWorkflowStep.values())
                    .filter(s -> s.name().equals(uploadStepName))
                    .findFirst()
                    .orElse(null);

            if (uploadStep != null) {
                boolean uploadStepCompleted = step.getKycWorkflow().getStepStatuses().stream()
                        .filter(s -> s.getStep() == uploadStep)
                        .anyMatch(s -> s.getStatus() == KycWorkflowStepStatus.StepStatus.COMPLETED);

                log.debug("Upload step {} completed for verification step {}: {}",
                        uploadStep, step.getStep(), uploadStepCompleted);

                if (!uploadStepCompleted) {
                    log.debug("Cannot complete {} - corresponding upload step {} not completed",
                            step.getStep(), uploadStep);
                    return false;
                }
            }
        }
        log.debug("Step {} can be completed", step.getStep());
        return true;
    }

    // ENHANCED AUTO-PROGRESSION LOGIC
    private boolean enhancedAutoProgressWorkflowState(KycWorkflow workflow, Long userId, String userName) {
        KycWorkflowState currentState = workflow.getCurrentState();
        KycWorkflowState newState = null;

        // Check step completion for state progression
        List<KycWorkflowStepStatus> steps = workflow.getStepStatuses();
        long completedSteps = steps.stream()
                .filter(step -> step.getStatus() == KycWorkflowStepStatus.StepStatus.COMPLETED)
                .count();
        long totalRequiredSteps = steps.stream()
                .filter(KycWorkflowStepStatus::getIsRequired)
                .count();

        // State progression rules
        switch (currentState) {
            case INITIATED:
                // Move to DOCUMENT_COLLECTION when workflow starts
                if (completedSteps > 0) {
                    newState = KycWorkflowState.DOCUMENT_COLLECTION;
                }
                break;

            case DOCUMENT_COLLECTION:
                // Move to UNDER_REVIEW when all document upload steps are complete
                boolean allUploadStepsComplete = steps.stream()
                        .filter(step -> step.getStep().name().startsWith("UPLOAD_"))
                        .allMatch(step -> step.getStatus() == KycWorkflowStepStatus.StepStatus.COMPLETED);
                if (allUploadStepsComplete) {
                    newState = KycWorkflowState.UNDER_REVIEW;
                }
                break;

            case UNDER_REVIEW:
                // Move to VERIFICATION_IN_PROGRESS when ready for verification
                boolean readyForVerification = steps.stream()
                        .filter(step -> step.getStep().name().startsWith("UPLOAD_"))
                        .allMatch(step -> step.getStatus() == KycWorkflowStepStatus.StepStatus.COMPLETED);
                if (readyForVerification) {
                    newState = KycWorkflowState.VERIFICATION_IN_PROGRESS;
                }
                break;

            case VERIFICATION_IN_PROGRESS:
                // Move to PENDING_APPROVAL when all verification steps are complete
                boolean allVerificationComplete = steps.stream()
                        .filter(step -> step.getStep().name().startsWith("VERIFY_"))
                        .allMatch(step -> step.getStatus() == KycWorkflowStepStatus.StepStatus.COMPLETED);
                if (allVerificationComplete) {
                    newState = KycWorkflowState.PENDING_APPROVAL;
                }
                break;

            case PENDING_APPROVAL:
                // Move to APPROVED when all approval steps are complete
                boolean allApprovalComplete = steps.stream()
                        .filter(step -> step.getStep().name().contains("APPROVAL"))
                        .allMatch(step -> step.getStatus() == KycWorkflowStepStatus.StepStatus.COMPLETED);
                if (allApprovalComplete) {
                    newState = KycWorkflowState.APPROVED;
                }
                break;

            case APPROVED:
                // Move to VERIFIED when fully approved
                if (completedSteps == totalRequiredSteps) {
                    newState = KycWorkflowState.VERIFIED;
                }
                break;
        }

        if (newState != null && newState != currentState) {
            workflow.setPreviousState(currentState);
            workflow.setCurrentState(newState);
            kycWorkflowRepository.save(workflow);

            // Add to history
            addWorkflowHistory(workflow, currentState, newState,
                    "Auto-progressed by system", userId, userName,
                    String.format("Auto-progressed from %s to %s based on step completion",
                            currentState, newState));

            log.info("Auto-progressed workflow for borrower {} from {} to {}",
                    workflow.getBorrower().getId(), currentState, newState);
            return true;
        }

        return false;
    }

    @Transactional
    public void autoCompleteStepsFromDocuments(Long borrowerId, List<Long> documentIds, Long userId, String userName) {
        try {
            log.info("=== START autoCompleteStepsFromDocuments ===");
            log.info("Parameters - borrowerId: {}, documentIds: {}, userId: {}, userName: {}",
                    borrowerId, documentIds, userId, userName);

            // Use a custom query to avoid loading entire entity graph
            KycWorkflow workflow = kycWorkflowRepository.findByBorrowerIdWithSteps(borrowerId)
                    .orElseThrow(() -> {
                        log.error("KYC workflow not found for borrower: {}", borrowerId);
                        return new RuntimeException("KYC workflow not found for borrower: " + borrowerId);
                    });

            log.info("Found workflow: ID={}, Current State={}, Steps Count={}",
                    workflow.getId(), workflow.getCurrentState(),
                    workflow.getStepStatuses() != null ? workflow.getStepStatuses().size() : 0);

            // Get documents without loading entire borrower graph
            List<BorrowerDocument> documents = borrowerDocumentRepository.findDocumentsByIdsWithoutRelations(documentIds);
            log.info("Found {} documents for IDs: {}", documents.size(), documentIds);

            documents.forEach(doc ->
                    log.info("Document - ID: {}, Type: {}, Status: {}",
                            doc.getId(), doc.getDocumentType(), doc.getStatus()));

            List<BorrowerDocument> verifiedDocuments = documents.stream()
                    .filter(doc -> DocumentConfig.DocumentStatus.VERIFIED.equals(doc.getStatus()))
                    .collect(Collectors.toList());

            log.info("Verified documents count: {}", verifiedDocuments.size());
            verifiedDocuments.forEach(doc ->
                    log.info("Verified Document - ID: {}, Type: {}", doc.getId(), doc.getDocumentType()));

            // Cache the document step map to avoid multiple service calls
            Map<String, List<KycWorkflowStep>> documentStepMap = documentConfigService.getDocumentStepMapForUseCase(def_usecase_name,borrowerId);

            log.info("Document step mapping configured with {} document types", documentStepMap.size());
            documentStepMap.forEach((docType, steps) ->
                    log.info("Document type '{}' maps to steps: {}", docType, steps));

            // Auto-complete steps based on verified documents
            int pendingStepsCount = 0;
            int completedStepsCount = 0;

            if (workflow.getStepStatuses() == null || workflow.getStepStatuses().isEmpty()) {
                log.warn("No step statuses found in workflow!");
                return;
            }
            log.info("Processing {} step statuses", workflow.getStepStatuses().size());

            for (KycWorkflowStepStatus step : workflow.getStepStatuses()) {
                log.info("Processing step: {}, Current Status: {}", step.getStep(), step.getStatus());

                if (step.getStatus() == KycWorkflowStepStatus.StepStatus.PENDING) {
                    pendingStepsCount++;
                    log.info("Step {} is PENDING - checking if it should be completed", step.getStep());

                    // Find which document types can complete this step
                    List<String> documentTypesForStep = documentStepMap.entrySet().stream()
                            .filter(entry -> entry.getValue().contains(step.getStep()))
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList());

                    log.info("Step {} can be completed by document types: {}", step.getStep(), documentTypesForStep);
                    // Check if any verified document matches
                   /* boolean shouldComplete = verifiedDocuments.stream()
                            .anyMatch(verifiedDoc -> documentTypesForStep.contains(verifiedDoc.getDocumentType()));*/

                    // In your autoCompleteStepsFromDocuments method, replace the complex logic with:
                    boolean shouldComplete = shouldCompleteStep(step, verifiedDocuments);
                    // Find which verified documents actually match for logging
                    List<String> matchingVerifiedDocs = verifiedDocuments.stream()
                            .filter(doc -> documentTypesForStep.contains(doc.getDocumentType().name()))
                            .map(doc -> doc.getDocumentType().name())
                            .collect(Collectors.toList());

                    log.info("Should complete step {}: {} (Matching verified docs: {})",
                            step.getStep(), shouldComplete, matchingVerifiedDocs);

                    if (shouldComplete) {
                        step.setStatus(KycWorkflowStepStatus.StepStatus.COMPLETED);
                        step.setCompletedAt(LocalDateTime.now());
                        step.setCompletedBy(userId);
                        step.setCompletedByName(userName);
                        step.setNotes("Auto-completed based on verified document upload");

                        log.info("Updating step {} to COMPLETED", step.getStep());

                        try {
                            KycWorkflowStepStatus savedStep = kycWorkflowStepStatusRepository.save(step);
                            log.info("Successfully saved step {} with new status: {}", savedStep.getStep(), savedStep.getStatus());
                            completedStepsCount++;
                        } catch (Exception e) {
                            log.error("Failed to save step {}: {}", step.getStep(), e.getMessage(), e);
                        }
                    } else {
                        log.info("No matching verified documents found for step {}", step.getStep());

                        // Debug: Show what verified documents we have
                        if (verifiedDocuments.isEmpty()) {
                            log.info("No verified documents available");
                        } else {
                            log.info("Available verified document types: {}",
                                    verifiedDocuments.stream()
                                            .map(BorrowerDocument::getDocumentType)
                                            .collect(Collectors.toList()));
                        }
                    }
                } else {
                    log.info("Step {} is already in status: {}", step.getStep(), step.getStatus());
                }
            }

            log.info("Step completion summary - Pending: {}, Completed: {}", pendingStepsCount, completedStepsCount);

            // Check if we can auto-progress the workflow
            log.info("Calling autoProgressWorkflowState for workflow ID: {}", workflow.getId());
            try {
               // autoProgressWorkflowState(workflow);
                // Enhanced auto-progression logic
                boolean progressed = enhancedAutoProgressWorkflowState(workflow, userId, userName);

                log.info("Successfully called autoProgressWorkflowState");
            } catch (Exception e) {
                log.error("Error in autoProgressWorkflowState: {}", e.getMessage(), e);
            }

            log.info("=== END autoCompleteStepsFromDocuments ===");

        } catch (Exception e) {
            log.error("=== ERROR in autoCompleteStepsFromDocuments ===");
            log.error("Error for borrower {}: {}", borrowerId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Simplified version for updating UPLOAD steps
     */
    private void updateUploadStepsForNewDocumentsSimple(KycWorkflow workflow, List<BorrowerDocument> documents,
                                                        Long userId, String userName) {
        // Filter newly uploaded PENDING documents
        List<BorrowerDocument> newDocuments = documents.stream()
                .filter(doc -> DocumentConfig.DocumentStatus.PENDING.equals(doc.getStatus()))
                .collect(Collectors.toList());

        for (BorrowerDocument document : newDocuments) {
            String documentType = String.valueOf(document.getDocumentType());

            // Map document type to UPLOAD step
            KycWorkflowStep uploadStep = mapDocumentTypeToUploadStep(documentType);
            if (uploadStep == null) {
                continue;
            }

            // Find the step status record
            Optional<KycWorkflowStepStatus> stepStatusOpt = kycWorkflowStepStatusRepository
                    .findByKycWorkflowIdAndStep(workflow.getId(), uploadStep);

            if (stepStatusOpt.isPresent()) {
                KycWorkflowStepStatus stepStatus = stepStatusOpt.get();

                if (stepStatus.getStatus() == KycWorkflowStepStatus.StepStatus.PENDING) {
                    stepStatus.setStatus(KycWorkflowStepStatus.StepStatus.COMPLETED);
                    stepStatus.setCompletedAt(LocalDateTime.now());
                    stepStatus.setCompletedBy(userId);
                    stepStatus.setCompletedByName(userName);
                    stepStatus.setNotes("Completed after document upload: " + document.getDocumentName());

                    kycWorkflowStepStatusRepository.save(stepStatus);

                    log.info("Marked UPLOAD step {} as COMPLETED for document {}", uploadStep, document.getDocumentName());
                }
            }

            // Find the step status record for INITIATE_KYC start
            Optional<KycWorkflowStepStatus> stepStatusOpt_initiate = kycWorkflowStepStatusRepository
                    .findByKycWorkflowIdAndStep(workflow.getId(), KycWorkflowStep.INITIATE_KYC);

            if (stepStatusOpt_initiate.isPresent()) {
                KycWorkflowStepStatus stepStatus = stepStatusOpt_initiate.get();

                if (stepStatus.getStatus() == KycWorkflowStepStatus.StepStatus.PENDING) {
                    stepStatus.setStatus(KycWorkflowStepStatus.StepStatus.COMPLETED);
                    stepStatus.setCompletedAt(LocalDateTime.now());
                    stepStatus.setCompletedBy(userId);
                    stepStatus.setCompletedByName(userName);
                    stepStatus.setNotes("Update Kyc Initiation Step: " + document.getDocumentName());
                    kycWorkflowStepStatusRepository.save(stepStatus);

                    log.info("Marked KYC INITIATE step {}", KycWorkflowStep.INITIATE_KYC);
                }
            }
            // Find the step status record for INITIATE_KYC End


        }


    }

    /**
     * Simple mapping from document type to UPLOAD step
     */
    private KycWorkflowStep mapDocumentTypeToUploadStep(String documentType) {
        switch (documentType.toUpperCase()) {
            case "NATIONAL_ID":
            case "PASSPORT":
            case "DRIVERS_LICENSE":
                return KycWorkflowStep.UPLOAD_ID_PROOF;
            case "UTILITY_BILL":
            case "BANK_STATEMENT":
            case "RENTAL_AGREEMENT":
                return KycWorkflowStep.UPLOAD_ADDRESS_PROOF;
            case "PAYSLIP":
            case "TAX_RETURN":
                return KycWorkflowStep.UPLOAD_INCOME_PROOF;
            case "PHOTOGRAPH":
                return KycWorkflowStep.UPLOAD_PHOTOGRAPH;
            default:
                return null;
        }
    }

    /**
     * Get workflow steps for document types WITH STATUS information from existing workflow
     */
    public Set<KycWorkflowStepStatusDto> getWorkflowStepsForDocumentTypesWithStatus(
            Set<DocumentConfig.DocumentType> documentTypes, Long borrowerId) {

        // Get document-based steps
        Set<KycWorkflowStep> requiredSteps = getWorkflowStepsForDocumentTypes(documentTypes);

        // Add compulsory approval steps
        Set<KycWorkflowStep> compulsorySteps = documentConfigService.getCompulsorySteps();
        Set<KycWorkflowStep> allSteps = new HashSet<>();
        allSteps.addAll(requiredSteps);
        allSteps.addAll(compulsorySteps);

        System.out.println("=== SERVICE DEBUG: Processing " + allSteps.size() + " total steps");
        System.out.println("Document Steps: " + requiredSteps.size());
        System.out.println("Compulsory Steps: " + compulsorySteps.size());
        System.out.println("Compulsory Steps List: " + compulsorySteps);

        try {
            // Use a custom query to fetch step statuses without triggering lazy loading
            List<KycWorkflowStepStatus> existingStepStatuses = kycWorkflowStepStatusRepository
                    .findByKycWorkflowBorrowerId(borrowerId);

            System.out.println("=== SERVICE DEBUG: Found " + existingStepStatuses.size() + " existing step statuses");

            return allSteps.stream()
                    .map(step -> {
                        Optional<KycWorkflowStepStatus> existingStatus = existingStepStatuses.stream()
                                .filter(status -> status.getStep() == step)
                                .findFirst();

                        KycWorkflowStepStatusDto dto = existingStatus.isPresent()
                                ? convertToStepStatusDto(existingStatus.get())
                                : createDefaultStepStatusDto(step, null);

                        // Log step details
                        if (compulsorySteps.contains(step)) {
                            System.out.println("=== COMPULSORY STEP: " + step + " -> " + dto.getStatus());
                        } else {
                            System.out.println("=== DOCUMENT STEP: " + step + " -> " + dto.getStatus());
                        }

                        return dto;
                    })
                    .collect(Collectors.<KycWorkflowStepStatusDto>toSet());

        } catch (RuntimeException e) {
            System.out.println("=== SERVICE DEBUG: No workflow found, using default steps");
            return allSteps.stream()
                    .map(step -> createDefaultStepStatusDto(step, null))
                    .collect(Collectors.<KycWorkflowStepStatusDto>toSet());
        }
    }


    public KycWorkflowStepStatusDto createDefaultStepStatusDto(KycWorkflowStep step, KycWorkflow workflow) {
        KycWorkflowStepStatusDto dto = new KycWorkflowStepStatusDto();
        dto.setStep(step); // This should be KycWorkflowStep enum, not String
        dto.setStatus(GeneralConfig.StepStatus.PENDING.name()); // Use your StepStatus enum
        dto.setStartedAt(null);
        dto.setCompletedAt(null);
        dto.setCompletedByName(null);
        dto.setNotes(null);

        if (workflow != null && workflow.getStartedAt() != null) {
            dto.setDueDate(workflow.getStartedAt().plusDays(7));
        } else {
            dto.setDueDate(LocalDateTime.now().plusDays(7));
        }

        dto.setIsRequired(true);
        dto.setIsOverdue(false);
        dto.setRetryCount(0);
        return dto;
    }
    /// ///End/////////////

    @Transactional
    public void updateWorkflowStepRemainingStatus(Long workflowId, String newState) {
        List<KycWorkflowStepStatus> stepsToUpdate = new ArrayList<>();

        try {
            if ("APPROVED".equals(newState)) {
                updateStepIfExists(workflowId, KycWorkflowStep.OFFICER_APPROVAL, stepsToUpdate);
            }
            else if ("VERIFIED".equals(newState)) {
                updateStepIfExists(workflowId, KycWorkflowStep.MANAGER_APPROVAL, stepsToUpdate);
                updateStepIfExists(workflowId, KycWorkflowStep.KYC_COMPLETION, stepsToUpdate);
                updateStepIfExists(workflowId, KycWorkflowStep.RISK_ASSESSMENT, stepsToUpdate);
            }
            else {
                log.warn("Unknown status received: {} for workflow: {}", newState, workflowId);
                return;
            }

            // Save all updated steps
            if (!stepsToUpdate.isEmpty()) {
                kycWorkflowStepStatusRepository.saveAll(stepsToUpdate);
                log.info("Successfully updated {} steps for workflow: {} with status: {}",
                        stepsToUpdate.size(), workflowId, newState);
            } else {
                log.warn("No steps were updated for workflow: {} with status: {}", workflowId, newState);
            }

        } catch (Exception e) {
            log.error("Error updating workflow steps for workflow: {} with status: {}", workflowId, newState, e);
            throw new RuntimeException("Failed to update workflow steps: " + e.getMessage(), e);
        }
    }

    private void updateStepIfExists(Long workflowId, KycWorkflowStep stepType, List<KycWorkflowStepStatus> stepsToUpdate) {
        try {
            KycWorkflowStepStatus step = kycWorkflowStepStatusRepository.findByKycWorkflowIdAndStep(workflowId, stepType)
                    .orElseThrow(() -> new RuntimeException("Workflow step not found: " + stepType + " for workflow: " + workflowId));

            if (step.getStatus() != KycWorkflowStepStatus.StepStatus.COMPLETED) {
                step.setStatus(KycWorkflowStepStatus.StepStatus.COMPLETED);
                step.setCompletedAt(LocalDateTime.now());
                step.setCompletedBy(securityUtils.getCurrentUserId());
                step.setCompletedByName(securityUtils.getCurrentUsername());
                stepsToUpdate.add(step);
                log.debug("Marked {} as COMPLETED for workflow: {}", stepType, workflowId);
            } else {
                log.debug("Step {} already COMPLETED for workflow: {}", stepType, workflowId);
            }
        } catch (RuntimeException e) {
            log.warn("Could not update step {} for workflow {}: {}", stepType, workflowId, e.getMessage());
            // Continue with other steps instead of failing completely
        }
    }


}