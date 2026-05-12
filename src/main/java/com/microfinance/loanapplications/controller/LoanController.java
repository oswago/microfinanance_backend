package com.microfinance.loanapplications.controller;

import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.loanapplications.dto.*;
import com.microfinance.loanapplications.dto.collection.AssignOfficerRequestDto;
import com.microfinance.loanapplications.dto.collection.BulkAssignOfficerRequestDto;
import com.microfinance.loanapplications.dto.collection.BulkAssignResultDto;
import com.microfinance.loanapplications.dto.collection.LoanEligibleForRecoveryDto;
import com.microfinance.loanapplications.dto.disbursement.DisburseLoanDto;
import com.microfinance.loanapplications.dto.disbursement.DisbursementReceiptDto;
import com.microfinance.loanapplications.dto.disbursement.PortfolioSummaryDto;
import com.microfinance.loanapplications.dto.disbursement.WriteOffRequestDto;
import com.microfinance.loanapplications.dto.repayment.LoanEligibleForRepaymentDto;
import com.microfinance.loanapplications.dto.repayment.RepaymentReceiptDto;
import com.microfinance.loanapplications.dto.repayment.RepaymentScheduleDto;
import com.microfinance.loanapplications.dto.repayment.RepaymentRequestDto;
import com.microfinance.loanapplications.service.LoanService;
import com.microfinance.base.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    /**
     * Get all loans with filtering and pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT', 'COLLECTION_OFFICER') and " +
                  "(@permissionCheckService.hasPermission('LOAN_VIEW_ALL') or @permissionCheckService.hasPermission('LOAN_VIEW_BRANCH') or hasPermission('LOAN_VIEW_OWN'))")
    public ResponseEntity<Page<LoanDto>> getLoans(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long borrowerId,
            @RequestParam(required = false) Long loanProductId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Fetching loans with filters - status: {}, branch: {}, borrower: {}, fromDate: {}, toDate: {}",
                status, branchId, borrowerId, fromDate, toDate);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<LoanDto> loans = loanService.getLoans(status, branchId, borrowerId, loanProductId,
                fromDate, toDate,pageable);
        return ResponseEntity.ok(loans);
    }

    /**
     * Get loan by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT', 'COLLECTION_OFFICER') " )
                 // "(hasAuthority('LOAN_VIEW_ALL') or hasAuthority('LOAN_VIEW_BRANCH') or hasAuthority('LOAN_VIEW_OWN'))")
    public ResponseEntity<LoanDto> getLoanById(@PathVariable Long id) {
        log.info("Fetching loan by ID: {}", id);

        LoanDto loan = loanService.getLoanById(id);
        return ResponseEntity.ok(loan);
    }

    /**
     * Get loan by account number
     */
    @GetMapping("/account/{accountNumber}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT', 'COLLECTION_OFFICER') and" +
                  "(@permissionCheckService.hasPermission('LOAN_VIEW_ALL') or @permissionCheckService.hasPermission('LOAN_VIEW_BRANCH') or @permissionCheckService.hasPermission('LOAN_VIEW_OWN'))")
    public ResponseEntity<LoanDto> getLoanByAccountNumber(@PathVariable String accountNumber) {
        log.info("Fetching loan by account number: {}", accountNumber);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        LoanDto loan = loanService.getLoanByAccountNumber(accountNumber, currentUser);
        return ResponseEntity.ok(loan);
    }

    /**
     * Get loans by borrower
     */
    @GetMapping("/borrower/{borrowerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<List<LoanSummaryDto>> getLoansByBorrower(@PathVariable Long borrowerId) {
        log.info("Fetching loans for borrower: {}", borrowerId);

        List<LoanSummaryDto> loans = loanService.getLoansByBorrower(borrowerId);
        return ResponseEntity.ok(loans);
    }

    /**
     * Get loan summary statistics
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, Object>> getLoanSummary(
            @RequestParam(required = false) Long branchId) {
        log.info("Fetching loan summary for branch: {}", branchId);

        Map<String, Object> summary = loanService.getLoanSummary(branchId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get repayment schedule for a loan
     */
    @GetMapping("/{id}/repayment-schedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<RepaymentScheduleDto>> getRepaymentSchedule(@PathVariable Long id) {
        log.info("Fetching repayment schedule for loan: {}", id);

        List<RepaymentScheduleDto> schedule = loanService.getRepaymentSchedule(id);
        return ResponseEntity.ok(schedule);
    }

    /**
     * Make a repayment
     */
    @PostMapping("/{id}/repay")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'COLLECTION_OFFICER') and " +
                  "@permissionCheckService.hasPermission('LOAN_REPAY')")
    public ResponseEntity<RepaymentReceiptDto> makeRepayment(
            @PathVariable Long id,
            @Valid @RequestBody RepaymentRequestDto request) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Processing repayment for loan {} by user: {}", id, currentUser.getUsername());

        RepaymentReceiptDto receipt = loanService.makeRepayment(id, request, currentUser);
        return ResponseEntity.ok(receipt);
    }

    /**
     * Get repayment history
     */
    @GetMapping("/{id}/repayments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<RepaymentReceiptDto>> getRepaymentHistory(@PathVariable Long id) {
        log.info("Fetching repayment history for loan: {}", id);

        List<RepaymentReceiptDto> history = loanService.getRepaymentHistory(id);
        return ResponseEntity.ok(history);
    }

    /**
     * Request loan reschedule
     */
    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER') and @permissionCheckService.hasPermission('LOAN_RESCHEDULE')")
    public ResponseEntity<LoanRescheduleResponseDto> requestReschedule(
            @PathVariable Long id,
            @Valid @RequestBody LoanRescheduleRequestDto request) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Processing reschedule request for loan {} by user: {}", id, currentUser.getUsername());

        LoanRescheduleResponseDto response = loanService.requestReschedule(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Request loan restructure
     */
    @PostMapping("/{id}/restructure")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER') and @permissionCheckService.hasPermission('LOAN_RESTRUCTURE')")
    public ResponseEntity<LoanRestructureResponseDto> requestRestructure(
            @PathVariable Long id,
            @Valid @RequestBody LoanRestructureRequestDto request) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Processing restructure request for loan {} by user: {}", id, currentUser.getUsername());

        LoanRestructureResponseDto response = loanService.requestRestructure(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Get loan documents
     */
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<List<LoanDocumentDto>> getLoanDocuments(@PathVariable Long id) {
        log.info("Fetching documents for loan: {}", id);

        List<LoanDocumentDto> documents = loanService.getLoanDocuments(id);
        return ResponseEntity.ok(documents);
    }

    /**
     * Get loan audit trail
     */
    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'AUDITOR')")
    public ResponseEntity<List<LoanAuditDto>> getLoanAuditTrail(@PathVariable Long id) {
        log.info("Fetching audit trail for loan: {}", id);

        List<LoanAuditDto> auditTrail = loanService.getLoanAuditTrail(id);
        return ResponseEntity.ok(auditTrail);
    }

    /**
     * Get loan statistics
     */
    @GetMapping("/stats/portfolio")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<PortfolioSummaryDto> getPortfolioSummary(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        log.info("Fetching portfolio summary for branch: {}, as of: {}", branchId, asOfDate);

        PortfolioSummaryDto summary = loanService.getPortfolioSummary(branchId, asOfDate);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get delinquent loans
     */
    @GetMapping("/delinquent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<Page<LoanDto>> getDelinquentLoans(
            @RequestParam(required = false) Integer daysOverdue,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching delinquent loans with days overdue: {}", daysOverdue);

        Pageable pageable = PageRequest.of(page, size, Sort.by("daysDelinquent").descending());
        Page<LoanDto> loans = loanService.getDelinquentLoans(daysOverdue, branchId, pageable);
        return ResponseEntity.ok(loans);
    }


    /**
     * Get loans eligible for repayment (active loans with outstanding balance)
     */
    @GetMapping("/eligible-for-repayment")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<LoanEligibleForRepaymentDto>> getLoansEligibleForRepayment(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long loanProductId,
            @RequestParam(required = false) String status) {

        log.info("Fetching loans eligible for repayment - branch: {}, product: {}, status: {}",
                branchId, loanProductId, status);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        List<LoanEligibleForRepaymentDto> loans = loanService.getLoansEligibleForRepayment(
                branchId, loanProductId, status, currentUser);

        return ResponseEntity.ok(loans);
    }



    /**
     * Get loans eligible for recovery (overdue loans not already in recovery)
     */
    @GetMapping("/eligible-for-recovery")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<LoanEligibleForRecoveryDto>> getLoansEligibleForRecovery() {
        log.info("Fetching loans eligible for recovery");

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        List<LoanEligibleForRecoveryDto> loans = loanService.getLoansEligibleForRecovery(currentUser);

        return ResponseEntity.ok(loans);
    }




    /**
     * Get overdue loans with filtering options
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'COLLECTION_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<Page<OverdueLoanDto>> getOverdueLoans(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long loanOfficerId,
            @RequestParam(required = false) Integer minDaysOverdue,
            @RequestParam(required = false) Integer maxDaysOverdue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "daysDelinquent") String sortBy,  // Changed from daysOverdue
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Fetching overdue loans for date: {}, branch: {}, officer: {}, minDays: {}, maxDays: {}",
                date, branchId, loanOfficerId, minDaysOverdue, maxDaysOverdue);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OverdueLoanDto> overdueLoans = loanService.getOverdueLoans(
                date, branchId, loanOfficerId, minDaysOverdue, maxDaysOverdue, currentUser, pageable);

        return ResponseEntity.ok(overdueLoans);
    }



    /**
     * Assign a collection officer to a loan
     */
    @PostMapping("/{loanId}/assign-officer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER') and " +
            "@permissionCheckService.hasPermission('COLLECTION_ASSIGN')")
    public ResponseEntity<LoanDto> assignCollectionOfficer(
            @PathVariable Long loanId,
            @Valid @RequestBody AssignOfficerRequestDto request) {

        log.info("Assigning collection officer {} to loan {}", request.getOfficerId(), loanId);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        LoanDto updatedLoan = loanService.assignCollectionOfficer(loanId, request.getOfficerId(), currentUser);

        return ResponseEntity.ok(updatedLoan);
    }

    /**
     * Unassign collection officer from a loan
     */
    @DeleteMapping("/{loanId}/assign-officer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER') and " +
            "@permissionCheckService.hasPermission('COLLECTION_ASSIGN')")
    public ResponseEntity<LoanDto> unassignCollectionOfficer(@PathVariable Long loanId) {

        log.info("Unassigning collection officer from loan {}", loanId);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        LoanDto updatedLoan = loanService.unassignCollectionOfficer(loanId, currentUser);

        return ResponseEntity.ok(updatedLoan);
    }

    /**
     * Bulk assign collection officers to multiple loans
     */
    @PostMapping("/bulk-assign-officers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER') and " +
            "@permissionCheckService.hasPermission('COLLECTION_BULK_ASSIGN')")
    public ResponseEntity<BulkAssignResultDto> bulkAssignCollectionOfficers(
            @Valid @RequestBody BulkAssignOfficerRequestDto request) {

        log.info("Bulk assigning collection officer {} to {} loans",
                request.getOfficerId(), request.getLoanIds().size());

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        BulkAssignResultDto result = loanService.bulkAssignCollectionOfficers(
                request.getLoanIds(), request.getOfficerId(), request.getNotes(), currentUser);

        return ResponseEntity.ok(result);
    }

    /**
     * Get loans assigned to a specific collection officer
     */
    @GetMapping("/assigned-to-officer/{officerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER')")
    public ResponseEntity<Page<LoanDto>> getLoansByCollectionOfficer(
            @PathVariable Long officerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching loans assigned to collection officer: {}", officerId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<LoanDto> loans = loanService.getLoansByCollectionOfficer(officerId, status, pageable);

        return ResponseEntity.ok(loans);
    }






}