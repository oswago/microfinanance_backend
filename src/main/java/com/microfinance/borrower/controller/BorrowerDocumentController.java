package com.microfinance.borrower.controller;

import com.microfinance.borrower.dto.*;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.borrower.enums.KycWorkflowStep;
import com.microfinance.borrower.service.BorrowerDocumentService;
import com.microfinance.borrower.service.KycWorkflowService;
import com.microfinance.common.config.DocumentConfig;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.common.dto.DocumentTypeDto;
import com.microfinance.common.dto.DocumentStatusDto;
import com.microfinance.common.dto.ApiResponse;
import com.microfinance.common.service.DocumentConfigService;
import com.microfinance.common.util.CommonUtil;
import com.microfinance.loanproducts.entity.LoanProduct;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.logging.log4j.MarkerManager.clear;

@RestController
@RequestMapping("/borrowers-doc")
@RequiredArgsConstructor
public class BorrowerDocumentController {

    private final BorrowerDocumentService documentService;
    private final DocumentConfigService documentConfigService;
    private final KycWorkflowService kycWorkflowService;

    //************************Document Management Endpoints**********************************************************/

    @GetMapping("/{borrowerId}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<BorrowerDocumentDto>> getBorrowerDocuments(
            @PathVariable Long borrowerId) {
        List<BorrowerDocumentDto> documents = documentService.getBorrowerDocuments(borrowerId);
        System.out.println("========DATA:========="+documents);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<BorrowerDocumentDto> getDocumentById(
            @PathVariable Long documentId) {
        BorrowerDocumentDto document = documentService.getDocumentById(documentId);
        return ResponseEntity.ok(document);
    }

    @PostMapping(value = "/{borrowerId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<BorrowerDocumentDto> uploadDocument(
            @PathVariable Long borrowerId,
            @RequestParam DocumentConfig.DocumentType documentType,
            @RequestParam String documentName,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) LocalDate expiryDate) {

        BorrowerDocumentDto document;
        if (expiryDate != null) {
            document = documentService.uploadDocument(
                    borrowerId, documentType, documentName, file, description, expiryDate);
        } else {
            document = documentService.uploadDocument(
                    borrowerId, documentType, documentName, file, description);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(document);
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<Void> removeDocument(@PathVariable Long documentId) {
        documentService.removeDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    //************************Document Status Management Endpoints***************************************************/

    @PatchMapping("/documents/{documentId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<BorrowerDocumentDto> updateDocumentStatus(
            @PathVariable Long documentId,
            @RequestParam DocumentConfig.DocumentStatus status,
            @RequestParam(required = false) String verificationNotes) {

        BorrowerDocumentDto updatedDocument = documentService.updateDocumentStatus(
                documentId, status, verificationNotes);
        return ResponseEntity.ok(updatedDocument);
    }

    @PatchMapping("/documents/{documentId}/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<BorrowerDocumentDto> verifyDocument(
            @PathVariable Long documentId,
            @RequestParam(required = false) String verificationNotes) {

        BorrowerDocumentDto updatedDocument = documentService.verifyDocument(documentId, verificationNotes);
        return ResponseEntity.ok(updatedDocument);
    }

    @PatchMapping("/documents/{documentId}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<BorrowerDocumentDto> rejectDocument(
            @PathVariable Long documentId,
            @RequestParam(required = false) String verificationNotes) {

        BorrowerDocumentDto updatedDocument = documentService.rejectDocument(documentId, verificationNotes);
        return ResponseEntity.ok(updatedDocument);
    }

    //************************Document Query Endpoints***************************************************************/

    @GetMapping("/{borrowerId}/documents/type/{documentType}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<BorrowerDocumentDto>> getDocumentsByType(
            @PathVariable Long borrowerId,
            @PathVariable DocumentConfig.DocumentType documentType) {

        List<BorrowerDocumentDto> documents = documentService.getDocumentsByType(borrowerId, documentType);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{borrowerId}/documents/status/{status}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<BorrowerDocumentDto>> getDocumentsByStatus(
            @PathVariable Long borrowerId,
            @PathVariable DocumentConfig.DocumentStatus status) {

        List<BorrowerDocumentDto> documents = documentService.getDocumentsByStatus(borrowerId, status);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{borrowerId}/documents/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<BorrowerDocumentDto>> getPendingDocuments(
            @PathVariable Long borrowerId) {

        List<BorrowerDocumentDto> documents = documentService.getPendingDocuments(borrowerId);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/documents/expired")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<BorrowerDocumentDto>> getExpiredDocuments() {
        List<BorrowerDocumentDto> documents = documentService.getExpiredDocuments();
        return ResponseEntity.ok(documents);
    }

    //************************Document Utility Endpoints*************************************************************/

    @GetMapping("/{borrowerId}/documents/count-verified/{documentType}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<Long> countVerifiedDocumentsByType(
            @PathVariable Long borrowerId,
            @PathVariable DocumentConfig.DocumentType documentType) {

        Long count = documentService.countVerifiedDocumentsByType(borrowerId, documentType);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/{borrowerId}/documents/check-required")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<Boolean> hasRequiredDocuments(
            @PathVariable Long borrowerId,
            @RequestBody List<DocumentConfig.DocumentType> requiredTypes) {

        boolean hasRequired = documentService.hasRequiredDocuments(borrowerId, requiredTypes);
        return ResponseEntity.ok(hasRequired);
    }

    @PostMapping("/{borrowerId}/documents/missing-types")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<DocumentConfig.DocumentType>> getMissingDocumentTypes(
            @PathVariable Long borrowerId,
            @RequestBody List<DocumentConfig.DocumentType> requiredTypes) {

        List<DocumentConfig.DocumentType> missingTypes =
                documentService.getMissingDocumentTypes(borrowerId, requiredTypes);
        return ResponseEntity.ok(missingTypes);
    }

    //************************Bulk Operations Endpoints**************************************************************/

    @PostMapping("/documents/bulk/status-update")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<Integer> bulkUpdateDocumentStatus(
            @RequestParam List<Long> documentIds,
            @RequestParam DocumentConfig.DocumentStatus status,
            @RequestParam(required = false) String verificationNotes) {

        int updatedCount = documentService.bulkUpdateDocumentStatus(documentIds, status, verificationNotes);
        return ResponseEntity.ok(updatedCount);
    }

    @PostMapping("/kyc/bulk-verification")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<BulkKycVerificationResponse> bulkUpdateKycStatus(
            @Valid @RequestBody BulkKycVerificationRequest request) {

        BulkKycVerificationResponse response = documentService.bulkUpdateKycStatus(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/kyc/bulk-verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<BulkKycVerificationResponse> bulkKycVerification(
            @RequestBody List<Long> borrowerIds) {

        BulkKycVerificationResponse response = documentService.bulkKycVerification(borrowerIds, null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/kyc/bulk-reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<BulkKycVerificationResponse> bulkKycRejection(
            @RequestBody List<Long> borrowerIds,
            @RequestParam String rejectionReason) {

        BulkKycVerificationResponse response = documentService.bulkKycRejection(
                borrowerIds, rejectionReason, null);
        return ResponseEntity.ok(response);
    }

    // KYC workflow
    @GetMapping("/{id}/kyc-summary")
    public ResponseEntity<BorrowerKycSummaryDto> getKycSummary(@PathVariable Long id) {
        BorrowerKycSummaryDto summary = documentService.getKycSummary(id);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/kyc-required-docs")
    public ResponseEntity<Set<DocumentConfig.DocumentType>> getKycDocuments() {
        Set<DocumentConfig.DocumentType> summary = documentConfigService.getBasicKycDocuments();
        return ResponseEntity.ok(summary);
    }


    @GetMapping("/use-cases/{useCaseName}/required-documents")
    public ResponseEntity<ApiResponse<KycRequirementsDto>> getRequiredDocumentsForUseCase(
            @PathVariable String useCaseName,
            @RequestParam(required = false) Long borrowerId) {
        try {
            // Get required document types
           // Set<DocumentConfig.DocumentType> documentTypes = documentConfigService.getRequiredDocumentsForUseCase(useCaseName);
            Set<DocumentConfig.DocumentType> documentTypes = CommonUtil.getRequiredDocumentsForBorrower(borrowerId,useCaseName);

            Set<DocumentTypeDto> documentDtos = documentTypes.stream()
                    .map(DocumentTypeDto::fromEntity)
                    .collect(Collectors.toSet());

            // Get corresponding workflow steps WITH STATUS if borrowerId provided
            Set<KycWorkflowStepStatusDto> stepDtos;
            if (borrowerId != null) {
                // Get steps with actual status from existing workflow
                stepDtos = kycWorkflowService.getWorkflowStepsForDocumentTypesWithStatus(documentTypes, borrowerId);

                // ⚠️ DEBUG: Check what we're actually getting
                System.out.println("=== CONTROLLER DEBUG ===");
                System.out.println("Step DTOs size: " + stepDtos.size());
                if (!stepDtos.isEmpty()) {
                    KycWorkflowStepStatusDto firstStep = stepDtos.iterator().next();
                    System.out.println("First step class: " + firstStep.getClass().getName());
                    System.out.println("First step: " + firstStep);
                }
            } else {
                // Get static step definitions (fallback)
                Set<KycWorkflowStep> workflowSteps = kycWorkflowService.getWorkflowStepsForDocumentTypes(documentTypes);
                stepDtos = workflowSteps.stream()
                        .map(step -> kycWorkflowService.createDefaultStepStatusDto(step, null))
                        .collect(Collectors.toSet());
            }

            KycRequirementsDto response = new KycRequirementsDto(documentDtos, stepDtos);
            return ResponseEntity.ok(ApiResponse.success("KYC requirements retrieved successfully", response));
        } catch (Exception e) {
            System.err.println("=== ERROR in controller: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/all/types")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<ApiResponse<List<DocumentTypeDto>>> getAllDocumentTypes() {
        try {
            List<DocumentTypeDto> documentTypes = documentConfigService.getAllDocumentTypes();
            return ResponseEntity.ok(
                    ApiResponse.success("Document types retrieved successfully", documentTypes)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to retrieve document types: " + e.getMessage())
            );
        }
    }

    @GetMapping("/types/required")
    public ResponseEntity<ApiResponse<List<DocumentTypeDto>>> getRequiredDocumentTypes() {
        try {
            List<DocumentTypeDto> requiredTypes = documentConfigService.getRequiredDocumentTypes();
            return ResponseEntity.ok(
                    ApiResponse.success("Required document types retrieved successfully", requiredTypes)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to retrieve required document types: " + e.getMessage())
            );
        }
    }

    @GetMapping("/types/categories")
    public ResponseEntity<ApiResponse<List<String>>> getDocumentCategories() {
        try {
            List<String> categories = Arrays.stream(DocumentConfig.DocumentType.values())
                    .map(DocumentConfig.DocumentType::getCategory)
                    .distinct()
                    .collect(Collectors.toList());
            return ResponseEntity.ok(
                    ApiResponse.success("Document categories retrieved successfully", categories)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to retrieve document categories: " + e.getMessage())
            );
        }
    }

    @GetMapping("/types/category/{category}")
    public ResponseEntity<ApiResponse<List<DocumentTypeDto>>> getDocumentTypesByCategory(
            @PathVariable String category) {
        try {
            List<DocumentTypeDto> documentTypes = documentConfigService.getDocumentTypesByCategory(category);
            return ResponseEntity.ok(
                    ApiResponse.success("Document types for category retrieved successfully", documentTypes)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to retrieve document types for category: " + e.getMessage())
            );
        }
    }

    @GetMapping("/all/statuses")
    public ResponseEntity<ApiResponse<List<DocumentStatusDto>>> getAllDocumentStatuses() {
        try {
            List<DocumentStatusDto> statuses = Arrays.stream(DocumentConfig.DocumentStatus.values())
                    .map(DocumentStatusDto::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(
                    ApiResponse.success("Document statuses retrieved successfully", statuses)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to retrieve document statuses: " + e.getMessage())
            );
        }
    }

    //************************KYC Eligibility & Management Endpoints*************************************************/

    @GetMapping("/kyc/eligible-borrowers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<BorrowerDto>> getBorrowersEligibleForKycUpdate(
            @RequestParam(required = false) GeneralConfig.KycStatus currentStatus,
            @RequestParam(required = false) Boolean documentsUploaded) {

        List<BorrowerDto> eligibleBorrowers =
                documentService.getBorrowersEligibleForKycUpdate(currentStatus, documentsUploaded);
        return ResponseEntity.ok(eligibleBorrowers);
    }

    //************************System Maintenance Endpoints***********************************************************/

    @PostMapping("/documents/mark-expired")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> markExpiredDocuments() {
        documentService.markExpiredDocuments();
        return ResponseEntity.ok().build();
    }

    //************************Health Check Endpoints*****************************************************************/

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Borrower Document Service is running");
    }
}