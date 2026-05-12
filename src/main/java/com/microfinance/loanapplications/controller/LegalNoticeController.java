// controller/LegalNoticeController.java
package com.microfinance.loanapplications.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.loanapplications.dto.collection.*;
import com.microfinance.loanapplications.service.LegalNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/legal-notices")
@RequiredArgsConstructor
public class LegalNoticeController {

    private final LegalNoticeService legalNoticeService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    /**
     * Send a new legal notice
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LEGAL_OFFICER')")
    public ResponseEntity<LegalNoticeDto> sendLegalNotice(@Valid @RequestBody SendLegalNoticeDto request) {
        log.info("Sending new legal notice for loan: {}", request.getLoanId());
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        LegalNoticeDto notice = legalNoticeService.sendLegalNotice(request, currentUser);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(notice);
    }

    /**
     * Get all legal notices with filters
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LEGAL_OFFICER', 'COLLECTION_OFFICER')")
    public ResponseEntity<Page<LegalNoticeDto>> getLegalNotices(
            @RequestParam(required = false) Long loanId,
            @RequestParam(required = false) Long recoveryCaseId,
            @RequestParam(required = false) Long assignedOfficerId,
            @RequestParam(required = false) String noticeType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        log.info("Fetching legal notices with filters - loanId: {}, status: {}", loanId, status);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        Page<LegalNoticeDto> notices = legalNoticeService.getLegalNotices(
                loanId, recoveryCaseId, assignedOfficerId, noticeType, status, fromDate, toDate, pageable, currentUser);
        
        return ResponseEntity.ok(notices);
    }

    /**
     * Get legal notice by ID
     */
    @GetMapping("/{noticeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LEGAL_OFFICER', 'COLLECTION_OFFICER')")
    public ResponseEntity<LegalNoticeDto> getLegalNoticeById(@PathVariable Long noticeId) {
        log.info("Fetching legal notice by ID: {}", noticeId);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        LegalNoticeDto notice = legalNoticeService.getLegalNoticeById(noticeId, currentUser);
        
        return ResponseEntity.ok(notice);
    }

    /**
     * Get notices for a specific loan
     */
    @GetMapping("/loan/{loanId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LEGAL_OFFICER', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<LegalNoticeDto>> getNoticesByLoanId(@PathVariable Long loanId) {
        log.info("Fetching legal notices for loan: {}", loanId);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        List<LegalNoticeDto> notices = legalNoticeService.getNoticesByLoanId(loanId, currentUser);
        
        return ResponseEntity.ok(notices);
    }

    /**
     * Get notices for a recovery case
     */
    @GetMapping("/recovery-case/{recoveryCaseId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LEGAL_OFFICER', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<LegalNoticeDto>> getNoticesByRecoveryCaseId(@PathVariable Long recoveryCaseId) {
        log.info("Fetching legal notices for recovery case: {}", recoveryCaseId);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        List<LegalNoticeDto> notices = legalNoticeService.getNoticesByRecoveryCaseId(recoveryCaseId, currentUser);
        
        return ResponseEntity.ok(notices);
    }

    /**
     * Update notice status
     */
    @PatchMapping("/{noticeId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LEGAL_OFFICER')")
    public ResponseEntity<LegalNoticeDto> updateNoticeStatus(
            @PathVariable Long noticeId,
            @Valid @RequestBody UpdateNoticeStatusDto request) {
        
        log.info("Updating status for notice: {} to: {}", noticeId, request.getStatus());
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        LegalNoticeDto notice = legalNoticeService.updateNoticeStatus(noticeId, request, currentUser);
        
        return ResponseEntity.ok(notice);
    }

    /**
     * Cancel a legal notice
     */
    @PostMapping("/{noticeId}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LEGAL_OFFICER')")
    public ResponseEntity<LegalNoticeDto> cancelNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody CancelNoticeDto request) {
        
        log.info("Cancelling legal notice: {}", noticeId);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        LegalNoticeDto notice = legalNoticeService.cancelNotice(noticeId, request.getReason(), currentUser);
        
        return ResponseEntity.ok(notice);
    }

    /**
     * Generate legal document
     */
    @GetMapping("/{noticeId}/generate-document")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LEGAL_OFFICER')")
    public ResponseEntity<byte[]> generateDocument(@PathVariable Long noticeId) {
        log.info("Generating document for legal notice: {}", noticeId);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        byte[] document = legalNoticeService.generateDocument(noticeId, currentUser);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "legal-notice-" + noticeId + ".pdf");
        
        return new ResponseEntity<>(document, headers, HttpStatus.OK);
    }

    /**
     * Download legal notice document
     */
    @GetMapping("/{noticeId}/download")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LEGAL_OFFICER')")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long noticeId) {
        log.info("Downloading document for legal notice: {}", noticeId);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        byte[] document = legalNoticeService.downloadDocument(noticeId, currentUser);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "legal-notice-" + noticeId + ".pdf");
        
        return new ResponseEntity<>(document, headers, HttpStatus.OK);
    }

    /**
     * Send compliance reminder
     */
    @PostMapping("/{noticeId}/send-reminder")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LEGAL_OFFICER', 'COLLECTION_OFFICER')")
    public ResponseEntity<Void> sendComplianceReminder(@PathVariable Long noticeId) {
        log.info("Sending compliance reminder for notice: {}", noticeId);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        legalNoticeService.sendComplianceReminder(noticeId, currentUser);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Export legal notices report
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT', 'LEGAL_OFFICER')")
    public ResponseEntity<byte[]> exportNotices(
            @RequestParam(required = false) Long loanId,
            @RequestParam(required = false) Long recoveryCaseId,
            @RequestParam(required = false) Long assignedOfficerId,
            @RequestParam(required = false) String noticeType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "PDF") String format) {
        
        log.info("Exporting legal notices report");
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        byte[] reportContent = legalNoticeService.exportNotices(
                loanId, recoveryCaseId, assignedOfficerId, noticeType, status, fromDate, toDate, format, currentUser);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(format.equals("PDF") ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "legal-notices-export." + format.toLowerCase());
        
        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }

    /**
     * Get legal notice statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT', 'LEGAL_OFFICER')")
    public ResponseEntity<Map<String, Object>> getNoticeStatistics() {
        log.info("Fetching legal notice statistics");
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        Map<String, Object> statistics = legalNoticeService.getNoticeStatistics(currentUser);
        
        return ResponseEntity.ok(statistics);
    }
}