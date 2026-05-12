// controller/BudgetController.java
package com.microfinance.financials.budget.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.financials.budget.dto.*;
import com.microfinance.financials.budget.service.BudgetService;
import com.microfinance.financials.budget.service.ForecastService;
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
@RequestMapping("/finance/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final ForecastService forecastService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    // Budget Endpoints
    @PostMapping("/budgets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<BudgetDTO> createBudget(@RequestBody BudgetDTO dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(budgetService.createBudget(dto, currentUser));
    }

    @PutMapping("/budgets/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<BudgetDTO> updateBudget(@PathVariable Long id, @RequestBody BudgetDTO dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(budgetService.updateBudget(id, dto, currentUser));
    }

    @GetMapping("/budgets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<Page<BudgetDTO>> getBudgets(
            @RequestParam(required = false) Integer fiscalYear,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fiscalYear"));
        return ResponseEntity.ok(budgetService.getBudgets(fiscalYear, category, pageable));
    }

    @GetMapping("/budgets/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<BudgetDTO> getBudgetById(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.getBudgetById(id));
    }

    @GetMapping("/budgets/years")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<List<Integer>> getAvailableYears() {
        return ResponseEntity.ok(budgetService.getAvailableYears());
    }

    // Budget vs Actual Report
    @GetMapping("/reports/budget-vs-actual")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<BudgetVsActualReportDTO> getBudgetVsActualReport(
            @RequestParam Integer fiscalYear,
            @RequestParam String category) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        // No Pageable parameter needed for reports
        return ResponseEntity.ok(budgetService.generateBudgetVsActualReport(fiscalYear, category, currentUser));
    }

    // Forecasting Endpoints
    @PostMapping("/forecasts/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ForecastResultDTO> generateForecast(@RequestBody ForecastRequestDTO request) {
        return ResponseEntity.ok(forecastService.generateForecast(request));
    }

    @PostMapping("/forecasts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ForecastDTO> createForecast(@RequestBody ForecastDTO dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(forecastService.createForecast(dto, currentUser));
    }

    @GetMapping("/forecasts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<List<ForecastDTO>> getForecasts(@RequestParam String category) {
        return ResponseEntity.ok(forecastService.getForecastsByCategory(category));
    }
}