// controller/TaxController.java
package com.microfinance.financials.taxmanagement.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.financials.taxmanagement.dto.*;
import com.microfinance.financials.taxmanagement.service.TaxService;
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

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/finance/tax")
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    // Tax Configuration Endpoints
    @PostMapping("/configs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<TaxConfigDTO> createTaxConfig(@RequestBody TaxConfigDTO dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(taxService.createTaxConfig(dto, currentUser));
    }

    @PutMapping("/configs/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<TaxConfigDTO> updateTaxConfig(@PathVariable Long id, @RequestBody TaxConfigDTO dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(taxService.updateTaxConfig(id, dto, currentUser));
    }

    @GetMapping("/configs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<List<TaxConfigDTO>> getAllTaxConfigs() {
        return ResponseEntity.ok(taxService.getAllTaxConfigs());
    }

    @GetMapping("/configs/active")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<List<TaxConfigDTO>> getActiveTaxConfigs() {
        return ResponseEntity.ok(taxService.getActiveTaxConfigs());
    }

    @GetMapping("/configs/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<TaxConfigDTO> getTaxConfigById(@PathVariable Long id) {
        return ResponseEntity.ok(taxService.getTaxConfigById(id));
    }

    // Tax Calculation Endpoints
    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<TaxCalculationResultDTO> calculateTax(@RequestBody TaxCalculationRequestDTO request) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(taxService.calculateTax(request, currentUser));
    }

    @PostMapping("/transactions/{id}/withhold")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<TaxTransactionDTO> withholdTax(@PathVariable Long id) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(taxService.withholdTax(id, currentUser));
    }

    @PostMapping("/transactions/{id}/remit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<TaxTransactionDTO> remitTax(@PathVariable Long id, @RequestParam String reference) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(taxService.remitTax(id, reference, currentUser));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<Page<TaxTransactionDTO>> getTaxTransactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));
        return ResponseEntity.ok(taxService.getTaxTransactions(startDate, endDate, pageable));
    }

    // Withholding Tax Certificate Endpoints
    @GetMapping("/certificates/borrower/{borrowerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<List<WithholdingTaxCertificateDTO>> getCertificatesByBorrower(@PathVariable Long borrowerId) {
        return ResponseEntity.ok(taxService.getCertificatesByBorrower(borrowerId));
    }

    @GetMapping("/certificates/{id}/print")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> printCertificate(@PathVariable Long id) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        byte[] pdf = taxService.printCertificate(id, currentUser);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=certificate_" + id + ".pdf")
                .body(pdf);
    }

    // Tax Report Endpoints
    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<TaxReportDTO> generateTaxReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(taxService.generateTaxReport(startDate, endDate, currentUser));
    }
}