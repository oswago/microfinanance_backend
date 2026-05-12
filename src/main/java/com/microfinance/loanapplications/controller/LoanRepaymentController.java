package com.microfinance.loanapplications.controller;

import com.beust.jcommander.Parameter;
import com.microfinance.base.service.UserService;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.loanapplications.dto.*;
import com.microfinance.loanapplications.dto.disbursement.LoanRepaymentDto;
import com.microfinance.loanapplications.dto.repayment.*;
import com.microfinance.loanapplications.dto.repayment.DailyCollectionDto;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.repository.LoanRepository;
import com.microfinance.loanapplications.service.LoanRepaymentService;
import com.microfinance.base.entity.User;
import com.microfinance.base.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/loan-repayments")
@RequiredArgsConstructor
public class LoanRepaymentController {
    
    private final LoanRepaymentService loanRepaymentService;
    private final SecurityUtils securityUtils;
    private final UserService userService;
    private final LoanRepository loanRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'ACCOUNTANT', 'COLLECTION_OFFICER') and @permissionCheckService.hasPermission('REPAYMENT_RECORD')")
    public ResponseEntity<RepaymentReceiptDto> recordRepayment(
            @Valid @RequestBody RepaymentDto dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Recording repayment for loan: {} by user: {}", dto.getLoanId(), currentUser.getUsername());
        
        RepaymentReceiptDto receipt = loanRepaymentService.recordRepayment(dto, currentUser);
        return ResponseEntity.ok(receipt);
    }


    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT') and @permissionCheckService.hasPermission('REPAYMENT_BULK')")
    public ResponseEntity<BulkRepaymentResultDto> recordBulkRepayments(
            @Valid @RequestBody List<RepaymentDto> repayments) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Recording bulk repayments for {} loans by user: {}", repayments.size(), currentUser.getUsername());
        
        BulkRepaymentResultDto result = loanRepaymentService.recordBulkRepayments(repayments, currentUser);
        return ResponseEntity.ok(result);
    }



    @GetMapping("/loan/{loanId}/schedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'ACCOUNTANT', 'COLLECTION_OFFICER') and " +
                 "(@permissionCheckService.hasPermission('REPAYMENT_READ') or @permissionCheckService.hasPermission('REPAYMENT_VIEW_ALL') or @permissionCheckService.hasPermission('REPAYMENT_VIEW_BRANCH'))")
    public ResponseEntity<List<RepaymentScheduleDto>> getRepaymentSchedule(@PathVariable Long loanId) {
        
        log.info("Fetching repayment schedule for loan: {}", loanId);
        
        List<RepaymentScheduleDto> schedule = loanRepaymentService.getRepaymentSchedule(loanId);
        return ResponseEntity.ok(schedule);
    }
    
    @GetMapping("/loan/{loanId}/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'ACCOUNTANT', 'COLLECTION_OFFICER') and " +
                 "(@permissionCheckService.hasPermission('REPAYMENT_READ') or @permissionCheckService.hasPermission('REPAYMENT_VIEW_ALL') or @permissionCheckService.hasPermission('REPAYMENT_VIEW_BRANCH'))")
    public ResponseEntity<Page<LoanRepaymentDto>> getRepaymentHistory(
            @PathVariable Long loanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        log.info("Fetching repayment history for loan: {}", loanId);
        
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<LoanRepaymentDto> repayments = loanRepaymentService.getRepaymentHistory(loanId, pageable);
        return ResponseEntity.ok(repayments);
    }

    /**
     * Export repayment history for a loan
     */
    @GetMapping("/{loanId}/repayments/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'ACCOUNTANT', 'COLLECTION_OFFICER') and " +
            "(@permissionCheckService.hasPermission('REPAYMENT_READ') or @permissionCheckService.hasPermission('REPAYMENT_VIEW_ALL') or @permissionCheckService.hasPermission('REPAYMENT_VIEW_BRANCH'))")
    public ResponseEntity<byte[]> exportRepaymentHistory(
            @PathVariable Long loanId,
            @RequestParam(defaultValue = "PDF") String format,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size,
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            HttpServletResponse response) {

        log.info("Exporting repayment history for loan: {} in format: {}", loanId, format);

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<LoanRepaymentDto> repayments = loanRepaymentService.getRepaymentHistory(loanId, pageable);

        byte[] reportContent = loanRepaymentService.exportRepaymentHistory(repayments, loanId, format);

        String contentType = format.equalsIgnoreCase("PDF") ? "application/pdf" : "application/vnd.ms-excel";
        String extension = format.toLowerCase();
        String filename = "repayment-history-loan-" + loanId + "." + extension;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }


    @GetMapping("/borrower/{borrowerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'ACCOUNTANT', 'COLLECTION_OFFICER') and " +
                 "(@permissionCheckService.hasPermission('REPAYMENT_READ') or @permissionCheckService.hasPermission('REPAYMENT_VIEW_ALL') or @permissionCheckService.hasPermission('REPAYMENT_VIEW_BRANCH'))")
    public ResponseEntity<Page<LoanRepaymentDto>> getRepaymentHistoryByBorrower(
            @PathVariable Long borrowerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        log.info("Fetching repayment history for borrower: {}", borrowerId);
        
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<LoanRepaymentDto> repayments = loanRepaymentService.getRepaymentHistoryByBorrower(borrowerId, pageable);
        return ResponseEntity.ok(repayments);
    }
    
    @GetMapping("/loan/{loanId}/early-repayment")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'ACCOUNTANT') and @permissionCheckService.hasPermission('EARLY_REPAYMENT_PROCESS')")
    public ResponseEntity<EarlyRepaymentQuoteDto> calculateEarlyRepaymentAmount(@PathVariable Long loanId) {
        
        log.info("Calculating early repayment amount for loan: {}", loanId);
        
        EarlyRepaymentQuoteDto quote = loanRepaymentService.calculateEarlyRepaymentAmount(loanId);
        return ResponseEntity.ok(quote);
    }
    
    @PostMapping("/{repaymentId}/reverse")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT') and @permissionCheckService.hasPermission('REPAYMENT_REVERSE')")
    public ResponseEntity<RepaymentReceiptDto> reverseRepayment(
            @PathVariable Long repaymentId,
            @RequestBody ReverseRepaymentDto dto) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Reversing repayment {} by user: {}", repaymentId, currentUser.getUsername());
        
        RepaymentReceiptDto receipt = loanRepaymentService.reverseRepayment(repaymentId, dto.getReason(), currentUser);
        return ResponseEntity.ok(receipt);
    }
    
    @PostMapping("/{repaymentId}/waive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER') and @permissionCheckService.hasPermission('REPAYMENT_WAIVE')")
    public ResponseEntity<RepaymentReceiptDto> waiveRepayment(
            @PathVariable Long repaymentId,
            @RequestBody WaiveRepaymentDto dto) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Waiving repayment {} by user: {}", repaymentId, currentUser.getUsername());
        
        RepaymentReceiptDto receipt = loanRepaymentService.waiveRepayment(repaymentId, dto, currentUser);
        return ResponseEntity.ok(receipt);
    }
    
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER', 'LOAN_OFFICER') and " +
                 "(@permissionCheckService.hasPermission('REPAYMENT_READ') or @permissionCheckService.hasPermission('REPAYMENT_VIEW_ALL') or @permissionCheckService.hasPermission('REPAYMENT_VIEW_BRANCH'))")
    public ResponseEntity<Page<OverdueInstallmentDto>> getOverdueInstallments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        
        log.info("Fetching overdue installments for date: {}, branch: {}", date, branchId);
        
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<OverdueInstallmentDto> installments = loanRepaymentService.getOverdueInstallments(date, branchId, pageable);
        return ResponseEntity.ok(installments);
    }
    
    @GetMapping("/daily-collection")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT') and @permissionCheckService.hasPermission('REPAYMENT_READ')")
    public ResponseEntity<DailyCollectionDto> getDailyCollectionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long officerId) {
        
        log.info("Fetching daily collection report for date: {}, branch: {}, officer: {}", date, branchId, officerId);
        
        DailyCollectionDto report = loanRepaymentService.getDailyCollectionReport(date, branchId, officerId);
        return ResponseEntity.ok(report);
    }

    // NEW ENDPOINT: Get repayment by receipt number
    @GetMapping("/receipt/{receiptNumber}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'ACCOUNTANT', 'COLLECTION_OFFICER') and " +
            "(@permissionCheckService.hasPermission('REPAYMENT_READ') or @permissionCheckService.hasPermission('REPAYMENT_VIEW_ALL') or @permissionCheckService.hasPermission('REPAYMENT_VIEW_BRANCH'))")
    public ResponseEntity<LoanRepaymentDto> getRepaymentByReceipt(
            @PathVariable String receiptNumber) {

        log.info("Fetching repayment by receipt number: {}", receiptNumber);

        // You might need to add this method to your service
        // For now, this is a placeholder
        throw new UnsupportedOperationException("Get repayment by receipt not yet implemented");
    }

    // NEW ENDPOINT: Validate repayment allocation
    @PostMapping("/validate-allocation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'ACCOUNTANT') and @permissionCheckService.hasPermission('REPAYMENT_RECORD')")
    public ResponseEntity<RepaymentAllocationDto> validateRepaymentAllocation(
            @Valid @RequestBody RepaymentValidationDto validationDto) {

        log.info("Validating repayment allocation for loan: {}, amount: {}",
                validationDto.getLoanId(), validationDto.getAmount());

        // You might need to add this method to your service
        // For now, this is a placeholder
        throw new UnsupportedOperationException("Repayment allocation validation not yet implemented");
    }

    // Add these to LoanRepaymentController

    @GetMapping("/stats/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<RepaymentStatisticsDto> getRepaymentStatistics() {
        log.info("Fetching repayment statistics");
        RepaymentStatisticsDto stats = loanRepaymentService.getRepaymentStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<LoanRepaymentDto>> getRecentRepayments(
            @RequestParam(defaultValue = "5") int limit) {
        log.info("Fetching {} recent repayments", limit);
        List<LoanRepaymentDto> recentRepayments = loanRepaymentService.getRecentRepayments(limit);
        return ResponseEntity.ok(recentRepayments);
    }

    @GetMapping("/calculate-allocation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<RepaymentAllocationDto> calculateRepaymentAllocation(
            @RequestParam Long loanId,
            @RequestParam BigDecimal amount) {
        log.info("Calculating repayment allocation for loan: {}, amount: {}", loanId, amount);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        RepaymentAllocationDto allocation = loanRepaymentService.calculateRepaymentAllocation(loan, amount);
        return ResponseEntity.ok(allocation);
    }

    @GetMapping("/collection-performance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<CollectionPerformanceDto> getCollectionPerformance(
            @RequestParam(required = false) Long officerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Fetching collection performance for officer: {}, period: {} to {}", officerId, startDate, endDate);

        CollectionPerformanceDto performance = loanRepaymentService.getCollectionPerformance(officerId, startDate, endDate);
        return ResponseEntity.ok(performance);
    }



}