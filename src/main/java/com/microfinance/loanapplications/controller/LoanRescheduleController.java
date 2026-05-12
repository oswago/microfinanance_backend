package com.microfinance.loanapplications.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microfinance.base.service.UserService;
import com.microfinance.exception.BusinessException;
import com.microfinance.loanapplications.dto.LoanDto;
import com.microfinance.loanapplications.dto.RescheduleRequestDto;
import com.microfinance.loanapplications.dto.disbursement.RescheduleApprovalDto;
import com.microfinance.loanapplications.dto.disbursement.RescheduleEligibilityDto;
import com.microfinance.loanapplications.dto.approval.ApprovalDecisionDto;
import com.microfinance.loanapplications.dto.rescheduling.*;
import com.microfinance.loanapplications.service.LoanRescheduleService;
import com.microfinance.base.entity.User;
import com.microfinance.base.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/loan-rescheduling")
@RequiredArgsConstructor
public class LoanRescheduleController {
    
    private final LoanRescheduleService loanRescheduleService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    @PostMapping("/loans/{loanId}/reschedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER') and @permissionCheckService.hasPermission('LOAN_RESCHEDULE_APPROVE')")
    public ResponseEntity<LoanDto> rescheduleLoan(
            @PathVariable Long loanId,
            @Valid @RequestBody RescheduleRequestDto dto) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Direct rescheduling of loan {} by user: {}", loanId, currentUser.getUsername());

        LoanDto loan = loanRescheduleService.rescheduleLoan(loanId, dto, currentUser);
        return ResponseEntity.ok(loan);
    }
    
    @PostMapping("/{loanId}/submit-request")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER') and @permissionCheckService.hasPermission('LOAN_RESCHEDULE_REQUEST')")
    public ResponseEntity<RescheduleApprovalDto> submitRescheduleRequest(
            @PathVariable Long loanId,
            @Valid @RequestBody RescheduleRequestDto dto) {
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Submitting reschedule request for loan {} by user: {}", loanId, currentUser.getUsername());
        
        RescheduleApprovalDto request = loanRescheduleService.submitRescheduleRequest(loanId, dto, currentUser);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER') and @permissionCheckService.hasPermission('LOAN_RESCHEDULE_APPROVE')")
    public ResponseEntity<Page<RescheduleApprovalDto>> getReschedulingRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "requestDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Fetching rescheduling requests with filters - status: {}, branch: {}, dates: {} - {}",
                status, branchId, startDate, endDate);

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RescheduleApprovalDto> requests = loanRescheduleService.getReschedulingRequests(
                status, branchId, startDate, endDate, pageable);
        return ResponseEntity.ok(requests);
    }

    
    @PostMapping("/requests/{requestId}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER') and @permissionCheckService.hasPermission('LOAN_RESCHEDULE_APPROVE')")
    public ResponseEntity<RescheduleApprovalDto> rejectRescheduleRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalDecisionDto dto) {
        
        User approver = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Rejecting reschedule request {} by user: {}", requestId, approver.getUsername());
        
        RescheduleApprovalDto rejection = loanRescheduleService.rejectReschedule(requestId, dto, approver);
        return ResponseEntity.ok(rejection);
    }
    
    @GetMapping("/loans/{loanId}/eligibility")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER') and " +
                 "(@permissionCheckService.hasPermission('LOAN_RESCHEDULE_REQUEST') or @permissionCheckService.hasPermission('LOAN_RESCHEDULE_APPROVE'))")
    public ResponseEntity<RescheduleEligibilityDto> checkRescheduleEligibility(@PathVariable Long loanId) {
        
        log.info("Checking reschedule eligibility for loan: {}", loanId);
        
        RescheduleEligibilityDto eligibility = loanRescheduleService.checkRescheduleEligibility(loanId);
        return ResponseEntity.ok(eligibility);
    }
    
    @GetMapping("/loans/{loanId}/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER') and " +
                 "(@permissionCheckService.hasPermission('LOAN_RESCHEDULE_REQUEST') or @permissionCheckService.hasPermission('LOAN_RESCHEDULE_APPROVE'))")
    public ResponseEntity<List<RescheduleApprovalDto>> getRescheduleHistory(@PathVariable Long loanId) {
        
        log.info("Fetching reschedule history for loan: {}", loanId);
        
        List<RescheduleApprovalDto> history = loanRescheduleService.getRescheduleHistory(loanId);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/requests/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER') and @permissionCheckService.hasPermission('LOAN_RESCHEDULE_APPROVE')")
    public ResponseEntity<List<RescheduleApprovalDto>> getPendingRescheduleRequests() {
        
        log.info("Fetching pending reschedule requests");
        
        List<RescheduleApprovalDto> pendingRequests = loanRescheduleService.getPendingRescheduleRequests();
        return ResponseEntity.ok(pendingRequests);
    }
    
    @GetMapping("/requests-no-pagination")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER') and @permissionCheckService.hasPermission('LOAN_RESCHEDULE_APPROVE')")
    public ResponseEntity<Page<RescheduleApprovalDto>> getRescheduleRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "requestDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        log.info("Fetching reschedule requests with status: {}", status);
        
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<RescheduleApprovalDto> requests = loanRescheduleService.getRescheduleRequestsByStatus(status, pageable);
        return ResponseEntity.ok(requests);
    }
    
    @GetMapping("/requests/{requestId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER') and " +
                 "(@permissionCheckService.hasPermission('LOAN_RESCHEDULE_REQUEST') or @permissionCheckService.hasPermission('LOAN_RESCHEDULE_APPROVE'))")
    public ResponseEntity<RescheduleApprovalDto> getRescheduleRequest(@PathVariable Long requestId) {
        
        log.info("Fetching reschedule request: {}", requestId);
        
        RescheduleApprovalDto request = loanRescheduleService.getRescheduleRequestById(requestId);
        return ResponseEntity.ok(request);
    }
    
    @GetMapping("/reasons")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER') and " +
                 "(@permissionCheckService.hasPermission('LOAN_RESCHEDULE_REQUEST') or @permissionCheckService.hasPermission('LOAN_RESCHEDULE_APPROVE'))")
    public ResponseEntity<List<String>> getValidRescheduleReasons() {
        
        log.info("Fetching valid reschedule reasons");
        
        List<String> reasons = loanRescheduleService.getValidRescheduleReasons();
        return ResponseEntity.ok(reasons);
    }
    
    @GetMapping("/loans/{loanId}/can-reschedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER') and " +
                 "(@permissionCheckService.hasPermission('LOAN_RESCHEDULE_REQUEST') or @permissionCheckService.hasPermission('LOAN_RESCHEDULE_APPROVE'))")
    public ResponseEntity<Boolean> canLoanBeRescheduled(@PathVariable Long loanId) {
        
        log.info("Checking if loan can be rescheduled: {}", loanId);
        
        boolean canReschedule = loanRescheduleService.canLoanBeRescheduled(loanId);
        return ResponseEntity.ok(canReschedule);
    }


    // Get rescheduling statistics
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<RescheduleStatisticsDto> getReschedulingStatistics() {
        log.info("Fetching rescheduling statistics");

        RescheduleStatisticsDto statistics = loanRescheduleService.getReschedulingStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/analytics/report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<byte[]> generateAnalyticsReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long branchId) {

        log.info("Generating analytics report from {} to {} for branch: {}", startDate, endDate, branchId);

        byte[] reportContent = loanRescheduleService.generateAnalyticsReport(startDate, endDate, branchId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = "rescheduling-analytics-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
        headers.setContentDispositionFormData("filename", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }

    // Search loans eligible for rescheduling
    @GetMapping("/eligible-loans")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<List<EligibleLoanDto>> searchLoansForRescheduling(
            @RequestParam(required = false) String search) {

        log.info("Searching eligible loans for rescheduling with term: {}", search);

        List<EligibleLoanDto> eligibleLoans = loanRescheduleService.searchEligibleLoans(search);
        return ResponseEntity.ok(eligibleLoans);
    }

    // Create rescheduling request with documents
    @PostMapping(value = "/requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER') and @permissionCheckService.hasPermission('LOAN_RESCHEDULE_REQUEST')")
    public ResponseEntity<RescheduleApprovalDto> createReschedulingRequest(
            HttpServletRequest request,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());

        // Log all parts
        try {
            if (request instanceof MultipartHttpServletRequest) {
                MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
                Map<String, String[]> parameterMap = multipartRequest.getParameterMap();
                log.info("Parameter map: {}", parameterMap);

                String requestJson = multipartRequest.getParameter("request");
                log.info("Request JSON: {}", requestJson);

                // Parse JSON
                ObjectMapper objectMapper = new ObjectMapper();
                CreateReschedulingRequestDto requestDto = objectMapper.readValue(requestJson, CreateReschedulingRequestDto.class);

                log.info("Creating rescheduling request for loan: {} by user: {}",
                        requestDto.getLoanId(), currentUser.getUsername());

                RescheduleApprovalDto result = loanRescheduleService.createReschedulingRequest(
                        requestDto, documents, currentUser);
                return ResponseEntity.status(HttpStatus.CREATED).body(result);
            }
        } catch (Exception e) {
            log.error("Error parsing request", e);
            throw new BusinessException("Failed to parse request: " + e.getMessage());
        }

        throw new BusinessException("Invalid request format");
    }

    // Get rescheduling request by ID
    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<RescheduleApprovalDto> getReschedulingRequestById(@PathVariable Long id) {
        log.info("Fetching rescheduling request: {}", id);

        RescheduleApprovalDto request = loanRescheduleService.getReschedulingRequestById(id);
        return ResponseEntity.ok(request);
    }

    // Approve request
    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER') and @permissionCheckService.hasPermission('LOAN_RESCHEDULE_APPROVE')")
    public ResponseEntity<RescheduleApprovalDto> approveRequest(
            @PathVariable Long id,
            @Valid @RequestBody ApproveRejectRequestDto requestDto) {

        User approver = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Approving rescheduling request {} by user: {}", id, approver.getUsername());

        RescheduleApprovalDto approval = loanRescheduleService.approveReschedulingRequest(id, requestDto, approver);
        return ResponseEntity.ok(approval);
    }

    // Reject request
    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER') and @permissionCheckService.hasPermission('LOAN_RESCHEDULE_APPROVE')")
    public ResponseEntity<RescheduleApprovalDto> rejectRequest(
            @PathVariable Long id,
            @Valid @RequestBody ApproveRejectRequestDto requestDto) {

        User approver = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Rejecting rescheduling request {} by user: {}", id, approver.getUsername());

        RescheduleApprovalDto rejection = loanRescheduleService.rejectReschedulingRequest(id, requestDto, approver);
        return ResponseEntity.ok(rejection);
    }

    // Request more information
    @PostMapping("/requests/{id}/request-info")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<RescheduleApprovalDto> requestMoreInfo(
            @PathVariable Long id,
            @RequestBody RequestMoreInfoDto requestDto) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Requesting more info for rescheduling request {} by user: {}", id, currentUser.getUsername());

        RescheduleApprovalDto updatedRequest = loanRescheduleService.requestMoreInfo(
                id, requestDto.getMessage(), currentUser);
        return ResponseEntity.ok(updatedRequest);
    }

    // Cancel request
    @PostMapping("/requests/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<RescheduleApprovalDto> cancelRequest(@PathVariable Long id) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Cancelling rescheduling request {} by user: {}", id, currentUser.getUsername());

        RescheduleApprovalDto cancelledRequest = loanRescheduleService.cancelReschedulingRequest(id, currentUser);
        return ResponseEntity.ok(cancelledRequest);
    }

    // Get rescheduling history for a loan
    @GetMapping("/history/{loanId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<RescheduleApprovalDto>> getReschedulingHistory(@PathVariable Long loanId) {
        log.info("Fetching rescheduling history for loan: {}", loanId);

        List<RescheduleApprovalDto> history = loanRescheduleService.getReschedulingHistory(loanId);
        return ResponseEntity.ok(history);
    }

    // Generate rescheduling report
    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<byte[]> generateReschedulingReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long branchId) {

        log.info("Generating rescheduling report from {} to {} for branch: {}", startDate, endDate, branchId);

        byte[] reportContent = loanRescheduleService.generateReschedulingReport(startDate, endDate, branchId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "rescheduling-report.pdf");

        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }


    @GetMapping("/requests/{id}/report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<byte[]> generateSingleReschedulingReport(@PathVariable Long id) {

        log.info("Generating rescheduling report for request ID: {}", id);

        byte[] reportContent = loanRescheduleService.generateSingleReschedulingReport(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "rescheduling-request-" + id + "-report.pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }


    // Get document
    @GetMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<ReschedulingDocumentDto> getDocument(@PathVariable Long documentId) {
        log.info("Fetching document: {}", documentId);

        ReschedulingDocumentDto document = loanRescheduleService.getDocument(documentId);
        return ResponseEntity.ok(document);
    }

    // Download document
    @GetMapping("/documents/{documentId}/download")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long documentId) {
        log.info("Downloading document: {}", documentId);

        byte[] documentContent = loanRescheduleService.downloadDocument(documentId);
        ReschedulingDocumentDto document = loanRescheduleService.getDocument(documentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", document.getFileName());

        return new ResponseEntity<>(documentContent, headers, HttpStatus.OK);
    }



}