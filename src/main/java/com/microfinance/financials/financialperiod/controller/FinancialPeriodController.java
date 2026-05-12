// controller/FinancialPeriodController.java
package com.microfinance.financials.financialperiod.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.financials.financialperiod.dto.ClosePeriodDto;
import com.microfinance.financials.financialperiod.service.FinancialPeriodService;
import com.microfinance.financials.generalledger.dto.FinancialPeriodDto;
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
@RequestMapping("/finance/financial-periods")
@RequiredArgsConstructor
public class FinancialPeriodController {

    private final FinancialPeriodService financialPeriodService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<FinancialPeriodDto> createFinancialPeriod(@RequestBody FinancialPeriodDto dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(financialPeriodService.createFinancialPeriod(dto, currentUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<FinancialPeriodDto> updateFinancialPeriod(@PathVariable Long id, 
                                                                     @RequestBody FinancialPeriodDto dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(financialPeriodService.updateFinancialPeriod(id, dto, currentUser));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<FinancialPeriodDto> closeFinancialPeriod(@PathVariable Long id,
                                                                    @RequestBody(required = false) ClosePeriodDto request) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        if (request == null) {
            request = ClosePeriodDto.builder().build();
        }
        return ResponseEntity.ok(financialPeriodService.closeFinancialPeriod(id, request, currentUser));
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<FinancialPeriodDto> lockFinancialPeriod(@PathVariable Long id,
                                                                    @RequestBody(required = false) String reason) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(financialPeriodService.lockFinancialPeriod(id, reason, currentUser));
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<FinancialPeriodDto> unlockFinancialPeriod(@PathVariable Long id,
                                                                      @RequestBody(required = false) String reason) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(financialPeriodService.unlockFinancialPeriod(id, reason, currentUser));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<FinancialPeriodDto> reopenFinancialPeriod(@PathVariable Long id,
                                                                     @RequestBody(required = false) String reason) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(financialPeriodService.reopenFinancialPeriod(id, reason, currentUser));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<Page<FinancialPeriodDto>> getFinancialPeriods(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "year", "month"));
        return ResponseEntity.ok(financialPeriodService.getFinancialPeriods(year, status, pageable, currentUser));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<FinancialPeriodDto> getFinancialPeriodById(@PathVariable Long id) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(financialPeriodService.getFinancialPeriodById(id, currentUser));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<FinancialPeriodDto> getCurrentFinancialPeriod() {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(financialPeriodService.getCurrentFinancialPeriod(currentUser));
    }

    @GetMapping("/{id}/next")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<FinancialPeriodDto> getNextFinancialPeriod(@PathVariable Long id) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        FinancialPeriodDto next = financialPeriodService.getNextFinancialPeriod(id, currentUser);
        return next != null ? ResponseEntity.ok(next) : ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/previous")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<FinancialPeriodDto> getPreviousFinancialPeriod(@PathVariable Long id) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        FinancialPeriodDto prev = financialPeriodService.getPreviousFinancialPeriod(id, currentUser);
        return prev != null ? ResponseEntity.ok(prev) : ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<List<FinancialPeriodDto>> getPeriodSummary(@RequestParam Integer year) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(financialPeriodService.getPeriodSummary(year, currentUser));
    }

    @PostMapping("/auto-create")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<Void> autoCreatePeriods(@RequestParam Integer year) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        financialPeriodService.autoCreatePeriods(year, currentUser);
        return ResponseEntity.ok().build();
    }
}