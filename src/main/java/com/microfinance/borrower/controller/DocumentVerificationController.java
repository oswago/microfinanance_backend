package com.microfinance.borrower.controller;

import com.microfinance.borrower.dto.*;
import com.microfinance.borrower.entity.DocumentVerification;
import com.microfinance.borrower.service.DocumentVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/document-verifications")
@RequiredArgsConstructor
public class DocumentVerificationController {

    private final DocumentVerificationService verificationService;

    @GetMapping("/borrower/{borrowerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<DocumentVerificationDto>> getDocumentVerifications(
            @PathVariable Long borrowerId) {
        List<DocumentVerificationDto> verifications = verificationService.getDocumentVerifications(borrowerId);
        return ResponseEntity.ok(verifications);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<DocumentVerificationDto> getDocumentVerification(
            @PathVariable Long id) {
        DocumentVerificationDto verification = verificationService.getDocumentVerification(id);
        return ResponseEntity.ok(verification);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<DocumentVerificationDto> createDocumentVerification(
            @Valid @RequestBody DocumentVerificationCreateRequest request) {
        DocumentVerificationDto verification = verificationService.createDocumentVerification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(verification);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<DocumentVerificationDto> updateDocumentVerification(
            @PathVariable Long id,
            @Valid @RequestBody DocumentVerificationUpdateRequest request) {
        DocumentVerificationDto verification = verificationService.updateDocumentVerification(id, request);
        return ResponseEntity.ok(verification);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<Void> deleteDocumentVerification(
            @PathVariable Long id) {
        verificationService.deleteDocumentVerification(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<DocumentVerificationDto> verifyDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentVerificationActionRequest request) {
        DocumentVerificationDto verification = verificationService.verifyDocument(id, request);
        return ResponseEntity.ok(verification);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<DocumentVerificationDto> rejectDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentVerificationActionRequest request) {
        DocumentVerificationDto verification = verificationService.rejectDocument(id, request);
        return ResponseEntity.ok(verification);
    }

    @GetMapping("/borrower/{borrowerId}/status/{status}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<DocumentVerificationDto>> getDocumentVerificationsByStatus(
            @PathVariable Long borrowerId,
            @PathVariable DocumentVerification.VerificationStatus status) {
        List<DocumentVerificationDto> verifications = verificationService.getDocumentVerificationsByStatus(borrowerId, status);
        return ResponseEntity.ok(verifications);
    }

    @GetMapping("/borrower/{borrowerId}/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<DocumentVerificationDto>> getPendingDocumentVerifications(
            @PathVariable Long borrowerId) {
        List<DocumentVerificationDto> verifications = verificationService.getPendingDocumentVerifications(borrowerId);
        return ResponseEntity.ok(verifications);
    }

    @GetMapping("/borrower/{borrowerId}/verified")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<DocumentVerificationDto>> getVerifiedDocumentVerifications(
            @PathVariable Long borrowerId) {
        List<DocumentVerificationDto> verifications = verificationService.getVerifiedDocumentVerifications(borrowerId);
        return ResponseEntity.ok(verifications);
    }

    @GetMapping("/borrower/{borrowerId}/count-verified")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<Long> countActiveVerifiedDocuments(
            @PathVariable Long borrowerId) {
        Long count = verificationService.countActiveVerifiedDocuments(borrowerId);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/mark-expired")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> markExpiredVerifications() {
        verificationService.markExpiredVerifications();
        return ResponseEntity.ok().build();
    }
}