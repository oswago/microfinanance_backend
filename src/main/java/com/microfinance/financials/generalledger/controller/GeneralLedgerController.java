// controller/GeneralLedgerController.java
package com.microfinance.financials.generalledger.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.financials.generalledger.dto.FinancialPeriodDto;
import com.microfinance.financials.generalledger.dto.GeneralLedgerDto;
import com.microfinance.financials.generalledger.dto.JournalEntryDto;
import com.microfinance.financials.generalledger.dto.TrialBalanceDto;
import com.microfinance.financials.generalledger.service.GeneralLedgerService;
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
@RequestMapping("/finance/general-ledger")
@RequiredArgsConstructor
public class GeneralLedgerController {

    private final GeneralLedgerService generalLedgerService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    // Journal Entry endpoints
    @PostMapping("/journal-entries")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<JournalEntryDto> createJournalEntry(@RequestBody JournalEntryDto dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(generalLedgerService.createJournalEntry(dto, currentUser));
    }

    @PostMapping("/journal-entries/{id}/post")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<JournalEntryDto> postJournalEntry(@PathVariable Long id) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(generalLedgerService.postJournalEntry(id, currentUser));
    }

    @PostMapping("/journal-entries/{id}/reverse")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<JournalEntryDto> reverseJournalEntry(@PathVariable Long id, @RequestBody String reason) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(generalLedgerService.reverseJournalEntry(id, reason, currentUser));
    }

    @GetMapping("/journal-entries")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<Page<JournalEntryDto>> getJournalEntries(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "entryDate"));
        return ResponseEntity.ok(generalLedgerService.getJournalEntries(status, startDate, endDate, pageable, currentUser));
    }

    @GetMapping("/journal-entries/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<JournalEntryDto> getJournalEntryById(@PathVariable Long id) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(generalLedgerService.getJournalEntryById(id, currentUser));
    }

    // General Ledger endpoints
    @GetMapping("/ledger-entries")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<Page<GeneralLedgerDto>> getLedgerEntries(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));
        return ResponseEntity.ok(generalLedgerService.getLedgerEntries(accountId, startDate, endDate, pageable, currentUser));
    }

    @GetMapping("/trial-balance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<List<TrialBalanceDto>> getTrialBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(generalLedgerService.getTrialBalance(asOfDate, currentUser));
    }

    // Financial Period endpoints
    @PostMapping("/periods")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<FinancialPeriodDto> createFinancialPeriod(@RequestBody FinancialPeriodDto dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(generalLedgerService.createFinancialPeriod(dto, currentUser));
    }

    @PostMapping("/periods/{id}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<FinancialPeriodDto> closeFinancialPeriod(@PathVariable Long id) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(generalLedgerService.closeFinancialPeriod(id, currentUser));
    }

    @GetMapping("/periods")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<List<FinancialPeriodDto>> getFinancialPeriods(
            @RequestParam(required = false) Integer year) {
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(generalLedgerService.getFinancialPeriods(year, currentUser));
    }

    @GetMapping("/periods/current")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<FinancialPeriodDto> getCurrentFinancialPeriod() {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(generalLedgerService.getCurrentFinancialPeriod(currentUser));
    }
}