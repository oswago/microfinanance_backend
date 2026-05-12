package com.microfinance.loanapplications.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.loanapplications.dto.collection.*;
import com.microfinance.loanapplications.service.RecoveryWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/recovery-workflow")
@RequiredArgsConstructor
public class RecoveryWorkflowController {

    private final RecoveryWorkflowService recoveryWorkflowService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    /**
     * Get all recovery cases with filters
     */
    @GetMapping("/cases")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<Page<RecoveryCaseDto>> getRecoveryCases(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Fetching recovery cases - search: {}, status: {}, stage: {}, priority: {}, assignedTo: {}",
                search, status, stage, priority, assignedTo);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        Page<RecoveryCaseDto> cases = recoveryWorkflowService.getRecoveryCases(
                search, status, stage, priority, assignedTo, page, size, sortBy, sortDirection, currentUser);

        return ResponseEntity.ok(cases);
    }

    /**
     * Get recovery case by ID
     */
    @GetMapping("/cases/{caseId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<RecoveryCaseDto> getRecoveryCaseById(@PathVariable Long caseId) {

        log.info("Fetching recovery case by ID: {}", caseId);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        RecoveryCaseDto recoveryCase = recoveryWorkflowService.getRecoveryCaseById(caseId, currentUser);

        return ResponseEntity.ok(recoveryCase);
    }



    /**
     * Create a new recovery case
     */
    @PostMapping("/cases")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<RecoveryCaseDto> createRecoveryCase(@Valid @RequestBody CreateRecoveryCaseDto request) {

        log.info("Creating new recovery case for loan: {}", request.getLoanId());

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        RecoveryCaseDto recoveryCase = recoveryWorkflowService.createRecoveryCase(request, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(recoveryCase);
    }


    // Add this method to RecoveryWorkflowController.java

    /**
     * Update recovery case after payment
     * This endpoint should be called when a payment is recorded
     */
    @PostMapping("/cases/update-after-payment")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<RecoveryCaseDto> updateRecoveryCaseAfterPayment(
            @Valid @RequestBody UpdateRecoveryCaseAfterPaymentDto request) {

        log.info(">>>Updating recovery case after payment for loan: {}, amount: {}",
                request.getLoanId(), request.getAmountPaid());

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        RecoveryCaseDto recoveryCase = recoveryWorkflowService.updateRecoveryCaseAfterPayment(
                request.getLoanId(), request.getAmountPaid(), request.getPaymentDate(), currentUser);

        return ResponseEntity.ok(recoveryCase);
    }


    /**
     * Escalate a recovery case
     */
    @PostMapping("/cases/{caseId}/escalate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<RecoveryCaseDto> escalateCase(
            @PathVariable Long caseId,
            @Valid @RequestBody EscalateCaseDto request) {

        log.info("Escalating recovery case: {}", caseId);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        RecoveryCaseDto recoveryCase = recoveryWorkflowService.escalateCase(caseId, request, currentUser);

        return ResponseEntity.ok(recoveryCase);
    }

    /**
     * Complete a workflow stage
     */
    @PostMapping("/cases/{caseId}/complete-stage")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<RecoveryCaseDto> completeStage(
            @PathVariable Long caseId,
            @Valid @RequestBody CompleteStageDto request) {

        log.info("Completing stage {} for case: {}", request.getStageKey(), caseId);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        RecoveryCaseDto recoveryCase = recoveryWorkflowService.completeStage(caseId, request, currentUser);

        return ResponseEntity.ok(recoveryCase);
    }

    /**
     * Close a recovery case
     */
    @PostMapping("/cases/{caseId}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<RecoveryCaseDto> closeCase(@PathVariable Long caseId, @RequestBody(required = false) String notes) {

        log.info("Closing recovery case: {}", caseId);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        RecoveryCaseDto recoveryCase = recoveryWorkflowService.closeCase(caseId, notes, currentUser);

        return ResponseEntity.ok(recoveryCase);
    }

    /**
     * Add a note to a recovery case
     */
    @PostMapping("/cases/{caseId}/notes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<CaseNoteDto> addCaseNote(
            @PathVariable Long caseId,
            @Valid @RequestBody AddCaseNoteDto request) {

        log.info("Adding note to recovery case: {}", caseId);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        CaseNoteDto note = recoveryWorkflowService.addCaseNote(caseId, request, currentUser);

        return ResponseEntity.ok(note);
    }

    /**
     * Get notes for a recovery case
     */
    @GetMapping("/cases/{caseId}/notes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<CaseNoteDto>> getCaseNotes(@PathVariable Long caseId) {

        log.info("Fetching notes for recovery case: {}", caseId);

        List<CaseNoteDto> notes = recoveryWorkflowService.getCaseNotes(caseId);

        return ResponseEntity.ok(notes);
    }

    /**
     * Get workflow stage statistics
     */
    @GetMapping("/stages/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<StageStatisticsDto>> getStageStatistics() {

        log.info("Fetching workflow stage statistics");

        List<StageStatisticsDto> stats = recoveryWorkflowService.getStageStatistics();

        return ResponseEntity.ok(stats);
    }

    /**
     * Get recovery agents
     */
    @GetMapping("/agents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<List<RecoveryAgentDto>> getRecoveryAgents() {

        log.info("Fetching recovery agents");

        List<RecoveryAgentDto> agents = recoveryWorkflowService.getRecoveryAgents();

        return ResponseEntity.ok(agents);
    }

    /**
     * Assign case to agent
     */
    @PostMapping("/cases/{caseId}/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<RecoveryCaseDto> assignCaseToAgent(
            @PathVariable Long caseId,
            @RequestParam Long agentId) {

        log.info("Assigning case {} to agent: {}", caseId, agentId);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        RecoveryCaseDto recoveryCase = recoveryWorkflowService.assignCaseToAgent(caseId, agentId, currentUser);

        return ResponseEntity.ok(recoveryCase);
    }

    /**
     * Export workflow data
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> exportWorkflowData(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(defaultValue = "PDF") String format) {

        log.info("Exporting workflow data with filters - search: {}, status: {}, stage: {}, priority: {}, assignedTo: {}",
                search, status, stage, priority, assignedTo);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        byte[] reportContent = recoveryWorkflowService.exportWorkflowData(
                search, status, stage, priority, assignedTo, format, currentUser);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "recovery-workflow-export.pdf");

        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }
}