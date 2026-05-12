package com.microfinance.loanapplications.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.RolePermissionRepository;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.BusinessException;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.loanapplications.dto.collection.*;
import com.microfinance.loanapplications.entity.*;
import com.microfinance.loanapplications.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hpsf.Decimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecoveryWorkflowServiceImpl implements RecoveryWorkflowService {

    private final LoanRepository loanRepository;
    private final RecoveryCaseRepository recoveryCaseRepository;
    private final CaseNoteRepository caseNoteRepository;
    private final CollectionActionRepository collectionActionRepository;
    private final UserService userService;
    private final SecurityUtils securityUtils;
    private final RolePermissionRepository rolePermissionRepository;
    private final PdfGenerationService pdfService;

    private static final List<String> WORKFLOW_STAGES = Arrays.asList(
        "INITIAL_CONTACT", "PAYMENT_NEGOTIATION", "PAYMENT_PLAN", 
        "FIELD_VISIT", "LEGAL_NOTICE", "COURT_CASE", "ASSET_SEIZURE"
    );

    @Override
    @Transactional(readOnly = true)
    public Page<RecoveryCaseDto> getRecoveryCases(String search, String status, String stage, String priority,
                                                  Long assignedTo, int page, int size, String sortBy,
                                                  String sortDirection, User currentUser) {
        log.debug("Fetching recovery cases with filters");

        // Map sortBy to entity field names
        String sortField = mapSortField(sortBy);

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RecoveryCase> casesPage = recoveryCaseRepository.findAllWithFilters(
                search, status, stage, priority, assignedTo, pageable);

        List<RecoveryCaseDto> dtos = casesPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, casesPage.getTotalElements());
    }

    private String mapSortField(String sortBy) {
        if (sortBy == null) return "createdAt";

        switch (sortBy) {
            case "caseNumber":
                return "caseNumber";
            case "borrowerName":
                return "borrower.firstName";
            case "outstandingAmount":
                return "outstandingAmount";
            case "currentStage":
                return "currentStage";
            case "priority":
                return "priority";
            case "status":
                return "status";
            case "assignedAgent":
                return "assignedAgent.firstName";
            case "createdDate":
                return "createdAt";  // Map createdDate to createdAt
            default:
                return "createdAt";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RecoveryCaseDto getRecoveryCaseById(Long caseId, User currentUser) {
        log.debug("Fetching recovery case by ID: {}", caseId);

        RecoveryCase recoveryCase = recoveryCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery case not found with ID: " + caseId));

        return convertToDto(recoveryCase);
    }

    @Override
    @Transactional
    public RecoveryCaseDto createRecoveryCase(CreateRecoveryCaseDto request, User currentUser) {
        log.debug("Creating new recovery case for loan: {}", request.getLoanId());

        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with ID: " + request.getLoanId()));

        // Check if recovery case already exists for this loan
        if (recoveryCaseRepository.existsByLoanId(request.getLoanId())) {
            throw new BusinessException("Recovery case already exists for this loan");
        }

        // Generate case number
        String caseNumber = generateCaseNumber();

        // Find assigned agent  assignedAgentId
        User assignedAgent = null;
        if (request.getAssignedAgentId() != null) {  // Changed from getAssignedToAgentId() to getAssignedAgentId()
            assignedAgent = userService.getUserById(request.getAssignedAgentId());
            log.info("Assigned agent found: {} with ID: {}", assignedAgent != null ? assignedAgent.getUsername() : "null", request.getAssignedAgentId());
        } else {
            log.warn("No assigned agent ID provided in request");
        }

        RecoveryCase recoveryCase = RecoveryCase.builder()
                .caseNumber(caseNumber)
                .loan(loan)
                .borrower(loan.getBorrower())
                .branch(loan.getBranch())
                .assignedAgent(assignedAgent)
                .outstandingAmount(loan.getOutstandingBalance())
                .originalLoanAmount(loan.getPrincipalAmount())
                .recoveredAmount(BigDecimal.ZERO)
                .remainingAmount(loan.getOutstandingBalance())
                .daysOverdue(loan.getDaysDelinquent() != null ? loan.getDaysDelinquent() : 0)
                .currentStage("INITIAL_CONTACT")
                .status("ACTIVE")
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .assignedAgent(assignedAgent)
                //.createdBy(currentUser)
               // .createdAt(LocalDateTime.now())
                .notesText(request.getNotes())
                .completedStages(new ArrayList<>())
                .stageDates(new ArrayList<>())
                .build();

        // Add initial stage date
        recoveryCase.getStageDates().add(StageDate.builder()
                .stage("INITIAL_CONTACT")
                .date(LocalDate.now())
                .build());

        log.info(">>Case DetailsAssigned Agent {}:  User:{}  Agent:{}",recoveryCase.getAssignedAgent(),assignedAgent,request.getAssignedAgentId());

        RecoveryCase savedCase = recoveryCaseRepository.save(recoveryCase);

        // Create initial collection action
        createInitialCollectionAction(loan, savedCase, currentUser);

        log.info("Recovery case created with number: {}", caseNumber);

        return convertToDto(savedCase);
    }

    @Transactional
    @Override
    public RecoveryCaseDto updateRecoveryCaseAfterPayment(Long loanId, Double amountPaid,
                                                          LocalDate paymentDate, User currentUser) {
        log.info("Updating recovery case after payment for loan: {}, amount: {}, payment date: {}",
                loanId, amountPaid, paymentDate);

        // Find the recovery case by loan ID
        RecoveryCase recoveryCase = recoveryCaseRepository.findByLoanId(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery case not found for loan ID: " + loanId));

        // Get the associated loan to verify payment amount
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with ID: " + loanId));

        // Convert amountPaid from Double to BigDecimal
        BigDecimal paymentAmount = BigDecimal.valueOf(amountPaid);

        // Update recovered amount
        BigDecimal newRecoveredAmount = (recoveryCase.getRecoveredAmount() != null ? recoveryCase.getRecoveredAmount() : BigDecimal.ZERO)
                .add(paymentAmount);
        recoveryCase.setRecoveredAmount(newRecoveredAmount);

        // Update outstanding amount (reduce by payment)
        BigDecimal newOutstandingAmount = recoveryCase.getOutstandingAmount().subtract(paymentAmount);
        if (newOutstandingAmount.compareTo(BigDecimal.ZERO) < 0) {
            newOutstandingAmount = BigDecimal.ZERO;
        }
        recoveryCase.setOutstandingAmount(newOutstandingAmount);

        // Update remaining amount
        BigDecimal newRemainingAmount = recoveryCase.getRemainingAmount().subtract(paymentAmount);
        if (newRemainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            newRemainingAmount = BigDecimal.ZERO;
        }
        recoveryCase.setRemainingAmount(newRemainingAmount);

        // Update recovery rate (percentage of original amount recovered)
        BigDecimal originalAmount = recoveryCase.getOriginalLoanAmount();
        BigDecimal recoveryRate = BigDecimal.ZERO;
        if (originalAmount != null && originalAmount.compareTo(BigDecimal.ZERO) > 0) {
            recoveryRate = newRecoveredAmount
                    .divide(originalAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        recoveryCase.setRecoveryRate((int) recoveryRate.doubleValue());

        // Update last payment date
        recoveryCase.setLastPaymentDate(paymentDate);

        // Update days overdue (recalculate based on loan's current days delinquent)
        if (loan.getDaysDelinquent() != null) {
            recoveryCase.setDaysOverdue(loan.getDaysDelinquent());
        }

        // Update status if fully recovered
        if (newOutstandingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            recoveryCase.setStatus("CLOSED");
            recoveryCase.setClosedDate(LocalDate.now());
            recoveryCase.setClosedBy(currentUser);

            // Move to final stage if not already there
            if (!"ASSET_SEIZURE".equals(recoveryCase.getCurrentStage())) {
                recoveryCase.setCurrentStage("ASSET_SEIZURE");
                // Add stage date
                recoveryCase.getStageDates().add(StageDate.builder()
                        .stage("ASSET_SEIZURE")
                        .date(LocalDate.now())
                        .build());
            }

            // Add completion note
            addSystemNote(recoveryCase,
                    String.format("Case automatically closed - Full payment of %.2f received. Total recovered: %.2f",
                            paymentAmount, newRecoveredAmount),
                    currentUser);
        } else {
            // Update priority based on new outstanding amount
            updateCasePriority(recoveryCase);

            // Add payment note
            addSystemNote(recoveryCase,
                    String.format("Payment of %.2f recorded. Recovered amount: %.2f, Outstanding: %.2f, Remaining: %.2f",
                            paymentAmount, newRecoveredAmount, newOutstandingAmount, newRemainingAmount),
                    currentUser);
        }

        // Create a collection action for this payment
        createPaymentCollectionAction(loan, recoveryCase, paymentAmount, paymentDate, currentUser,newOutstandingAmount);

        RecoveryCase savedCase = recoveryCaseRepository.save(recoveryCase);

        log.info("Recovery case updated after payment. Case: {}, New outstanding: {}, New recovered: {}",
                savedCase.getCaseNumber(), newOutstandingAmount, newRecoveredAmount);

        return convertToDto(savedCase);
    }

    /**
     * Add a system-generated note to the recovery case
     */
    private void addSystemNote(RecoveryCase recoveryCase, String content, User currentUser) {
        CaseNote note = CaseNote.builder()
                .recoveryCase(recoveryCase)
                .content(content)
                .type("SYSTEM")
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();

        caseNoteRepository.save(note);
    }

    /**
     * Update case priority based on outstanding amount and days overdue
     */


    /**
     * Create a collection action for the payment
     */

    private void createPaymentCollectionAction(Loan loan, RecoveryCase recoveryCase,
                                               BigDecimal paymentAmount, LocalDate paymentDate,
                                               User currentUser, BigDecimal newOutstandingAmount) {

        GeneralConfig.Outcome outcome= GeneralConfig.Outcome.PARTIAL_PAYMENT;
        if(newOutstandingAmount.compareTo(BigDecimal.ZERO) <= 0){
             outcome= GeneralConfig.Outcome.FULL_PAYMENT;
        }else{
             outcome= GeneralConfig.Outcome.PARTIAL_PAYMENT;
        }
        CollectionAction action = CollectionAction.builder()
                .loan(loan)
                .actionType(GeneralConfig.ActionType.PAYMENT_COLLECTION)
                .actionStatus(GeneralConfig.ActionStatus.COMPLETED)
                .actionDate(LocalDate.now())
                .actionTime(java.time.LocalTime.now())
                .outcome(outcome)
                .notes(String.format("Payment of %.2f recorded of Case No: %s", paymentAmount,recoveryCase.getId()))
                .performedBy(currentUser)
                .build();

        collectionActionRepository.save(action);
    }


    @Override
    @Transactional
    public RecoveryCaseDto escalateCase(Long caseId, EscalateCaseDto request, User currentUser) {
        log.info("Escalating case: {} with reason: {}", caseId, request.getReason());

        RecoveryCase recoveryCase = recoveryCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery case not found with ID: " + caseId));

        String currentStage = recoveryCase.getCurrentStage();
        String escalatedStage = getEscalatedStage(currentStage);
        GeneralConfig.CasePriority currentPriority = GeneralConfig.CasePriority.valueOf(recoveryCase.getPriority());
        GeneralConfig.CasePriority escalatedPriority = getEscalatedPriority(currentPriority);

        // Store old values for logging
        String oldStage = currentStage;
        String oldPriority = recoveryCase.getPriority();

        // Update priority to higher level on escalation
        recoveryCase.setPriority(escalatedPriority.name());
        log.debug("Priority escalated from {} to {}", oldPriority, escalatedPriority);

        // Update the case stage
        recoveryCase.setCurrentStage(escalatedStage);
        recoveryCase.setStatus("ESCALATED");

        // Add current stage to completed stages
        if (recoveryCase.getCompletedStages() == null) {
            recoveryCase.setCompletedStages(new ArrayList<>());
        }
        if (!recoveryCase.getCompletedStages().contains(currentStage)) {
            recoveryCase.getCompletedStages().add(currentStage);
        }

        // Add stage dates
        if (recoveryCase.getStageDates() == null) {
            recoveryCase.setStageDates(new ArrayList<>());
        }

        // Add date for the escalated stage
        recoveryCase.getStageDates().add(StageDate.builder()
                .recoveryCase(recoveryCase)
                .stage(escalatedStage)
                .date(LocalDate.now())
                .build());

        // Add escalation note
        String escalationNote = String.format(
                "Case escalated from '%s' to '%s'. Reason: %s. Priority changed from %s to %s.",
                currentStage, escalatedStage,
                request.getReason() != null ? request.getReason() : "Not specified",
                oldPriority, escalatedPriority
        );
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            escalationNote += " Additional notes: " + request.getNotes();
        }

        CaseNote note = CaseNote.builder()
                .recoveryCase(recoveryCase)
                .content(escalationNote)
                .type("ESCALATION")
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();
        caseNoteRepository.save(note);

        // Update timestamps
        recoveryCase.setUpdatedAt(LocalDateTime.now());
        recoveryCase.setUpdatedBy(currentUser.getId());

        RecoveryCase savedCase = recoveryCaseRepository.save(recoveryCase);

        log.info("Case escalated: {} from {} to {}. New priority: {}",
                recoveryCase.getCaseNumber(), oldStage, escalatedStage, escalatedPriority);

        return convertToDto(savedCase);
    }



    @Override
    @Transactional
    public RecoveryCaseDto completeStage(Long caseId, CompleteStageDto request, User currentUser) {
        log.info("Completing stage {} for case: {}", request.getStageKey(), caseId);

        RecoveryCase recoveryCase = recoveryCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery case not found with ID: " + caseId));

        String currentStage = recoveryCase.getCurrentStage();
        String targetStage = request.getStageKey();

        // Validate that the stage being completed matches the current stage
        if (!currentStage.equals(targetStage)) {
            log.warn("Cannot complete stage {} because current stage is {}", targetStage, currentStage);
            throw new BusinessException("Cannot complete stage " + targetStage +
                    " because current stage is " + currentStage);
        }

        // Add to completed stages if not already there
        if (recoveryCase.getCompletedStages() == null) {
            recoveryCase.setCompletedStages(new ArrayList<>());
        }
        if (!recoveryCase.getCompletedStages().contains(targetStage)) {
            recoveryCase.getCompletedStages().add(targetStage);
            log.debug("Added stage {} to completed stages", targetStage);
        }

        // Update or add stage date
        if (recoveryCase.getStageDates() == null) {
            recoveryCase.setStageDates(new ArrayList<>());
        }

        Optional<StageDate> stageDateOpt = recoveryCase.getStageDates().stream()
                .filter(sd -> sd.getStage().equals(targetStage))
                .findFirst();

        if (stageDateOpt.isPresent()) {
            stageDateOpt.get().setDate(LocalDate.now());
            log.debug("Updated stage date for {}", targetStage);
        } else {
            recoveryCase.getStageDates().add(StageDate.builder()
                    .recoveryCase(recoveryCase)
                    .stage(targetStage)
                    .date(LocalDate.now())
                    .build());
            log.debug("Added new stage date for {}", targetStage);
        }

        // Move to next stage in workflow
        String nextStage = getNextStage(currentStage);
        recoveryCase.setCurrentStage(nextStage);
        log.info("Moving case from {} to {}", currentStage, nextStage);

        // Add stage date for the new stage
        recoveryCase.getStageDates().add(StageDate.builder()
                .recoveryCase(recoveryCase)
                .stage(nextStage)
                .date(LocalDate.now())
                .build());

        // Update priority based on new stage and outstanding amount
        updateCasePriority(recoveryCase);

        // Add completion note
        String noteContent = String.format("Stage '%s' completed. Moving to '%s' stage.",
                targetStage, nextStage);
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            noteContent += " Notes: " + request.getNotes();
        }

        CaseNote note = CaseNote.builder()
                .recoveryCase(recoveryCase)
                .content(noteContent)
                .type("STAGE_COMPLETION")
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();
        caseNoteRepository.save(note);

        // Update timestamps
        recoveryCase.setUpdatedAt(LocalDateTime.now());
        recoveryCase.setUpdatedBy(currentUser.getId());

        // Update status if this was the final stage
        if (isLastStage(nextStage)) {
            recoveryCase.setStatus("CLOSED");
            recoveryCase.setClosedDate(LocalDate.now());
            recoveryCase.setClosedBy(currentUser);
            log.info("Case {} reached final stage and is now closed", recoveryCase.getCaseNumber());

            // Add closure note
            CaseNote closureNote = CaseNote.builder()
                    .recoveryCase(recoveryCase)
                    .content("Case automatically closed after completing final stage: " + nextStage)
                    .type("CLOSURE")
                    .createdBy(currentUser)
                    .createdAt(LocalDateTime.now())
                    .build();
            caseNoteRepository.save(closureNote);
        }

        RecoveryCase savedCase = recoveryCaseRepository.save(recoveryCase);

        log.info("Stage {} completed for case {}. New stage: {}",
                targetStage, caseId, savedCase.getCurrentStage());

        return convertToDto(savedCase);
    }




    @Override
    @Transactional
    public RecoveryCaseDto closeCase(Long caseId, String notes, User currentUser) {
        log.debug("Closing recovery case: {}", caseId);

        RecoveryCase recoveryCase = recoveryCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery case not found with ID: " + caseId));

        recoveryCase.setStatus("CLOSED");
        recoveryCase.setClosedDate(LocalDate.now());
        recoveryCase.setClosedBy(currentUser);
        recoveryCase.setUpdatedAt(LocalDateTime.now());
        recoveryCase.setUpdatedBy(currentUser.getId());

        // Add closing note
        if (notes != null && !notes.isEmpty()) {
            CaseNote note = CaseNote.builder()
                    .recoveryCase(recoveryCase)
                    .content("Case closed: " + notes)
                    .type("CLOSURE")
                    .createdBy(currentUser)
                    .createdAt(LocalDateTime.now())
                    .build();
            caseNoteRepository.save(note);
        }

        RecoveryCase savedCase = recoveryCaseRepository.save(recoveryCase);

        log.info("Recovery case {} closed", caseId);

        return convertToDto(savedCase);
    }

    @Override
    @Transactional
    public CaseNoteDto addCaseNote(Long caseId, AddCaseNoteDto request, User currentUser) {
        log.debug("Adding note to recovery case: {}", caseId);

        RecoveryCase recoveryCase = recoveryCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery case not found with ID: " + caseId));

        CaseNote note = CaseNote.builder()
                .recoveryCase(recoveryCase)
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : "NOTE")
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();

        CaseNote savedNote = caseNoteRepository.save(note);

        recoveryCase.setUpdatedAt(LocalDateTime.now());
        recoveryCase.setUpdatedBy(currentUser.getId());
        recoveryCaseRepository.save(recoveryCase);

        log.info("Note added to case {}", caseId);

        return convertToNoteDto(savedNote);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CaseNoteDto> getCaseNotes(Long caseId) {
        log.debug("Fetching notes for recovery case: {}", caseId);

        List<CaseNote> notes = caseNoteRepository.findByRecoveryCaseIdOrderByCreatedAtDesc(caseId);

        return notes.stream()
                .map(this::convertToNoteDto)
                .collect(Collectors.toList());
    }



    // Helper methods

    private String getNextStage(String currentStage) {
        List<String> stageOrder = Arrays.asList(
                "INITIAL_CONTACT", "PAYMENT_NEGOTIATION", "PAYMENT_PLAN",
                "FIELD_VISIT", "LEGAL_NOTICE", "COURT_CASE", "ASSET_SEIZURE"
        );

        int currentIndex = stageOrder.indexOf(currentStage);
        if (currentIndex >= 0 && currentIndex < stageOrder.size() - 1) {
            return stageOrder.get(currentIndex + 1);
        }

        // If it's the last stage, return the same stage
        return currentStage;
    }

    private String getEscalatedStage(String currentStage) {
        List<String> stageOrder = Arrays.asList(
                "INITIAL_CONTACT", "PAYMENT_NEGOTIATION", "PAYMENT_PLAN",
                "FIELD_VISIT", "LEGAL_NOTICE", "COURT_CASE", "ASSET_SEIZURE"
        );

        int currentIndex = stageOrder.indexOf(currentStage);
        if (currentIndex >= 0 && currentIndex < stageOrder.size() - 2) {
            // Skip one stage when escalating (jump ahead 2 stages)
            return stageOrder.get(currentIndex + 2);
        } else if (currentIndex >= 0 && currentIndex < stageOrder.size() - 1) {
            // Move to next stage if can't skip
            return stageOrder.get(currentIndex + 1);
        }

        // If it's the last stage, return the same stage
        return currentStage;
    }

    private GeneralConfig.CasePriority getEscalatedPriority(GeneralConfig.CasePriority currentPriority) {
        switch (currentPriority) {
            case LOW:
                return GeneralConfig.CasePriority.MEDIUM;
            case MEDIUM:
                return GeneralConfig.CasePriority.HIGH;
            case HIGH:
                return GeneralConfig.CasePriority.CRITICAL;
            case CRITICAL:
                return GeneralConfig.CasePriority.CRITICAL;
            default:
                return GeneralConfig.CasePriority.MEDIUM;
        }
    }

    private void updateCasePriority(RecoveryCase recoveryCase) {
        // Priority based on stage and outstanding amount
        String stage = recoveryCase.getCurrentStage();
        BigDecimal outstanding = recoveryCase.getOutstandingAmount();
        String newPriority;

        if ("COURT_CASE".equals(stage) || "ASSET_SEIZURE".equals(stage)) {
            newPriority = "CRITICAL";
        } else if ("LEGAL_NOTICE".equals(stage)) {
            newPriority = "HIGH";
        } else if ("FIELD_VISIT".equals(stage)) {
            if (outstanding != null && outstanding.compareTo(BigDecimal.valueOf(25000)) > 0) {
                newPriority = "HIGH";
            } else {
                newPriority = "MEDIUM";
            }
        } else if (outstanding != null && outstanding.compareTo(BigDecimal.valueOf(50000)) > 0) {
            newPriority = "HIGH";
        } else if (outstanding != null && outstanding.compareTo(BigDecimal.valueOf(25000)) > 0) {
            newPriority = "MEDIUM";
        } else {
            newPriority = "LOW";
        }

        String oldPriority = recoveryCase.getPriority();
        if (!newPriority.equals(oldPriority)) {
            recoveryCase.setPriority(newPriority);
            log.debug("Priority updated from {} to {} for case {}",
                    oldPriority, newPriority, recoveryCase.getCaseNumber());
        }
    }

    private boolean isLastStage(String stage) {
        List<String> stageOrder = Arrays.asList(
                "INITIAL_CONTACT", "PAYMENT_NEGOTIATION", "PAYMENT_PLAN",
                "FIELD_VISIT", "LEGAL_NOTICE", "COURT_CASE", "ASSET_SEIZURE"
        );
        return stageOrder.indexOf(stage) == stageOrder.size() - 1;
    }




    @Override
    @Transactional(readOnly = true)
    public List<StageStatisticsDto> getStageStatistics() {
        log.debug("Fetching stage statistics");

        List<StageStatisticsDto> stats = new ArrayList<>();

        for (String stage : WORKFLOW_STAGES) {
            List<RecoveryCase> cases = recoveryCaseRepository.findByCurrentStage(stage);
            
            int count = cases.size();
            BigDecimal totalAmount = cases.stream()
                    .map(RecoveryCase::getOutstandingAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal recoveryRate = BigDecimal.ZERO;
            if (count > 0) {
                BigDecimal totalRecoveryRate = BigDecimal.ZERO;
                int validCount = 0;

                for (RecoveryCase recoveryCase : cases) {
                    Integer rate = recoveryCase.getRecoveryRate();
                    if (rate != null) {
                        totalRecoveryRate = totalRecoveryRate.add(BigDecimal.valueOf(rate));
                        validCount++;
                    }
                }

                if (validCount > 0) {
                    recoveryRate = totalRecoveryRate.divide(BigDecimal.valueOf(validCount), 2, RoundingMode.HALF_UP);
                }
            }


            stats.add(StageStatisticsDto.builder()
                    .stage(stage)
                    .count(count)
                    .amount(totalAmount)
                    .recoveryRate(recoveryRate)
                    .build());
        }

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecoveryAgentDto> getRecoveryAgents() {
        log.debug("Fetching recovery agents");

        List<User> agents = userService.getUsersByRole(User.UserRole.COLLECTION_OFFICER);
        List<RecoveryAgentDto> agentDtos = new ArrayList<>();

        for (User agent : agents) {
            List<RecoveryCase> assignedCases = recoveryCaseRepository.findByAssignedAgentId(agent.getId());
            
            int assignedCount = assignedCases.size();
            int resolvedCount = (int) assignedCases.stream()
                    .filter(c -> "CLOSED".equals(c.getStatus()))
                    .count();
            
            BigDecimal recoveredAmount = assignedCases.stream()
                    .map(RecoveryCase::getRecoveredAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            agentDtos.add(RecoveryAgentDto.builder()
                    .id(agent.getId())
                    .name(agent.getFirstName() + " " + (agent.getLastName() != null ? agent.getLastName() : ""))
                    .assignedCases(assignedCount)
                    .resolvedCases(resolvedCount)
                    .recoveryAmount(recoveredAmount)
                    .build());
        }

        return agentDtos;
    }

    @Override
    @Transactional
    public RecoveryCaseDto assignCaseToAgent(Long caseId, Long agentId, User currentUser) {
        log.debug("Assigning case {} to agent: {}", caseId, agentId);

        RecoveryCase recoveryCase = recoveryCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery case not found with ID: " + caseId));

        User agent = userService.getUserById(agentId);
        if (agent == null) {
            throw new ResourceNotFoundException("Agent not found with ID: " + agentId);
        }

        recoveryCase.setAssignedAgent(agent);
        recoveryCase.setUpdatedAt(LocalDateTime.now());
        recoveryCase.setUpdatedBy(currentUser.getId());

        // Add assignment note
        CaseNote note = CaseNote.builder()
                .recoveryCase(recoveryCase)
                .content("Case assigned to " + agent.getFirstName() + " " + agent.getLastName())
                .type("ASSIGNMENT")
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();
        caseNoteRepository.save(note);

        RecoveryCase savedCase = recoveryCaseRepository.save(recoveryCase);

        log.info("Case {} assigned to agent {}", caseId, agentId);

        return convertToDto(savedCase);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportWorkflowData(String search, String status, String stage, String priority,
                                     Long assignedTo, String format, User currentUser) {
        log.debug("Exporting workflow data");

        Pageable pageable = PageRequest.of(0, 1000);
        Page<RecoveryCase> casesPage = recoveryCaseRepository.findAllWithFilters(
                search, status, stage, priority, assignedTo, pageable);

        List<RecoveryCaseDto> cases = casesPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        if (format.equalsIgnoreCase("PDF")) {
            try {
                return generateWorkflowPdfReport(cases);
            } catch (DocumentException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new BusinessException("Unsupported format: " + format);
        }
    }

    // ==================== Private Helper Methods ====================

    private String generateCaseNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        String random = String.format("%06d", new Random().nextInt(999999));
        return "RC-" + year + "-" + random;
    }


    private void createInitialCollectionAction(Loan loan, RecoveryCase recoveryCase, User currentUser) {
        CollectionAction action = CollectionAction.builder()
                .loan(loan)
                .actionType(GeneralConfig.ActionType.FOLLOW_UP)
                .actionStatus(GeneralConfig.ActionStatus.COMPLETED)
                .actionDate(LocalDate.now())
                .actionTime(java.time.LocalTime.now())
                .outcome(GeneralConfig.Outcome.REQUESTED_FOLLOW_UP)
                .notes("Recovery case created: " + recoveryCase.getCaseNumber())
                .performedBy(currentUser)
                .build();
        
        collectionActionRepository.save(action);
    }

    private void createEscalationAction(Loan loan, RecoveryCase recoveryCase, User currentUser, String reason) {
        CollectionAction action = CollectionAction.builder()
                .loan(loan)
                .actionType(GeneralConfig.ActionType.ESCALATION)
                .actionStatus(GeneralConfig.ActionStatus.COMPLETED)
                .actionDate(LocalDate.now())
                .actionTime(java.time.LocalTime.now())
                .outcome(GeneralConfig.Outcome.REFUSED_TO_PAY)
                .notes("Case escalated: " + reason + " - Priority: " + recoveryCase.getPriority())
                .performedBy(currentUser)
                .build();
        
        collectionActionRepository.save(action);
    }

    private RecoveryCaseDto convertToDto(RecoveryCase recoveryCase) {
        // Calculate recovered amount from payments
        BigDecimal recoveredAmount = calculateRecoveredAmount(recoveryCase.getLoan());

        // Calculate recovery rate
        // Calculate recovery rate safely
        int recoveryRate = 0;
        if (recoveryCase.getRecoveryRate() != null) {
            recoveryRate = recoveryCase.getRecoveryRate();
        } else if (recoveryCase.getOriginalLoanAmount() != null &&
                recoveryCase.getOriginalLoanAmount().compareTo(BigDecimal.ZERO) > 0) {
            // Calculate from recovered amount if not set
             recoveredAmount = calculateRecoveredAmount(recoveryCase.getLoan());
            recoveryRate = recoveredAmount.multiply(BigDecimal.valueOf(100))
                    .divide(recoveryCase.getOriginalLoanAmount(), 0, RoundingMode.HALF_UP).intValue();
        }

        // Calculate days in recovery
        long daysInRecovery = ChronoUnit.DAYS.between(
                recoveryCase.getCreatedAt().toLocalDate(), LocalDate.now());

        // Get recent activities
        List<CollectionAction> recentActions = collectionActionRepository
                .findTop5ByLoanIdOrderByActionDateDesc(recoveryCase.getLoan().getId());

        LocalDateTime lastActivityDate = recentActions.isEmpty() ? null : 
                recentActions.get(0).getActionDate().atTime(recentActions.get(0).getActionTime());
        String lastActivityType = recentActions.isEmpty() ? null : 
                recentActions.get(0).getActionType().name();

        // Get notes
        List<CaseNoteDto> notes = caseNoteRepository.findByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCase.getId())
                .stream()
                .map(this::convertToNoteDto)
                .collect(Collectors.toList());

        // Get stage dates
        List<StageDateDto> stageDates = recoveryCase.getStageDates().stream()
                .map(sd -> StageDateDto.builder()
                        .stage(sd.getStage())
                        .date(LocalDate.parse(sd.getDate().toString()))
                        .build())
                .collect(Collectors.toList());

        return RecoveryCaseDto.builder()
                .id(recoveryCase.getId())
                .caseNumber(recoveryCase.getCaseNumber())
                .loanId(recoveryCase.getLoan().getId())
                .loanNumber(recoveryCase.getLoan().getLoanAccountNumber())
                .borrowerId(recoveryCase.getBorrower().getId())
                .borrowerName(recoveryCase.getBorrower().getFullName())
                .borrowerPhone(recoveryCase.getBorrower().getPhoneNumber())
                .borrowerEmail(recoveryCase.getBorrower().getEmail())
                .borrowerAddress(recoveryCase.getBorrower().getAddress())
                .outstandingAmount(recoveryCase.getOutstandingAmount())
                .originalLoanAmount(recoveryCase.getOriginalLoanAmount())

                .loanProductName(recoveryCase.getLoan().getLoanProduct() !=null ? recoveryCase.getLoan().getLoanProduct().getName():"N/A")
                .interestRate(recoveryCase.getLoan().getInterestRate() != null ? recoveryCase.getLoan().getInterestRate() : BigDecimal.ZERO )
                .branchName(recoveryCase.getBranch() !=null ? recoveryCase.getBranch().getName() : "N/A")

                .recoveredAmount(recoveredAmount)
                .remainingAmount(recoveryCase.getOutstandingAmount().subtract(recoveredAmount))
                .daysOverdue(recoveryCase.getDaysOverdue())
                .daysInRecovery((int) daysInRecovery)
                .recoveryRate(recoveryRate)
                .currentStage(recoveryCase.getCurrentStage())
                .status(recoveryCase.getStatus())
                .priority(recoveryCase.getPriority())
                .stageDuration(calculateStageDuration(recoveryCase))
                .assignedAgentId(recoveryCase.getAssignedAgent() != null ? recoveryCase.getAssignedAgent().getId() : null)
                .assignedAgent(recoveryCase.getAssignedAgent() != null ?
                        recoveryCase.getAssignedAgent().getFirstName() + " " +
                        (recoveryCase.getAssignedAgent().getLastName() != null ? recoveryCase.getAssignedAgent().getLastName() : "") : null)
                .lastActivityDate(lastActivityDate)
                .lastActivityType(lastActivityType)
                .contactAttempts(Math.toIntExact(collectionActionRepository.countByLoanId(recoveryCase.getLoan().getId())))
                .agentsInvolved(countAgentsInvolved(recoveryCase))
                .completedStages(recoveryCase.getCompletedStages())
                .notes(notes)
                .stageDates(stageDates)
                .createdDate(recoveryCase.getCreatedAt())
                .build();
    }

    private CaseNoteDto convertToNoteDto(CaseNote note) {
        return CaseNoteDto.builder()
                .id(note.getId())
                .content(note.getContent())
                .createdBy(note.getCreatedBy().getFirstName() + " " + 
                        (note.getCreatedBy().getLastName() != null ? note.getCreatedBy().getLastName() : ""))
                .createdDate(note.getCreatedAt())
                .type(note.getType())
                .attachments(new ArrayList<>()) // Add attachment handling if needed
                .build();
    }

  /*  private CaseNoteDto convertToNoteDto(CaseNote note) {
        CaseNoteDto dto = new CaseNoteDto();
        dto.setId(note.getId());
        dto.setContent(note.getContent());
        dto.setType(note.getType());
        if (note.getCreatedBy() != null) {
            dto.setCreatedBy(note.getCreatedBy().getFullName());
            dto.setCreatedById(note.getCreatedBy().getId());
        }
        dto.setCreatedDate(note.getCreatedAt());
        return dto;
    }*/

    private BigDecimal calculateRecoveredAmount(Loan loan) {
        // Sum of all repayments made on this loan
        return loan.getRepayments().stream()
                .map(LoanRepayment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer calculateStageDuration(RecoveryCase recoveryCase) {
        Optional<StageDate> currentStageDate = recoveryCase.getStageDates().stream()
                .filter(sd -> sd.getStage().equals(recoveryCase.getCurrentStage()))
                .findFirst();
        
        if (currentStageDate.isPresent()) {
            return (int) ChronoUnit.DAYS.between(currentStageDate.get().getDate(), LocalDate.now());
        }
        return 0;
    }

    private Integer countAgentsInvolved(RecoveryCase recoveryCase) {
        List<Long> agentIds = collectionActionRepository
                .findDistinctAgentIdsByLoanId(recoveryCase.getLoan().getId());
        return agentIds.size();
    }

    private byte[] generateWorkflowPdfReport(List<RecoveryCaseDto> cases) throws DocumentException, IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            pdfService.addDocumentHeader(document, "Recovery Workflow Report", "Case Management Summary");

            // Add summary statistics
            pdfService.addSectionTitle(document, "Summary");
            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            pdfService.addTableHeader(summaryTable, "Total Cases");
            pdfService.addTableHeader(summaryTable, "Active Cases");
            pdfService.addTableHeader(summaryTable, "Closed Cases");
            pdfService.addTableHeader(summaryTable, "Total Outstanding");

            long totalCases = cases.size();
            long activeCases = cases.stream().filter(c -> "ACTIVE".equals(c.getStatus())).count();
            long closedCases = cases.stream().filter(c -> "CLOSED".equals(c.getStatus())).count();
            BigDecimal totalOutstanding = cases.stream()
                    .map(RecoveryCaseDto::getOutstandingAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            pdfService.addTableCell(summaryTable, String.valueOf(totalCases));
            pdfService.addTableCell(summaryTable, String.valueOf(activeCases));
            pdfService.addTableCell(summaryTable, String.valueOf(closedCases));
            pdfService.addAmountCell(summaryTable, totalOutstanding);
            document.add(summaryTable);
            document.add(new Paragraph(" "));

            // Add cases table
            pdfService.addSectionTitle(document, "Recovery Cases");
            PdfPTable casesTable = new PdfPTable(7);
            casesTable.setWidthPercentage(100);
            pdfService.addTableHeader(casesTable, "Case #");
            pdfService.addTableHeader(casesTable, "Borrower");
            pdfService.addTableHeader(casesTable, "Amount");
            pdfService.addTableHeader(casesTable, "Stage");
            pdfService.addTableHeader(casesTable, "Status");
            pdfService.addTableHeader(casesTable, "Priority");
            pdfService.addTableHeader(casesTable, "Assigned To");

            for (RecoveryCaseDto recoveryCase : cases) {
                pdfService.addTableCell(casesTable, recoveryCase.getCaseNumber());
                pdfService.addTableCell(casesTable, recoveryCase.getBorrowerName());
                pdfService.addAmountCell(casesTable, recoveryCase.getOutstandingAmount());
                pdfService.addTableCell(casesTable, formatStageName(recoveryCase.getCurrentStage()));
                pdfService.addTableCell(casesTable, recoveryCase.getStatus());
                pdfService.addTableCell(casesTable, recoveryCase.getPriority());
                pdfService.addTableCell(casesTable, recoveryCase.getAssignedAgent() != null ? 
                        recoveryCase.getAssignedAgent() : "Unassigned");
            }
            document.add(casesTable);
            document.add(new Paragraph(" "));

            pdfService.addDocumentFooter(document);
            document.close();

            return baos.toByteArray();
        }
    }

    private String formatStageName(String stage) {
        Map<String, String> stageNames = new HashMap<>();
        stageNames.put("INITIAL_CONTACT", "Initial Contact");
        stageNames.put("PAYMENT_NEGOTIATION", "Negotiation");
        stageNames.put("PAYMENT_PLAN", "Payment Plan");
        stageNames.put("FIELD_VISIT", "Field Visit");
        stageNames.put("LEGAL_NOTICE", "Legal Notice");
        stageNames.put("COURT_CASE", "Court Case");
        stageNames.put("ASSET_SEIZURE", "Asset Seizure");
        return stageNames.getOrDefault(stage, stage);
    }
}