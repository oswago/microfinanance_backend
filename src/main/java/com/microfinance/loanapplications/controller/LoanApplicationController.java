package com.microfinance.loanapplications.controller;

import com.microfinance.base.service.UserService;
import com.microfinance.loanapplications.dto.*;
import com.microfinance.loanapplications.service.LoanApplicationService;
import com.microfinance.base.entity.User;
import com.microfinance.base.utils.SecurityUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/loan-applications")
@RequiredArgsConstructor
public class LoanApplicationController {
    
    private final LoanApplicationService loanApplicationService;
    private final SecurityUtils securityUtils;
    private final UserService userService;
    
    @PostMapping
    //@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER') and hasPermission('APPLICATION_CREATE')")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<LoanApplicationDto> createApplication(
            @Valid @RequestBody CreateLoanApplicationDto dto) {

        log.info("Creating loan application for borrower: {} by user: {}", 
                dto.getBorrowerId(), securityUtils.getCurrentUsername());
        User currentUser=userService.getUserById(securityUtils.getCurrentUserId());
        
        LoanApplicationDto application = loanApplicationService.createApplication(dto, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(application);
    }
    
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER') and hasPermission('APPLICATION_SUBMIT')")
    public ResponseEntity<LoanApplicationDto> submitForApproval(
            @PathVariable Long id,
            @Valid @RequestBody SubmitApplicationDto dto) {

        User currentUser=userService.getUserById(securityUtils.getCurrentUserId());
        
        log.info("Submitting loan application {} for approval by user: {}", id, currentUser.getUsername());
        
        LoanApplicationDto application = loanApplicationService.submitForApproval(id, dto, currentUser);
        return ResponseEntity.ok(application);
    }
    
    @GetMapping("/drafts")
   // @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER') and hasPermission('APPLICATION_DRAFT_MANAGE')")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<List<LoanApplicationDto>> getDraftApplications() {
        User currentUser=userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Fetching draft applications for user: {}", currentUser.getUsername());
        
        List<LoanApplicationDto> drafts = loanApplicationService.getDraftApplications(currentUser);
        return ResponseEntity.ok(drafts);
    }
    
    @GetMapping("/pending-approval")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER') and hasPermission('APPLICATION_APPROVE')")
    public ResponseEntity<List<LoanApplicationDto>> getPendingApprovals() {
        
        log.info("Fetching pending approval applications");
        
        List<LoanApplicationDto> applications = loanApplicationService.getPendingApprovals();
        return ResponseEntity.ok(applications);
    }

    @GetMapping
  /*  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER', 'ACCOUNTANT') and " +
            "(hasPermission('APPLICATION_VIEW_ALL') or hasPermission('APPLICATION_VIEW_BRANCH') or hasPermission('APPLICATION_VIEW_OWN'))")*/
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER', 'ACCOUNTANT')")
    public ResponseEntity<Page<LoanApplicationDto>> getApplications(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long officerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Fetching loan applications with filters - status: {}, branch: {}, officer: {}",
                status, branchId, officerId);

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<LoanApplicationDto> applications = loanApplicationService.getApplicationsByStatus(status, pageable);

        return ResponseEntity.ok(applications);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER', 'ACCOUNTANT')" )
                // "(hasPermission('APPLICATION_READ') or hasPermission('APPLICATION_VIEW_ALL') or hasPermission('APPLICATION_VIEW_BRANCH') or hasPermission('APPLICATION_VIEW_OWN'))")
    public ResponseEntity<LoanApplicationDto> getApplication(@PathVariable Long id) {

        log.info("Fetching loan application details for id: {}", id);

        LoanApplicationDto application = loanApplicationService.getApplicationById(id);
        return ResponseEntity.ok(application);
    }

    
    @GetMapping("/number/{applicationNumber}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER', 'ACCOUNTANT')")
                 //"(hasPermission('APPLICATION_READ') or hasPermission('APPLICATION_VIEW_ALL') or hasPermission('APPLICATION_VIEW_BRANCH') or hasPermission('APPLICATION_VIEW_OWN'))")
    public ResponseEntity<LoanApplicationDto> getApplicationByNumber(@PathVariable String applicationNumber) {
        
        log.info("Fetching loan application by number: {}", applicationNumber);
        
        LoanApplicationDto application = loanApplicationService.getApplicationByNumber(applicationNumber);
        return ResponseEntity.ok(application);
    }
    
    @GetMapping("/borrower/{borrowerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER', 'ACCOUNTANT')")
                 //"(hasPermission('APPLICATION_READ') or hasPermission('APPLICATION_VIEW_ALL') or hasPermission('APPLICATION_VIEW_BRANCH') or hasPermission('APPLICATION_VIEW_OWN'))")
    public ResponseEntity<List<LoanApplicationDto>> getApplicationsByBorrower(@PathVariable Long borrowerId) {
        
        log.info("Fetching loan applications for borrower: {}", borrowerId);
        
        List<LoanApplicationDto> applications = loanApplicationService.getApplicationsByBorrower(borrowerId);
        return ResponseEntity.ok(applications);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
            //"and hasPermission('APPLICATION_UPDATE')")
    public ResponseEntity<LoanApplicationDto> updateApplication(
            @PathVariable Long id,
            @Valid @RequestBody CreateLoanApplicationDto dto) {

        User currentUser=userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Updating loan application {} by user: {}", id, currentUser.getUsername());
        
        LoanApplicationDto application = loanApplicationService.updateApplication(id, dto, currentUser);
        return ResponseEntity.ok(application);
    }
    
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
            //"and hasPermission('APPLICATION_UPDATE')")
    public ResponseEntity<LoanApplicationDto> cancelApplication(
            @PathVariable Long id,
            @RequestBody CancelRequestDto dto) {
        User currentUser=userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Cancelling loan application {} by user: {}", id, currentUser.getUsername());
        
        LoanApplicationDto application = loanApplicationService.cancelApplication(id, dto.getReason(), currentUser);
        return ResponseEntity.ok(application);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')" )
            //"and hasPermission('APPLICATION_DELETE')")
    public ResponseEntity<Void> deleteDraftApplication(
            @PathVariable Long id) {
        User currentUser=userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Deleting draft loan application {} by user: {}", id, currentUser.getUsername());
        
        loanApplicationService.deleteDraftApplication(id, currentUser);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER') " )
            //"and hasPermission('APPLICATION_STATS_VIEW')")
    public ResponseEntity<ApplicationStatsDto> getApplicationStatistics(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Fetching application statistics for branch: {}, period: {} to {}", branchId, startDate, endDate);
        
        ApplicationStatsDto stats = loanApplicationService.getApplicationStatistics(branchId, startDate, endDate);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER') " )
            //"and hasPermission('APPLICATION_EXPORT')")
    public ResponseEntity<Resource> exportApplications(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "csv") String format) {
        
        log.info("Exporting applications with filters - status: {}, branch: {}, from: {}, to: {}", 
                status, branchId, fromDate, toDate);
        
        // Implementation for export would go here
        // This would return a CSV or Excel file
        return ResponseEntity.ok().build();
    }


    @GetMapping("/compliance/check")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<DocumentComplianceSummary> checkDocumentCompliance(
            @RequestParam Long borrowerId,
            @RequestParam Long loanProductId) {

        log.info("Starting document compliance check for borrower: {}, product: {}", borrowerId, loanProductId);

        try {
            DocumentComplianceSummary compliance = loanApplicationService.checkDocumentCompliance(borrowerId, loanProductId);
            log.info("Compliance check completed successfully");
            return ResponseEntity.ok(compliance);
        } catch (StackOverflowError e) {
            log.error("StackOverflowError in compliance check: {}", e.getMessage());
            // Return a simple error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DocumentComplianceSummary.builder()
                            .meetsRequirements(false)
                            .errorMessage("Internal error: " + e.getMessage())
                            .build());
        }
    }


    @PostMapping("/validate-requirements")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<ApplicationValidationResult> validateApplicationRequirements(
            @Valid @RequestBody ApplicationValidationRequest request) {

        Boolean requirementsMet = loanApplicationService.validateApplicationRequirements(
                request.getBorrowerId(), request.getLoanProductId());

        ApplicationValidationResult result = ApplicationValidationResult.builder()
                .requirementsMet(requirementsMet)
                .message(requirementsMet ?
                        "All requirements met" :
                        "Some requirements are not met")
                .build();

        return ResponseEntity.ok(result);
    }
}