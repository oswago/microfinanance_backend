package com.microfinance.loanapplications.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanapplications.dto.earlyrepayment.*;
import com.microfinance.loanapplications.dto.earlyrepayment.EligibleLoanDto;
import com.microfinance.loanapplications.service.EarlyRepaymentService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/early-repayments")
@RequiredArgsConstructor
public class EarlyRepaymentController {

    private final EarlyRepaymentService earlyRepaymentService;
    private final SecurityUtils securityUtils;
    private final UserService userService;


    /**
     * Get eligible loans for early repayment
     * Returns loans that are active and eligible for early repayment
     */
    @GetMapping("/eligible-loans")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<List<EligibleLoanDto>> getEligibleLoans() {
        log.info("Fetching eligible loans for early repayment");

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        List<EligibleLoanDto> eligibleLoans = earlyRepaymentService.getEligibleLoans(currentUser);

        return ResponseEntity.ok(eligibleLoans);
    }



    /**
     * Validate if a loan is eligible for early repayment
     */
    @GetMapping("/validate/{loanId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<EligibilityResponseDto> validateLoanEligibility(@PathVariable Long loanId) {
        log.info("Validating loan eligibility for early repayment: {}", loanId);

        EligibilityResponseDto eligibility = earlyRepaymentService.validateLoanEligibility(loanId);
        return ResponseEntity.ok(eligibility);
    }

       //Calculate early repayment
    @GetMapping("/calculate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<EarlyRepaymentCalculationDto> calculateEarlyRepayment(
            @RequestParam Long loanId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) BigDecimal customDiscount) {

        log.info("Calculating early repayment for loan: {} with amount: {}, discount: {}", loanId, amount, customDiscount);
        EarlyRepaymentCalculationDto calculation = earlyRepaymentService.calculateEarlyRepayment(loanId, amount, customDiscount);
        return ResponseEntity.ok(calculation);
    }

    /**
     * Get early repayment calculation options
     */
    @GetMapping("/options/{loanId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<List<EarlyRepaymentOptionDto>> getEarlyRepaymentOptions(@PathVariable Long loanId) {
        log.info("Fetching early repayment options for loan: {}", loanId);

        List<EarlyRepaymentOptionDto> options = earlyRepaymentService.getEarlyRepaymentOptions(loanId);
        return ResponseEntity.ok(options);
    }


    /* Get early repayment fee structure for a loan product*/
    @GetMapping("/fee-structure/{loanProductId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<EarlyRepaymentFeeStructureDto> getEarlyRepaymentFeeStructure(
            @PathVariable Long loanProductId) {
        log.info("Fetching early repayment fee structure for loan product: {}", loanProductId);

        EarlyRepaymentFeeStructureDto feeStructure = earlyRepaymentService.getEarlyRepaymentFeeStructure(loanProductId);
        return ResponseEntity.ok(feeStructure);
    }



    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<EarlyRepaymentCalculationDto> calculateEarlyRepayment(
            @RequestParam Long loanId,
            @RequestParam(required = false) BigDecimal customDiscount) {

        log.info("Calculating early repayment for loan: {}", loanId);
        EarlyRepaymentCalculationDto calculation = earlyRepaymentService.calculateEarlyRepayment(loanId, customDiscount);
        return ResponseEntity.ok(calculation);
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<EarlyRepaymentRequestDto> createEarlyRepaymentRequest(
            @Valid @RequestBody CreateEarlyRepaymentRequestDto requestDto) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Creating early repayment request by user: {}", currentUser.getUsername());

        EarlyRepaymentRequestDto request = earlyRepaymentService.createEarlyRepaymentRequest(requestDto, currentUser);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<Page<EarlyRepaymentRequestDto>> getEarlyRepaymentRequests(
            @RequestParam(required = false) GeneralConfig.EarlyRepaymentStatus status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long loanProductId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "requestedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Fetching early repayment requests with filters");

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<EarlyRepaymentRequestDto> requests = earlyRepaymentService
                .getEarlyRepaymentRequests(status, branchId, loanProductId, search, pageable);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<EarlyRepaymentRequestDto> getEarlyRepaymentRequest(@PathVariable Long id) {
        log.info("Fetching early repayment request: {}", id);
        EarlyRepaymentRequestDto request = earlyRepaymentService.getEarlyRepaymentRequestById(id);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<EarlyRepaymentRequestDto> approveEarlyRepayment(
            @PathVariable Long id,
            @Valid @RequestBody ApproveEarlyRepaymentDto approveDto) {

        User approver = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Approving early repayment request: {} by user: {}", id, approver.getUsername());

        EarlyRepaymentRequestDto request = earlyRepaymentService.approveEarlyRepayment(id, approveDto, approver);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<EarlyRepaymentRequestDto> rejectEarlyRepayment(
            @PathVariable Long id,
            @Valid @RequestBody RejectEarlyRepaymentDto rejectDto) {

        User rejector = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Rejecting early repayment request: {} by user: {}", id, rejector.getUsername());

        EarlyRepaymentRequestDto request = earlyRepaymentService.rejectEarlyRepayment(id, rejectDto, rejector);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/requests/{id}/payment")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<EarlyRepaymentRequestDto> processEarlyRepaymentPayment(
            @PathVariable Long id,
            @Valid @RequestBody EarlyRepaymentPaymentDto paymentDto) {

        User processor = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Processing early repayment payment for request: {} by user: {}", id, processor.getUsername());

        EarlyRepaymentRequestDto request = earlyRepaymentService
                .processEarlyRepaymentPayment(id, paymentDto, processor);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<EarlyRepaymentStatisticsDto> getEarlyRepaymentStatistics() {
        log.info("Fetching early repayment statistics");
        EarlyRepaymentStatisticsDto stats = earlyRepaymentService.getEarlyRepaymentStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/requests/{id}/settlement-letter")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> generateSettlementLetter(@PathVariable Long id) {
        log.info("Generating settlement letter for early repayment request: {}", id);

        byte[] pdfBytes = earlyRepaymentService.generateSettlementLetter(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "settlement-letter-" + id + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/requests/bulk-approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<Void> bulkApproveRequests(@RequestBody List<Long> requestIds) {
        User approver = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Bulk approving {} early repayment requests by user: {}", requestIds.size(), approver.getUsername());

        for (Long id : requestIds) {
            ApproveEarlyRepaymentDto approveDto = ApproveEarlyRepaymentDto.builder()
                    .approvedBy(approver.getUsername())
                    .approvalDate(LocalDate.now())
                    .comments("Bulk approved")
                    .build();
            earlyRepaymentService.approveEarlyRepayment(id, approveDto, approver);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Get early repayment history with filters
     * Returns paginated list of completed/processed early repayments
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Page<EarlyRepaymentHistoryDto>> getEarlyRepaymentHistory(
            @RequestParam(required = false) GeneralConfig.EarlyRepaymentStatus status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long loanProductId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "requestedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Fetching early repayment history with filters - status: {}, branch: {}, dates: {} - {}",
                status, branchId, startDate, endDate);

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<EarlyRepaymentHistoryDto> history = earlyRepaymentService
                .getEarlyRepaymentHistory(status, branchId, loanProductId, startDate, endDate, search, pageable);
        return ResponseEntity.ok(history);
    }

    /**
     * Generate history report (PDF/Excel)
     */
    @GetMapping("/reports/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> generateHistoryReport(
            @RequestParam(required = false) GeneralConfig.EarlyRepaymentStatus status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long loanProductId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "PDF") String format) {

        log.info("Generating early repayment history report - format: {}, status: {}, branch: {}, dates: {} - {}",
                format, status, branchId, startDate, endDate);

        byte[] reportContent = earlyRepaymentService.generateHistoryReport(status, branchId, loanProductId, startDate, endDate, format);

        String contentType = format.equalsIgnoreCase("PDF") ? "application/pdf" : "application/vnd.ms-excel";
        String extension = format.toLowerCase();
        String filename = "early-repayment-history." + extension;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }

    /**
     * Generate summary report
     */
    @GetMapping("/reports/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> generateSummaryReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "MONTH") String groupBy,
            @RequestParam(defaultValue = "PDF") String format) {

        log.info("Generating early repayment summary report - format: {}, branch: {}, dates: {} - {}, groupBy: {}",
                format, branchId, startDate, endDate, groupBy);

        byte[] reportContent = earlyRepaymentService.generateSummaryReport(startDate, endDate, branchId, groupBy, format);

        String contentType = format.equalsIgnoreCase("PDF") ? "application/pdf" : "application/vnd.ms-excel";
        String extension = format.toLowerCase();
        String filename = "early-repayment-summary." + extension;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }

    /**
     * Generate detailed transactions report
     */
    @GetMapping("/reports/detailed")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> generateDetailedReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) GeneralConfig.EarlyRepaymentStatus status,
            @RequestParam(defaultValue = "PDF") String format) {

        log.info("Generating early repayment detailed report - format: {}, branch: {}, dates: {} - {}, status: {}",
                format, branchId, startDate, endDate, status);

        byte[] reportContent = earlyRepaymentService.generateDetailedReport(startDate, endDate, branchId, status, format);

        String contentType = format.equalsIgnoreCase("PDF") ? "application/pdf" : "application/vnd.ms-excel";
        String extension = format.toLowerCase();
        String filename = "early-repayment-detailed." + extension;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }

    /**
     * Generate discount analysis report
     */
    @GetMapping("/reports/discount-analysis")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> generateDiscountAnalysisReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) BigDecimal minDiscount,
            @RequestParam(required = false) BigDecimal maxDiscount,
            @RequestParam(defaultValue = "PDF") String format) {

        log.info("Generating early repayment discount analysis report - format: {}, branch: {}, dates: {} - {}, discount: {}-{}",
                format, branchId, startDate, endDate, minDiscount, maxDiscount);

        byte[] reportContent = earlyRepaymentService.generateDiscountAnalysisReport(startDate, endDate, branchId, minDiscount, maxDiscount, format);

        String contentType = format.equalsIgnoreCase("PDF") ? "application/pdf" : "application/vnd.ms-excel";
        String extension = format.toLowerCase();
        String filename = "early-repayment-discount-analysis." + extension;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }

    /**
     * Get recent reports list
     */
    @GetMapping("/reports/recent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<RecentReportDto>> getRecentReports() {
        log.info("Fetching recent early repayment reports");

        List<RecentReportDto> recentReports = earlyRepaymentService.getRecentReports();
        return ResponseEntity.ok(recentReports);
    }


}