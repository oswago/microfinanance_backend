package com.microfinance.loanapplications.controller;

import com.microfinance.base.service.UserService;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.common.service.GeneralConfigService;
import com.microfinance.exception.BusinessException;
import com.microfinance.loanapplications.dto.LoanDto;
import com.microfinance.loanapplications.dto.disbursement.*;
import com.microfinance.loanapplications.dto.repayment.RepaymentReceiptDto;
import com.microfinance.loanapplications.service.LoanDisbursementService;
import com.microfinance.base.entity.User;
import com.microfinance.base.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/loan-disbursements")
@RequiredArgsConstructor
public class LoanDisbursementController {
    
    private final LoanDisbursementService loanDisbursementService;
    private final SecurityUtils securityUtils;
    private final UserService userService;
    private final GeneralConfigService generalConfigService;
    
    @PostMapping("/{id}/disburse")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT') and @permissionCheckService.hasPermission('LOAN_DISBURSE')")
    //@PreAuthorize("@permissionCheckService.hasPermission('LOAN_DISBURSE')")
    public ResponseEntity<LoanDto> disburseLoan(
            @PathVariable Long id,
            @Valid @RequestBody DisburseLoanDto dto) {


        User currentUser=userService.getUserById(securityUtils.getCurrentUserId());

        log.info("Disbursing loan {} by user: {}", id, currentUser.getUsername());
        
        LoanDto loan = loanDisbursementService.disburseLoan(id, dto, currentUser);
        return ResponseEntity.ok(loan);
    }


    
    @GetMapping("/pending-disbursement")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT', 'LOAN_OFFICER' )")
                // "(hasPermission('LOAN_DISBURSE') or hasPermission('LOAN_VIEW_ALL') or hasPermission('LOAN_VIEW_BRANCH') or hasPermission('LOAN_VIEW_OWN'))")
    public ResponseEntity<Page<LoanDto>> getLoansPendingDisbursement(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "approvedDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        
        log.info("Fetching loans pending disbursement");
        
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        List<LoanDto> loans = loanDisbursementService.getLoansPendingDisbursement();
        return ResponseEntity.ok(new PageImpl<>(loans, pageable, loans.size()));
    }
    
    @GetMapping("/{id}/disbursement-receipt")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT', 'LOAN_OFFICER') and " +
                 "(hasPermission('LOAN_DISBURSE') or hasPermission('LOAN_VIEW_ALL') or hasPermission('LOAN_VIEW_BRANCH') or hasPermission('LOAN_VIEW_OWN'))")
    public ResponseEntity<DisbursementReceiptDto> generateDisbursementReceipt(@PathVariable Long id) {
        
        log.info("Generating disbursement receipt for loan: {}", id);

        DisbursementReceiptDto receipt = loanDisbursementService.generateDisbursementReceipt(id);
        return ResponseEntity.ok(receipt);
    }
    
    @GetMapping("/account/{accountNumber}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT', 'COLLECTION_OFFICER') and " +
                 "(hasPermission('LOAN_VIEW_ALL') or hasPermission('LOAN_VIEW_BRANCH') or hasPermission('LOAN_VIEW_OWN'))")
    public ResponseEntity<LoanDto> getLoanByAccountNumber(@PathVariable String accountNumber) {
        
        log.info("Fetching loan by account number: {}", accountNumber);
        
        LoanDto loan = loanDisbursementService.getLoanByAccountNumber(accountNumber);
        return ResponseEntity.ok(loan);
    }
    
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT') and hasPermission('LOAN_CLOSE')")
    public ResponseEntity<LoanDto> closeLoan(
            @PathVariable Long id) {
        User currentUser=userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Closing loan {} by user: {}", id, currentUser.getUsername());
        
        LoanDto loan = loanDisbursementService.closeLoan(id, currentUser);
        return ResponseEntity.ok(loan);
    }

    /*
    Write off endpoints
     */
    @PostMapping("/{id}/write-off")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER') and @permissionCheckService.hasPermission('LOAN_WRITE_OFF_APPROVE')")
    public ResponseEntity<WriteOffResponseDto> writeOffLoan(
            @PathVariable Long id,
            @Valid @RequestBody WriteOffRequestDto dto) {

        // Validate input
        if (dto == null) {
            throw new BusinessException("Write-off request cannot be null");
        }

        // Get current user
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());

        // Log the request
        log.info("Processing write-off for loan {} by user: {}. Amount: {}, Reason: {}",
                id, currentUser.getUsername(), dto.getWriteOffAmount(), dto.getWriteOffReason());

        // Process write-off
        WriteOffResponseDto response = loanDisbursementService.processWriteOff(id, dto, currentUser);

        // Log success
        log.info("Write-off processed successfully for loan {}. Status: {}",
                id, response.getWriteOffStatus());

        return ResponseEntity.ok(response);
    }


    @GetMapping("/write-offs/eligible")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<List<LoanDto>> getEligibleLoansForWriteOff() {
        log.info("Fetching loans eligible for write-off");
        List<LoanDto> eligibleLoans = loanDisbursementService.getEligibleLoansForWriteOff();
        return ResponseEntity.ok(eligibleLoans);
    }

    @GetMapping("/write-offs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Page<LoanDto>> getWrittenOffLoans(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String recoveryPlan,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "writeOffDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Fetching written-off loans with filters");

        WriteOffSearchCriteria criteria = new WriteOffSearchCriteria();
        criteria.setStatus(status);
        criteria.setBranchId(branchId);
        criteria.setStartDate(startDate);
        criteria.setEndDate(endDate);
        criteria.setRecoveryPlan(recoveryPlan);
        criteria.setSearchTerm(searchTerm);

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<LoanDto> loans = loanDisbursementService.getWrittenOffLoans(criteria, pageable);
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/write-offs/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<WriteOffSummaryDto> getWriteOffSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long branchId) {

        log.info("Fetching write-off summary");
        WriteOffSummaryDto summary = loanDisbursementService.getWriteOffSummary(startDate, endDate, branchId);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/{id}/write-off/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<WriteOffResponseDto> approveWriteOff(
            @PathVariable Long id,
            @RequestParam(required = false) String comments) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Approving write-off for loan {} by user: {}", id, currentUser.getUsername());

        WriteOffResponseDto response = loanDisbursementService.approveWriteOff(id, currentUser, comments);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/write-off/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<WriteOffResponseDto> rejectWriteOff(
            @PathVariable Long id,
            @RequestParam String reason) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Rejecting write-off for loan {} by user: {}", id, currentUser.getUsername());

        WriteOffResponseDto response = loanDisbursementService.rejectWriteOff(id, currentUser, reason);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/write-offs/report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> generateWriteOffReport(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String recoveryPlan) {

        log.info("Generating write-off report");

        WriteOffSearchCriteria criteria = new WriteOffSearchCriteria();
        criteria.setBranchId(branchId);
        criteria.setStartDate(startDate);
        criteria.setEndDate(endDate);
        criteria.setRecoveryPlan(recoveryPlan);

        byte[] reportContent = loanDisbursementService.generateWriteOffReport(criteria);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "write-off-report.pdf");

        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }

    /*
    End of Write Off endpoints
     */

    @GetMapping("/by-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT', 'COLLECTION_OFFICER') and " +
            "(@permissionCheckService.hasPermission('LOAN_VIEW_ALL') or @permissionCheckService.hasPermission('LOAN_VIEW_BRANCH') or hasPermission('LOAN_VIEW_OWN'))")
    public ResponseEntity<Page<LoanDto>> getLoansByStatus(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "disbursementDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Fetching loans by status: {}", status);

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Call service method with only status filter
        Page<LoanDto> loans = loanDisbursementService.getLoansByStatus(status, pageable);
        return ResponseEntity.ok(loans);
    }

    
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT', 'COLLECTION_OFFICER') and " +
                 "(@permissionCheckService.hasPermission('LOAN_VIEW_ALL') or @permissionCheckService.hasPermission('LOAN_VIEW_BRANCH') or hasPermission('LOAN_VIEW_OWN'))")
    public ResponseEntity<Page<LoanDto>> getLoans(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long borrowerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "disbursementDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        log.info("Fetching loans with filters - status: {}, branch: {}, borrower: {}", status, branchId, borrowerId);
        
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<LoanDto> loans = loanDisbursementService.getLoans(status, branchId, borrowerId, pageable);
        return ResponseEntity.ok(loans);
    }
    
  /*  @GetMapping("/stats/portfolio")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT') and @permissionCheckService.hasPermission('LOAN_VIEW_ALL')")
    public ResponseEntity<PortfolioSummaryDto> getPortfolioSummary(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        
        log.info("Fetching portfolio summary for branch: {}, as of: {}", branchId, asOfDate);
        
        PortfolioSummaryDto summary = loanDisbursementService.getPortfolioSummary(branchId, asOfDate);
        return ResponseEntity.ok(summary);
    }*/


    @GetMapping("/stats/portfolio")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT') and @permissionCheckService.hasPermission('LOAN_VIEW_ALL')")
    public ResponseEntity<PortfolioSummaryDto> getPortfolioSummary(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        log.info("========== PORTFOLIO SUMMARY REQUEST ==========");
        log.info("Request received at: {}", LocalDateTime.now());
        log.info("Branch ID: {}", branchId);
        log.info("As Of Date raw value: {}", asOfDate);
        log.info("As Of Date class: {}", asOfDate != null ? asOfDate.getClass().getName() : "null");

        // If asOfDate is null, use current date
        LocalDate effectiveDate = asOfDate != null ? asOfDate : LocalDate.now();
        log.info("Effective date being used: {}", effectiveDate);

        try {
            log.info("Calling service with branchId: {}, effectiveDate: {}", branchId, effectiveDate);
            PortfolioSummaryDto summary = loanDisbursementService.getPortfolioSummary(branchId, effectiveDate);
            log.info("Service returned successfully");
            log.info("==============================================");
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error in portfolio summary: {}", e.getMessage(), e);
            log.info("==============================================");
            throw e;
        }
    }


    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT', 'LOAN_OFFICER')")
    public ResponseEntity<List<LoanDto>> getRecentDisbursements(
            @RequestParam(defaultValue = "5") int limit) {
        log.info("Fetching {} recent disbursements", limit);
        List<LoanDto> recentDisbursements = loanDisbursementService.getRecentDisbursements(limit);
        return ResponseEntity.ok(recentDisbursements);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT', 'LOAN_OFFICER')")
    public ResponseEntity<DisbursementStatsDto> getDisbursementStatistics() {
        log.info("Fetching disbursement statistics");
        DisbursementStatsDto stats = loanDisbursementService.getDisbursementStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}/receipt-pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT', 'LOAN_OFFICER')")
    public ResponseEntity<byte[]> generateDisbursementReceiptPdf(@PathVariable Long id) {
        log.info("Generating disbursement receipt PDF for loan: {}", id);

        byte[] pdfContent = loanDisbursementService.generateDisbursementReceiptPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "disbursement-receipt-" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }

    @PostMapping("/bulk-disbursement")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT') and @permissionCheckService.hasPermission('LOAN_DISBURSE')")
    public ResponseEntity<BulkDisbursementResponseDto> processBulkDisbursement(
            @Valid @RequestBody BulkDisbursementRequestDto request) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info(">>> Processing bulk disbursement for {} loans by user: {}",
                request.getLoanIds().size(), currentUser.getUsername());

        BulkDisbursementResponseDto response = loanDisbursementService.processBulkDisbursement(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk-disbursement/validate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<LoanDto>> validateLoansForBulkDisbursement(
            @RequestBody List<Long> loanIds) {

        log.info("Validating {} loans for bulk disbursement", loanIds.size());
        List<LoanDto> validLoans = loanDisbursementService.getLoansForBulkDisbursement(loanIds);
        return ResponseEntity.ok(validLoans);
    }

    @GetMapping("/reports/disbursements")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> generateDisbursementReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "PDF") String format) {

        log.info("Generating disbursement report from {} to {} for branch: {}", startDate, endDate, branchId);

        byte[] reportContent = loanDisbursementService.generateDisbursementReport(startDate, endDate, branchId, format);

        HttpHeaders headers = new HttpHeaders();
        String contentType = format.equalsIgnoreCase("PDF") ? "application/pdf" :
                format.equalsIgnoreCase("EXCEL") ? "application/vnd.ms-excel" : "text/csv";
        String extension = format.toLowerCase();

        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("filename", "disbursement-report." + extension);

        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }

    @GetMapping("/reports/portfolio-summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> exportPortfolioSummary(
            @RequestParam(required = false) Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        log.info("Exporting portfolio summary as of {} for branch: {}", asOfDate, branchId);

        byte[] pdfContent = loanDisbursementService.exportPortfolioSummary(branchId, asOfDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "portfolio-summary.pdf");

        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }


}