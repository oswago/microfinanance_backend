package com.microfinance.borrower.controller;

import com.microfinance.borrower.dto.*;
import com.microfinance.borrower.dto.KycWorkflowUpdateRequest;
import com.microfinance.borrower.service.KycWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/kyc-workflow/{borrowerId}/kyc")
@RequiredArgsConstructor
//@Tag(name = "KYC Workflow", description = "APIs for managing KYC workflow processes")
public class KycWorkflowController {

    private final KycWorkflowService kycWorkflowService;

    @PostMapping("/start")
    //@Operation(summary = "Start KYC process for a borrower")
    public ResponseEntity<KycWorkflowDto> startKycProcess(
            @PathVariable Long borrowerId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Name") String userName) {
        KycWorkflowDto workflow = kycWorkflowService.startKycProcess(borrowerId, userId, userName);
        return ResponseEntity.ok(workflow);
    }

    @PutMapping("/status")
    //@Operation(summary = "Update KYC workflow status")
    public ResponseEntity<KycWorkflowDto> updateKycStatus(
            @PathVariable Long borrowerId,
            @RequestBody KycWorkflowUpdateRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Name") String userName) {
        KycWorkflowDto workflow = kycWorkflowService.updateKycStatus(borrowerId, request, userId, userName);
        return ResponseEntity.ok(workflow);
    }

    @GetMapping
   // @Operation(summary = "Get KYC workflow details")
    public ResponseEntity<KycWorkflowDto> getKycWorkflow(@PathVariable Long borrowerId) {
        KycWorkflowDto workflow = kycWorkflowService.getKycWorkflow(borrowerId);
        return ResponseEntity.ok(workflow);
    }

    @GetMapping("/steps")
    //@Operation(summary = "Get KYC workflow steps")
    public ResponseEntity<List<KycWorkflowStepStatusDto>> getKycWorkflowSteps(@PathVariable Long borrowerId) {
        List<KycWorkflowStepStatusDto> steps = kycWorkflowService.getKycWorkflowSteps(borrowerId);
        return ResponseEntity.ok(steps);
    }

    @PutMapping("/steps/{stepId}")
    //@Operation(summary = "Update workflow step status")
    public ResponseEntity<KycWorkflowStepStatusDto> updateWorkflowStep(
            @PathVariable Long borrowerId,
            @PathVariable Long stepId,
            @RequestBody StepUpdateRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Name") String userName) {
        KycWorkflowStepStatusDto step = kycWorkflowService.updateWorkflowStep(borrowerId, stepId, request, userId, userName);
        return ResponseEntity.ok(step);
    }

    // Add these methods to your KycWorkflowController

    @PostMapping("/sync-documents")
//@Operation(summary = "Sync verified documents with KYC workflow")
    public ResponseEntity<DocumentSyncResponse> syncDocumentsWithWorkflow(
            @PathVariable Long borrowerId,
            @RequestBody DocumentSyncRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Name") String userName) {
        DocumentSyncResponse response = kycWorkflowService.syncDocumentsWithWorkflow(
                borrowerId, request.getDocumentIds(), userId, userName);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auto-complete-steps")
//@Operation(summary = "Auto-complete workflow steps based on verified documents")
    public ResponseEntity<AutoCompleteResponse> autoCompleteSteps(
            @PathVariable Long borrowerId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Name") String userName) {
        AutoCompleteResponse response = kycWorkflowService.autoCompleteSteps(borrowerId, userId, userName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/document-mapping")
//@Operation(summary = "Get document to workflow step mapping")
    public ResponseEntity<Map<String, List<String>>> getDocumentStepMapping(@PathVariable Long borrowerId) {
        Map<String, List<String>> mapping = kycWorkflowService.getDocumentStepMapping(borrowerId);
        return ResponseEntity.ok(mapping);
    }

    @GetMapping("/check-auto-progress")
//@Operation(summary = "Check and perform auto-progression if possible")
    public ResponseEntity<AutoProgressResponse> checkAutoProgress(
            @PathVariable Long borrowerId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Name") String userName) {
        AutoProgressResponse response = kycWorkflowService.checkAutoProgress(borrowerId, userId, userName);
        return ResponseEntity.ok(response);
    }





}