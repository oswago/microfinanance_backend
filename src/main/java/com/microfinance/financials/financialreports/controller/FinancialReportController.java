// controller/FinancialReportController.java
package com.microfinance.financials.financialreports.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.financials.financialreports.dto.*;
import com.microfinance.financials.financialreports.service.FinancialReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/finance/financial-reports")
@RequiredArgsConstructor
/**
 * @Tag(name = "Financial Reports", description = "Endpoints for financial reporting")
 */
public class FinancialReportController {

    private final FinancialReportService reportService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    @PostMapping("/income-statement")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    /**
     * @Operation(summary = "Generate Income Statement")
     */
    public ResponseEntity<IncomeStatementDTO> generateIncomeStatement(@RequestBody IncomeStatementRequest request) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        IncomeStatementDTO report = reportService.generateIncomeStatement(request, currentUser);
        return ResponseEntity.ok(report);
    }


    @GetMapping("/income-statement")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    /**
     * @Operation(summary = "Generate Income Statement with query params")
     */
    public ResponseEntity<IncomeStatementDTO> generateIncomeStatement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long financialPeriodId,
            @RequestParam(defaultValue = "false") Boolean includeDetails) {
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        IncomeStatementRequest request = IncomeStatementRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .financialPeriodId(financialPeriodId)
                .includeDetails(includeDetails)
                .build();
        return ResponseEntity.ok(reportService.generateIncomeStatement(request, currentUser));
    }


    @PostMapping("/balance-sheet")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    /**
     *    @Operation(summary = "Generate Balance Sheet")
     */
    public ResponseEntity<BalanceSheetDTO> generateBalanceSheet(@RequestBody BalanceSheetRequest request) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        BalanceSheetDTO report = reportService.generateBalanceSheet(request, currentUser);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/balance-sheet")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    //@Operation(summary = "Generate Balance Sheet with query params")
    public ResponseEntity<BalanceSheetDTO> generateBalanceSheet(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) Long financialPeriodId,
            @RequestParam(defaultValue = "false") Boolean showComparative) {
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        BalanceSheetRequest request = BalanceSheetRequest.builder()
                .asOfDate(asOfDate)
                .financialPeriodId(financialPeriodId)
                .showComparative(showComparative)
                .build();
        return ResponseEntity.ok(reportService.generateBalanceSheet(request, currentUser));
    }

    @PostMapping("/cash-flow")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
   // @Operation(summary = "Generate Cash Flow Statement")
    public ResponseEntity<CashFlowStatementDTO> generateCashFlowStatement(@RequestBody CashFlowRequest request) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        CashFlowStatementDTO report = reportService.generateCashFlowStatement(request, currentUser);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/cash-flow")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
   // @Operation(summary = "Generate Cash Flow Statement with query params")
    public ResponseEntity<CashFlowStatementDTO> generateCashFlowStatement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "INDIRECT") String method) {
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        CashFlowRequest request = CashFlowRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .method(method)
                .build();
        return ResponseEntity.ok(reportService.generateCashFlowStatement(request, currentUser));
    }

    @PostMapping("/general-ledger")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    /**
     * @Operation(summary = "Generate General Ledger Report")
      */
    public ResponseEntity<GeneralLedgerReportDTO> generateGeneralLedgerReport(@RequestBody GeneralLedgerRequest request) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        GeneralLedgerReportDTO report = reportService.generateGeneralLedgerReport(request, currentUser);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/general-ledger")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    /**
      @Operation(summary = "Generate General Ledger Report with query params")
     */
    public ResponseEntity<GeneralLedgerReportDTO> generateGeneralLedgerReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String accountCode,
            @RequestParam(required = false) String journalNumber) {
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        GeneralLedgerRequest request = GeneralLedgerRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .accountId(accountId)
                .accountCode(accountCode)
                .journalNumber(journalNumber)
                .build();
        return ResponseEntity.ok(reportService.generateGeneralLedgerReport(request, currentUser));
    }

    @GetMapping("/trial-balance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    /**
     * @Operation(summary = "Generate Trial Balance Report")
     */
    public ResponseEntity<TrialBalanceDTO> generateTrialBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) Long financialPeriodId) {
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        TrialBalanceRequest request = TrialBalanceRequest.builder()
                .asOfDate(asOfDate)
                .financialPeriodId(financialPeriodId)
                .build();
        return ResponseEntity.ok(reportService.generateTrialBalance(request, currentUser));
    }

    @GetMapping("/export-pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    /**
     * @Operation(summary = "Export report as PDF")
     */
    public ResponseEntity<byte[]> exportReportAsPdf(
            @RequestParam String reportType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        byte[] pdfContent = reportService.exportReportAsPdf(reportType, startDate, endDate, currentUser);
        
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=" + reportType + "_" + startDate + "_to_" + endDate + ".pdf")
                .body(pdfContent);
    }

    @GetMapping("/export-excel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    /**
     * @Operation(summary = "Export report as Excel")
     */
    public ResponseEntity<byte[]> exportReportAsExcel(
            @RequestParam String reportType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        byte[] excelContent = reportService.exportReportAsExcel(reportType, startDate, endDate, currentUser);
        
        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=" + reportType + "_" + startDate + "_to_" + endDate + ".xlsx")
                .body(excelContent);
    }
}