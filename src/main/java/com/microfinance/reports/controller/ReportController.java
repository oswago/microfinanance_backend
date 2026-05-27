// controller/ReportController.java
package com.microfinance.reports.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.reports.dto.*;
import com.microfinance.reports.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    @GetMapping("/financial")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<FinancialReportDto> getFinancialReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String period) {
        
        log.info("Generating financial report");
        
        ReportFilterDto filter = ReportFilterDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .period(period)
                .build();
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        FinancialReportDto report = reportService.generateFinancialReport(filter, currentUser);
        
        return ResponseEntity.ok(report);
    }

    @GetMapping("/portfolio")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'PORTFOLIO_MANAGER')")
    public ResponseEntity<PortfolioReportDto> getPortfolioReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String period) {
        
        log.info("Generating portfolio report");
        
        ReportFilterDto filter = ReportFilterDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .period(period)
                .build();
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        PortfolioReportDto report = reportService.generatePortfolioReport(filter, currentUser);
        
        return ResponseEntity.ok(report);
    }

    @GetMapping("/compliance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ComplianceReportDto> getComplianceReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Generating compliance report");
        
        ReportFilterDto filter = ReportFilterDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        ComplianceReportDto report = reportService.generateComplianceReport(filter, currentUser);
        
        return ResponseEntity.ok(report);
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'AUDITOR')")
    public ResponseEntity<AuditReportDto> getAuditReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Generating audit report");
        
        ReportFilterDto filter = ReportFilterDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        AuditReportDto report = reportService.generateAuditReport(filter, currentUser);
        
        return ResponseEntity.ok(report);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam String reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "PDF") String format) {
        
        log.info("Exporting report: {} in format: {}", reportType, format);
        
        ReportFilterDto filter = ReportFilterDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .format(format)
                .build();
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        byte[] reportContent = reportService.exportReport(reportType, filter, format, currentUser);
        
        HttpHeaders headers = new HttpHeaders();
        String contentType = format.equals("PDF") ? MediaType.APPLICATION_PDF_VALUE : 
                            (format.equals("EXCEL") ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : 
                            "text/csv");
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", "report." + format.toLowerCase());
        
        return ResponseEntity.ok().headers(headers).body(reportContent);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<Map<String, Object>> getReportStatistics() {
        log.info("Fetching report statistics");
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        Map<String, Object> stats = reportService.getReportStatistics(currentUser);
        
        return ResponseEntity.ok(stats);
    }





}