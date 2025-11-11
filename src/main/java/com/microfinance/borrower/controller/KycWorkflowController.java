package com.microfinance.borrower.controller;

import com.microfinance.borrower.dto.KycWorkflowDto;
import com.microfinance.borrower.dto.KycWorkflowStepStatusDto;
import com.microfinance.borrower.service.KycWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @RequestBody KycWorkflowService.KycWorkflowUpdateRequest request,
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
            @RequestBody KycWorkflowService.StepUpdateRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Name") String userName) {
        KycWorkflowStepStatusDto step = kycWorkflowService.updateWorkflowStep(borrowerId, stepId, request, userId, userName);
        return ResponseEntity.ok(step);
    }
}