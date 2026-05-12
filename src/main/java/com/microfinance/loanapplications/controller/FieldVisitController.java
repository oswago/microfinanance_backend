// controller/FieldVisitController.java
package com.microfinance.loanapplications.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.loanapplications.dto.collection.*;
import com.microfinance.loanapplications.service.FieldVisitService;
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

@Slf4j
@RestController
@RequestMapping("/field-visits")
@RequiredArgsConstructor
public class FieldVisitController {

    private final FieldVisitService fieldVisitService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    /**
     * Schedule a new field visit
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<FieldVisitDto> scheduleVisit(@Valid @RequestBody ScheduleVisitDto request) {
        log.info("Scheduling new field visit for loan: {}", request.getLoanId());
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        FieldVisitDto visit = fieldVisitService.scheduleVisit(request, currentUser);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(visit);
    }

    /**
     * Get all field visits with filters
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<Page<FieldVisitDto>> getFieldVisits(
            @RequestParam(required = false) Long loanId,
            @RequestParam(required = false) Long recoveryCaseId,
            @RequestParam(required = false) Long assignedOfficerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "visitDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        log.info("Fetching field visits with filters - loanId: {}, status: {}", loanId, status);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        Page<FieldVisitDto> visits = fieldVisitService.getFieldVisits(
                loanId, recoveryCaseId, assignedOfficerId, status, fromDate, toDate, pageable, currentUser);
        
        return ResponseEntity.ok(visits);
    }

    /**
     * Get field visit by ID
     */
    @GetMapping("/{visitId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<FieldVisitDto> getFieldVisitById(@PathVariable Long visitId) {
        log.info("Fetching field visit by ID: {}", visitId);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        FieldVisitDto visit = fieldVisitService.getFieldVisitById(visitId, currentUser);
        
        return ResponseEntity.ok(visit);
    }

    /**
     * Get visits for a specific loan
     */
    @GetMapping("/loan/{loanId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<FieldVisitDto>> getVisitsByLoanId(@PathVariable Long loanId) {
        log.info("Fetching field visits for loan: {}", loanId);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        List<FieldVisitDto> visits = fieldVisitService.getVisitsByLoanId(loanId, currentUser);
        
        return ResponseEntity.ok(visits);
    }

    /**
     * Get visits for a recovery case
     */
    @GetMapping("/recovery-case/{recoveryCaseId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<FieldVisitDto>> getVisitsByRecoveryCaseId(@PathVariable Long recoveryCaseId) {
        log.info("Fetching field visits for recovery case: {}", recoveryCaseId);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        List<FieldVisitDto> visits = fieldVisitService.getVisitsByRecoveryCaseId(recoveryCaseId, currentUser);
        
        return ResponseEntity.ok(visits);
    }

    /**
     * Update visit outcome
     */
    @PatchMapping("/{visitId}/outcome")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<FieldVisitDto> updateVisitOutcome(
            @PathVariable Long visitId,
            @Valid @RequestBody UpdateVisitOutcomeDto request) {
        
        log.info("Updating outcome for visit: {} to status: {}", visitId, request.getStatus());
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        FieldVisitDto visit = fieldVisitService.updateVisitOutcome(visitId, request, currentUser);
        
        return ResponseEntity.ok(visit);
    }

    /**
     * Cancel a field visit
     */
    @PostMapping("/{visitId}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<FieldVisitDto> cancelVisit(
            @PathVariable Long visitId,
            @Valid @RequestBody CancelVisitDto request) {
        
        log.info("Cancelling field visit: {}", visitId);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        FieldVisitDto visit = fieldVisitService.cancelVisit(visitId, request.getReason(), currentUser);
        
        return ResponseEntity.ok(visit);
    }

    /**
     * Reschedule a field visit
     */
    @PostMapping("/{visitId}/reschedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<FieldVisitDto> rescheduleVisit(
            @PathVariable Long visitId,
            @Valid @RequestBody RescheduleVisitDto request) {
        
        log.info("Rescheduling field visit: {} to date: {}", visitId, request.getNewDate());
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        FieldVisitDto visit = fieldVisitService.rescheduleVisit(
                visitId, request.getNewDate(), request.getNewTime(), request.getReason(), currentUser);
        
        return ResponseEntity.ok(visit);
    }

    /**
     * Get upcoming visits for current user
     */
    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<FieldVisitDto>> getUpcomingVisits() {
        log.info("Fetching upcoming visits for current user");
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        List<FieldVisitDto> visits = fieldVisitService.getUpcomingVisits(currentUser);
        
        return ResponseEntity.ok(visits);
    }

    /**
     * Send reminder for a visit
     */
    @PostMapping("/{visitId}/send-reminder")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<Void> sendReminder(@PathVariable Long visitId) {
        log.info("Sending reminder for visit: {}", visitId);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        fieldVisitService.sendReminder(visitId, currentUser);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Export visits report
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> exportVisits(
            @RequestParam(required = false) Long loanId,
            @RequestParam(required = false) Long recoveryCaseId,
            @RequestParam(required = false) Long assignedOfficerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "PDF") String format) {
        
        log.info("Exporting field visits report");
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        byte[] reportContent = fieldVisitService.exportVisits(
                loanId, recoveryCaseId, assignedOfficerId, status, fromDate, toDate, format, currentUser);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(format.equals("PDF") ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "field-visits-export." + format.toLowerCase());
        
        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }
}