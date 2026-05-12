// controller/BankReconciliationController.java
package com.microfinance.financials.reconciliation.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.financials.reconciliation.dto.*;
import com.microfinance.financials.reconciliation.service.BankReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/finance/bank-reconciliation")
@RequiredArgsConstructor
public class BankReconciliationController {

    private final BankReconciliationService reconciliationService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    // Bank Account Endpoints
    @PostMapping("/bank-accounts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<BankAccountDTO> createBankAccount(@Valid @RequestBody BankAccountDTO dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(reconciliationService.createBankAccount(dto, currentUser));
    }

    @PutMapping("/bank-accounts/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<BankAccountDTO> updateBankAccount(@PathVariable Long id, 
                                                             @Valid @RequestBody BankAccountDTO dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(reconciliationService.updateBankAccount(id, dto, currentUser));
    }

    @GetMapping("/bank-accounts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<List<BankAccountDTO>> getBankAccounts() {
        return ResponseEntity.ok(reconciliationService.getBankAccounts());
    }

    @GetMapping("/bank-accounts/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<BankAccountDTO> getBankAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.getBankAccountById(id));
    }

    // Reconciliation Endpoints
    @PostMapping("/reconcile/start")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ReconciliationDTO> startReconciliation(@Valid @RequestBody ReconcileRequestDTO request) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(reconciliationService.startReconciliation(request, currentUser));
    }

    @PostMapping("/reconcile/match")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ReconciliationDTO> matchItems(@Valid @RequestBody MatchItemsRequestDTO request) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(reconciliationService.matchItems(request, currentUser));
    }

    @PostMapping("/reconcile/{id}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ReconciliationDTO> completeReconciliation(@PathVariable Long id) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(reconciliationService.completeReconciliation(id, currentUser));
    }

    @GetMapping("/reconcile/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<ReconciliationDTO> getReconciliationById(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.getReconciliationById(id));
    }

    @GetMapping("/reconcile/history/{bankAccountId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<Page<ReconciliationDTO>> getReconciliationHistory(
            @PathVariable Long bankAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reconciliationDate"));
        return ResponseEntity.ok(reconciliationService.getReconciliationHistory(bankAccountId, pageable));
    }

    @GetMapping("/reconcile/latest/{bankAccountId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<ReconciliationDTO> getLatestReconciliation(@PathVariable Long bankAccountId) {
        ReconciliationDTO reconciliation = reconciliationService.getLatestReconciliation(bankAccountId);
        return reconciliation != null ? ResponseEntity.ok(reconciliation) : ResponseEntity.noContent().build();
    }

    // Bank Statement Endpoints
    @PostMapping("/bank-statements")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<BankStatementDTO> uploadBankStatement(@Valid @RequestBody BankStatementDTO dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(reconciliationService.uploadBankStatement(dto, currentUser));
    }
}