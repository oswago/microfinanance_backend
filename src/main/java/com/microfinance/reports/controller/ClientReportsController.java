// src/main/java/com/microfinance/loanapplications/controller/ClientReportsController.java
package com.microfinance.reports.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.loanapplications.dto.report.*;

import com.microfinance.reports.service.ClientReportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/client-reports")
@RequiredArgsConstructor
@Tag(name = "Client Reports", description = "Client reports and analytics endpoints")
public class ClientReportsController {

    private final ClientReportsService clientReportsService;
    private final SecurityUtils securityUtils;

    @GetMapping("/demographics")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    @Operation(summary = "Get client demographics report")
    public ResponseEntity<ClientDemographicsReport> getClientDemographics(
            @RequestParam(required = false) Long branchId) {
        log.info("REST request to get client demographics for branch: {}", branchId);
        ClientDemographicsReport report = clientReportsService.getClientDemographics(branchId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/kyc-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    @Operation(summary = "Get KYC status report")
    public ResponseEntity<KycStatusReport> getKycStatusReport(
            @RequestParam(required = false) Long branchId) {
        log.info("REST request to get KYC status report for branch: {}", branchId);
        KycStatusReport report = clientReportsService.getKycStatusReport(branchId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/portfolio-summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Get portfolio summary report")
    public ResponseEntity<PortfolioSummaryReport> getPortfolioSummaryReport(
            @RequestParam(required = false) Long branchId) {
        log.info("REST request to get portfolio summary for branch: {}", branchId);
        PortfolioSummaryReport report = clientReportsService.getPortfolioSummaryReport(branchId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/group-performance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Get group performance report")
    public ResponseEntity<GroupPerformanceReport> getGroupPerformanceReport(
            @RequestParam(required = false) Long branchId) {
        log.info("REST request to get group performance for branch: {}", branchId);
        GroupPerformanceReport report = clientReportsService.getGroupPerformanceReport(branchId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/activities")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    @Operation(summary = "Get activity report")
    public ResponseEntity<ActivityReport> getActivityReport(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to get activity report for branch: {}, period: {} to {}", branchId, startDate, endDate);
        
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        
        ActivityReport report = clientReportsService.getActivityReport(branchId, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/risk-assessment")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'RISK_MANAGER')")
    @Operation(summary = "Get risk assessment report")
    public ResponseEntity<RiskAssessmentReport> getRiskAssessmentReport(
            @RequestParam(required = false) Long branchId) {
        log.info("REST request to get risk assessment for branch: {}", branchId);
        RiskAssessmentReport report = clientReportsService.getRiskAssessmentReport(branchId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/export/{reportType}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    @Operation(summary = "Export report")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable String reportType,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to export report: {} in format: {}", reportType, format);
        
        Map<String, Object> params = new HashMap<>();
        if (branchId != null) params.put("branchId", branchId);
        if (startDate != null) params.put("startDate", startDate);
        if (endDate != null) params.put("endDate", endDate);
        
        byte[] reportData = clientReportsService.exportReport(reportType, format, params);
        
        String contentType;
        String extension;
        if ("pdf".equalsIgnoreCase(format)) {
            contentType = MediaType.APPLICATION_PDF_VALUE;
            extension = "pdf";
        } else if ("csv".equalsIgnoreCase(format)) {
            contentType = "text/csv";
            extension = "csv";
        } else {
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            extension = "xlsx";
        }
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=" + reportType + "_report_" + LocalDate.now() + "." + extension)
            .body(reportData);
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    @Operation(summary = "Get recent reports")
    public ResponseEntity<Page<ReportHistoryDto>> getRecentReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("REST request to get recent reports");
        Pageable pageable = PageRequest.of(page, size, Sort.by("generatedAt").descending());
        Page<ReportHistoryDto> reports = clientReportsService.getRecentReports(pageable);
        return ResponseEntity.ok(reports);
    }


    @PostMapping("/save-configuration")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    @Operation(summary = "Save report configuration")
    public ResponseEntity<ReportConfigurationDto> saveReportConfiguration(
            @RequestBody SaveReportRequestDto request
          //  @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("REST request to save report configuration: {}", request.getName());
        Long userIdFetched = getCurrentUserId(); // Get from security context instead of header
        ReportConfigurationDto savedConfig = clientReportsService.saveReportConfiguration(request, userIdFetched);
        return ResponseEntity.ok(savedConfig);
    }

    @GetMapping("/saved-reports")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    @Operation(summary = "Get saved reports")
    public ResponseEntity<Page<ReportConfigurationDto>> getSavedReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
         //   @RequestHeader("X-User-Id") Long userId
    )
         {
             Long userIdFetched = getCurrentUserId(); // Get from security context instead of header
        log.info("REST request to get saved reports for user: {}", userIdFetched);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReportConfigurationDto> reports = clientReportsService.getSavedReports(userIdFetched, pageable);
        return ResponseEntity.ok(reports);
    }

    @PostMapping("/generate-from-config/{configId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    @Operation(summary = "Generate report from saved configuration")
    public ResponseEntity<byte[]> generateFromConfiguration(
            @PathVariable Long configId,
            @RequestParam(defaultValue = "pdf") String format
           // @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("REST request to generate report from configuration: {} in format: {}", configId, format);
        Long userIdFetched = getCurrentUserId(); // Get from security context instead of header
        byte[] reportData = clientReportsService.generateReportFromConfiguration(configId, userIdFetched, format);

        String contentType;
        String extension;
        if ("pdf".equalsIgnoreCase(format)) {
            contentType = MediaType.APPLICATION_PDF_VALUE;
            extension = "pdf";
        } else if ("csv".equalsIgnoreCase(format)) {
            contentType = "text/csv";
            extension = "csv";
        } else {
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            extension = "xlsx";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=report_" + System.currentTimeMillis() + "." + extension)
                .body(reportData);
    }

    @DeleteMapping("/delete-config/{configId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    @Operation(summary = "Delete saved report configuration")
    public ResponseEntity<Void> deleteReportConfiguration(
            @PathVariable Long configId
          //  @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("REST request to delete report configuration: {}", configId);
        Long userIdFetched = getCurrentUserId(); // Get from security context instead of header
        clientReportsService.deleteReportConfiguration(configId, userIdFetched);
        return ResponseEntity.noContent().build();
    }


    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            return user.getId();
        }
        // Fallback - get from custom security utils
        return securityUtils.getCurrentUserId();
    }



}