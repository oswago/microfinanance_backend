// controller/ProvisionController.java
package com.microfinance.financials.provisions.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.financials.provisions.dto.*;
import com.microfinance.financials.provisions.service.ProvisionService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/finance/provision")
@RequiredArgsConstructor
public class ProvisionController {

    private final ProvisionService provisionService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    // Provision Calculation Endpoints
    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<List<ProvisionCalculationDTO>> calculateProvisions(@RequestBody ProvisionCalculationRequestDTO request) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(provisionService.calculateProvisions(request, currentUser));
    }

    @GetMapping("/calculations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<Page<ProvisionCalculationDTO>> getProvisionCalculations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "calculationDate"));
        return ResponseEntity.ok(provisionService.getProvisionCalculations(startDate, endDate, pageable));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<List<ProvisionSummaryDTO>> getProvisionSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate calculationDate) {
        return ResponseEntity.ok(provisionService.getProvisionSummary(calculationDate));
    }

    // Write-off Request Endpoints
    @PostMapping("/write-offs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<WriteOffRequestDTO> createWriteOffRequest(@RequestBody WriteOffRequestDTO dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(provisionService.createWriteOffRequest(dto, currentUser));
    }

    @PostMapping("/write-offs/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<WriteOffRequestDTO> approveWriteOffRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String approvalNotes) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(provisionService.approveWriteOffRequest(id, approvalNotes, currentUser));
    }

    @PostMapping("/write-offs/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<WriteOffRequestDTO> rejectWriteOffRequest(
            @PathVariable Long id,
            @RequestParam String rejectionReason) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(provisionService.rejectWriteOffRequest(id, rejectionReason, currentUser));
    }

    @PostMapping("/write-offs/{id}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<WriteOffRequestDTO> completeWriteOff(@PathVariable Long id) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(provisionService.completeWriteOff(id, currentUser));
    }

    @GetMapping("/write-offs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'MANAGER')")
    public ResponseEntity<Page<WriteOffRequestDTO>> getWriteOffRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestDate"));
        return ResponseEntity.ok(provisionService.getWriteOffRequests(status, pageable));
    }

    // Loan Recovery Endpoints
    @PostMapping("/recoveries")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<LoanRecoveryDTO> recordRecovery(@RequestBody LoanRecoveryDTO dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(provisionService.recordRecovery(dto, currentUser));
    }

    @GetMapping("/recoveries")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<Page<LoanRecoveryDTO>> getRecoveries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recoveryDate"));
        return ResponseEntity.ok(provisionService.getRecoveries(startDate, endDate, pageable));
    }

    @GetMapping("/recoveries/total")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT', 'AUDITOR')")
    public ResponseEntity<BigDecimal> getTotalRecoveries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(provisionService.getTotalRecoveries(startDate, endDate));
    }
}