package com.microfinance.system.controller;

import com.microfinance.system.dto.BranchRequest;
import com.microfinance.system.dto.BranchResponse;
import com.microfinance.system.entity.*;
import com.microfinance.system.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {
    
    private final SystemService systemService;
    
    // System Settings
    @GetMapping("/settings")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SystemSettings> getSystemSettings() {
        return ResponseEntity.ok(systemService.getSystemSettings());
    }
    
    @PutMapping("/settings")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SystemSettings> updateSystemSettings(@RequestBody SystemSettings settings) {
        return ResponseEntity.ok(systemService.updateSystemSettings(settings));
    }
    
    // Branch Management
    @GetMapping("/branches")
   // @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<List<Branch>> getBranches() {
        return ResponseEntity.ok(systemService.getAllBranches());
    }


    @PostMapping("/branches")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<BranchResponse> createBranch(@RequestBody BranchRequest request) {
        Branch branch = systemService.createBranch(request);
        BranchResponse response = BranchResponse.fromEntity(branch);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/branches/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<BranchResponse> updateBranch(@PathVariable Long id, @RequestBody BranchRequest request) {
        //return ResponseEntity.ok(systemService.updateBranch(id, request));
        Branch branch = systemService.updateBranch(id, request);
        BranchResponse response = BranchResponse.fromEntity(branch);
        return ResponseEntity.ok(response);
    }
    
    // Currency Management
    @GetMapping("/currencies")
    public ResponseEntity<List<CurrencySettings>> getCurrencies() {
        return ResponseEntity.ok(systemService.getAllCurrencies());
    }
    
    @PostMapping("/currencies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CurrencySettings> createCurrency(@RequestBody CurrencySettings currency) {
        return ResponseEntity.ok(systemService.createCurrency(currency));
    }
    
    @PutMapping("/currencies/{code}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CurrencySettings> updateCurrency(@PathVariable String code, @RequestBody CurrencySettings currency) {
        return ResponseEntity.ok(systemService.updateCurrency(code, currency));
    }
    
    // Holiday Management
    @GetMapping("/holidays")
    public ResponseEntity<List<HolidayCalendar>> getHolidays() {
        return ResponseEntity.ok(systemService.getAllHolidays());
    }
    
    @PostMapping("/holidays")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<HolidayCalendar> createHoliday(@RequestBody HolidayCalendar holiday) {
        return ResponseEntity.ok(systemService.createHoliday(holiday));
    }

    @PutMapping("/holidays/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<HolidayCalendar> updateCurrency(@PathVariable Long id, @RequestBody HolidayCalendar holiday) {
        return ResponseEntity.ok(systemService.updateHoliday(id, holiday));
    }
    
    @GetMapping("/holidays/check")
    public ResponseEntity<Boolean> isHoliday(@RequestParam String date) {
        return ResponseEntity.ok(systemService.isHoliday(java.time.LocalDate.parse(date)));
    }
    
    // Number Sequences
    @GetMapping("/number-sequences")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<NumberSequence>> getNumberSequences() {
        return ResponseEntity.ok(systemService.getAllNumberSequences());
    }
    
    @GetMapping("/number-sequences/next/{sequenceCode}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'CASHIER')")
    public ResponseEntity<String> getNextNumber(@PathVariable String sequenceCode) {
        return ResponseEntity.ok(systemService.getNextNumber(sequenceCode));
    }

    @PostMapping("/number-sequences")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','LOAN_OFFICER')")
    public ResponseEntity<NumberSequence> createHoliday(@RequestBody NumberSequence numberSequence) {
        return ResponseEntity.ok(systemService.createNumberSequence(numberSequence));
    }

    @PutMapping("/number-sequences/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','LOAN_OFFICER')")
    public ResponseEntity<NumberSequence> updateCurrency(@PathVariable Long id, @RequestBody NumberSequence numberSequence) {
        return ResponseEntity.ok(systemService.updateNumberSequence(id, numberSequence));
    }
}