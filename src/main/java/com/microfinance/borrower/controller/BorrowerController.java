package com.microfinance.borrower.controller;

import com.microfinance.borrower.dto.*;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.borrower.service.BorrowerActivityService;
import com.microfinance.borrower.service.BorrowerService;
import com.microfinance.common.config.DocumentConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.microfinance.base.utils.SecurityUtils;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/borrowers")
@RequiredArgsConstructor
public class BorrowerController {

    private final BorrowerService borrowerService;
    private final SecurityUtils securityUtils;
    private final BorrowerActivityService borrowerActivityService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<Page<BorrowerDto>> getAllBorrowers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<BorrowerDto> borrowers = borrowerService.getAllBorrowers(pageable);
        return ResponseEntity.ok(borrowers);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<Page<BorrowerDto>> searchBorrowers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BorrowerDto> borrowers = borrowerService.searchBorrowers(query, pageable);
        return ResponseEntity.ok(borrowers);
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<Page<BorrowerDto>> getBorrowersByBranch(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BorrowerDto> borrowers = borrowerService.getBorrowersByBranch(branchId, pageable);
        return ResponseEntity.ok(borrowers);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<BorrowerDto> getBorrowerById(@PathVariable Long id) {
        BorrowerDto borrower = borrowerService.getBorrowerById(id);
        return ResponseEntity.ok(borrower);
    }

    @GetMapping("/number/{borrowerNumber}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<BorrowerDto> getBorrowerByNumber(@PathVariable String borrowerNumber) {
        BorrowerDto borrower = borrowerService.getBorrowerByNumber(borrowerNumber);
        return ResponseEntity.ok(borrower);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<BorrowerDto> createBorrower(
            @Valid @RequestBody BorrowerDto borrowerDto,
            Authentication authentication) {
        // Extract user ID from authentication
        Long createdBy = securityUtils.getCurrentUserId();
        BorrowerDto createdBorrower = borrowerService.createBorrower(borrowerDto, createdBy);
        return ResponseEntity.ok(createdBorrower);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<BorrowerDto> updateBorrower(
            @PathVariable Long id,
            @Valid @RequestBody BorrowerDto borrowerDto) {
        
        BorrowerDto updatedBorrower = borrowerService.updateBorrower(id, borrowerDto);
        return ResponseEntity.ok(updatedBorrower);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteBorrower(@PathVariable Long id) {
        borrowerService.deleteBorrower(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<BorrowerDto> updateBorrowerStatus(
            @PathVariable Long id,
            @RequestParam Borrower.BorrowerStatus status) {
        
        BorrowerDto updatedBorrower = borrowerService.updateBorrowerStatus(id, status);
        return ResponseEntity.ok(updatedBorrower);
    }

    @PatchMapping("/{id}/kyc-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'CREDIT_APPROVER')")
    public ResponseEntity<BorrowerDto> updateKycStatus(
            @PathVariable Long id,
            @RequestParam Borrower.KycStatus kycStatus,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        
        //Long verifiedBy = getUserIdFromAuthentication(authentication);
        Long verifiedBy =securityUtils.getCurrentUserId();
        BorrowerDto updatedBorrower = borrowerService.updateKycStatus(id, kycStatus, verifiedBy, notes);
        return ResponseEntity.ok(updatedBorrower);
    }


    @GetMapping("/branch/{branchId}/count")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<Long> getBorrowerCountByBranch(@PathVariable Long branchId) {
        Long count = borrowerService.getBorrowerCountByBranch(branchId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<BorrowerSummaryDto> getBorrowerSummary(@PathVariable Long id) {
        BorrowerSummaryDto summary = borrowerService.getBorrowerSummaryById(id);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/summaries")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<Page<BorrowerSummaryDto>> getAllBorrowerSummaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BorrowerSummaryDto> summaries = borrowerService.getAllBorrowerSummaries(pageable);
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/group/{groupId}/summaries")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<List<BorrowerSummaryDto>> getBorrowerSummariesByGroup(@PathVariable Long groupId) {
        List<BorrowerSummaryDto> summaries = borrowerService.getBorrowerSummariesByGroup(groupId);
        return ResponseEntity.ok(summaries);
    }

    // ***************************************Document management endpoints************************************/
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<BorrowerDocumentDto>> getBorrowerDocuments(@PathVariable Long id) {
        List<BorrowerDocumentDto> documents = borrowerService.getBorrowerDocuments(id);
        return ResponseEntity.ok(documents);
    }

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<BorrowerDocumentDto> uploadDocument(
            @PathVariable Long id,
            @RequestParam DocumentConfig.DocumentType documentType,
            @RequestParam String documentName,
            @RequestParam(required = false) String description,
            @RequestParam MultipartFile file) {

        BorrowerDocumentDto document = borrowerService.uploadDocument(id, documentType, documentName, file, description);
        return ResponseEntity.ok(document);
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<Void> removeDocument(@PathVariable Long documentId) {
        borrowerService.removeDocument(documentId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/documents/{documentId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'CREDIT_APPROVER')")
    public ResponseEntity<BorrowerDocumentDto> updateDocumentStatus(
            @PathVariable Long documentId,
            @RequestParam DocumentConfig.DocumentStatus status,
            @RequestParam(required = false) String verificationNotes) {

        Long verifiedBy = securityUtils.getCurrentUserId();
        BorrowerDocumentDto updatedDocument = borrowerService.updateDocumentStatus(documentId, status, verifiedBy, verificationNotes);
        return ResponseEntity.ok(updatedDocument);
    }

    @GetMapping("/{id}/documents/type/{documentType}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<BorrowerDocumentDto>> getDocumentsByType(
            @PathVariable Long id,
            @PathVariable DocumentConfig.DocumentType documentType) {

        List<BorrowerDocumentDto> documents = borrowerService.getDocumentsByType(id, documentType);
        return ResponseEntity.ok(documents);
    }

    /// ////////////
    // Credit assessment
    @GetMapping("/{id}/credit-assessment")
    public ResponseEntity<BorrowerCreditAssessmentDto> getCreditAssessment(@PathVariable Long id) {
        BorrowerCreditAssessmentDto assessment = borrowerService.assessCreditworthiness(id);
        return ResponseEntity.ok(assessment);
    }

    // KYC workflow
    @GetMapping("/{id}/kyc-summary")
    public ResponseEntity<BorrowerKycSummaryDto> getKycSummary(@PathVariable Long id) {
        BorrowerKycSummaryDto summary = borrowerService.getKycSummary(id);
        return ResponseEntity.ok(summary);
    }

    // Portfolio summary
    @GetMapping("/{id}/portfolio-summary")
    public ResponseEntity<BorrowerPortfolioSummaryDto> getPortfolioSummary(@PathVariable Long id) {
        BorrowerPortfolioSummaryDto summary = borrowerService.getPortfolioSummary(id);
        return ResponseEntity.ok(summary);
    }

    // Loan eligibility
    @GetMapping("/{id}/loan-eligibility/{loanProductId}")
    public ResponseEntity<Boolean> checkLoanEligibility(
            @PathVariable Long id,
            @PathVariable Long loanProductId) {
        Boolean eligible = borrowerService.isBorrowerEligibleForLoan(id, loanProductId);
        return ResponseEntity.ok(eligible);
    }


    @GetMapping("/{id}/activities")
    public ResponseEntity<Page<BorrowerActivityDto>> getBorrowerActivities(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("activityDate").descending());
        Page<BorrowerActivityDto> activities = borrowerService.getBorrowerActivities(id, pageable);
        return ResponseEntity.ok(activities);
    }

    @PostMapping("/{id}/activities/search")
    public ResponseEntity<Page<BorrowerActivityDto>> searchActivities(
            @PathVariable Long id,
            @RequestBody ActivitySearchCriteria criteria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        criteria.setBorrowerId(id);
        Pageable pageable = PageRequest.of(page, size);
        Page<BorrowerActivityDto> activities = borrowerService.searchActivities(criteria, pageable);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/{id}/activities/summary")
    public ResponseEntity<BorrowerActivitySummaryDto> getActivitySummary(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        BorrowerActivitySummaryDto summary = borrowerService.getActivitySummary(id, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}/activities/timeline")
    public ResponseEntity<List<BorrowerActivityDto.TimelineGroup>> getActivityTimeline(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int days) {

        List<BorrowerActivityDto.TimelineGroup> timeline = borrowerActivityService.getActivityTimeline(id, days);
        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/{id}/activities/recent")
    public ResponseEntity<List<BorrowerActivityDto>> getRecentActivities(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit) {

        List<BorrowerActivityDto> activities = borrowerService.getRecentActivities(id, limit);
        return ResponseEntity.ok(activities);
    }

    // Bulk KYC status update
    @PostMapping("/bulk-kyc-verification")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'CREDIT_APPROVER')")
    public ResponseEntity<BulkKycVerificationResponse> bulkKycVerification(
            @Valid @RequestBody BulkKycVerificationRequest request) {
        BulkKycVerificationResponse response = borrowerService.bulkUpdateKycStatus(request);
        return ResponseEntity.ok(response);
    }

    // Quick bulk verification endpoint
    @PostMapping("/bulk-kyc-verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'CREDIT_APPROVER')")
    public ResponseEntity<BulkKycVerificationResponse> bulkKycVerify(
            @RequestBody List<Long> borrowerIds) {
        Long performedBy = securityUtils.getCurrentUserId();
        BulkKycVerificationResponse response = borrowerService.bulkKycVerification(borrowerIds, performedBy);
        return ResponseEntity.ok(response);
    }

    // Quick bulk rejection endpoint
    @PostMapping("/bulk-kyc-reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'CREDIT_APPROVER')")
    public ResponseEntity<BulkKycVerificationResponse> bulkKycReject(
            @RequestParam List<Long> borrowerIds,
            @RequestParam String rejectionReason) {
        Long performedBy = securityUtils.getCurrentUserId();
        BulkKycVerificationResponse response = borrowerService.bulkKycRejection(borrowerIds, rejectionReason, performedBy);
        return ResponseEntity.ok(response);
    }

    // Get borrowers eligible for bulk KYC update
    @GetMapping("/bulk-kyc-eligible")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<BorrowerDto>> getBulkKycEligibleBorrowers(
            @RequestParam(required = false) Borrower.KycStatus currentStatus,
            @RequestParam(defaultValue = "false") Boolean documentsUploaded) {

        List<BorrowerDto> eligibleBorrowers = borrowerService.getBorrowersEligibleForKycUpdate(currentStatus, documentsUploaded);
        return ResponseEntity.ok(eligibleBorrowers);
    }
}