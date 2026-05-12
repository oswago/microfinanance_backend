// controller/ChartOfAccountsController.java
package com.microfinance.financials.chartofaccounts.controller;

import com.microfinance.financials.chartofaccounts.dto.AccountCategoryDto;
import com.microfinance.financials.chartofaccounts.dto.AccountDto;
import com.microfinance.financials.chartofaccounts.service.ChartOfAccountsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/finance/chart-of-accounts")
@RequiredArgsConstructor
public class ChartOfAccountsController {
    
    private final ChartOfAccountsService chartOfAccountsService;
    
    // ==================== Account Categories ====================
    
    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountCategoryDto>> getAllCategories() {
        return ResponseEntity.ok(chartOfAccountsService.getAllCategories());
    }
    
    @GetMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<AccountCategoryDto> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(chartOfAccountsService.getCategoryById(id));
    }
    
    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<AccountCategoryDto> createCategory(@RequestBody AccountCategoryDto dto) {
        return ResponseEntity.ok(chartOfAccountsService.createCategory(dto));
    }
    
    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<AccountCategoryDto> updateCategory(@PathVariable Long id, 
                                                              @RequestBody AccountCategoryDto dto) {
        return ResponseEntity.ok(chartOfAccountsService.updateCategory(id, dto));
    }
    
    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        chartOfAccountsService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }
    
    // ==================== Accounts ====================
    
    @GetMapping("/accounts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        return ResponseEntity.ok(chartOfAccountsService.getAllAccounts());
    }
    
    @GetMapping("/accounts/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(chartOfAccountsService.getAccountById(id));
    }
    
    @GetMapping("/categories/{categoryId}/accounts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountDto>> getAccountsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(chartOfAccountsService.getAccountsByCategory(categoryId));
    }
    
    @PostMapping("/accounts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<AccountDto> createAccount(@RequestBody AccountDto dto) {
        return ResponseEntity.ok(chartOfAccountsService.createAccount(dto));
    }
    
    @PutMapping("/accounts/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<AccountDto> updateAccount(@PathVariable Long id, 
                                                     @RequestBody AccountDto dto) {
        return ResponseEntity.ok(chartOfAccountsService.updateAccount(id, dto));
    }
    
    @DeleteMapping("/accounts/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        chartOfAccountsService.deleteAccount(id);
        return ResponseEntity.ok().build();
    }
}