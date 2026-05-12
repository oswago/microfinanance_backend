package com.microfinance.borrower.controller;

import com.microfinance.borrower.dto.GuarantorDto;
import com.microfinance.borrower.dto.GuarantorCreateRequest;
import com.microfinance.borrower.service.GuarantorService;
import com.microfinance.common.config.GeneralConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/guarantors")
@RequiredArgsConstructor
public class GuarantorController {

    private final GuarantorService guarantorService;

    @GetMapping("/borrower/{borrowerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<Page<GuarantorDto>> getGuarantorsByBorrower(
            @PathVariable Long borrowerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<GuarantorDto> guarantors = guarantorService.getGuarantorsByBorrower(borrowerId, pageable);
        return ResponseEntity.ok(guarantors);
    }

    @GetMapping("/borrower/{borrowerId}/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<Page<GuarantorDto>> searchGuarantors(
            @PathVariable Long borrowerId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<GuarantorDto> guarantors = guarantorService.searchGuarantors(borrowerId, query, pageable);
        return ResponseEntity.ok(guarantors);
    }

    @GetMapping("/borrower/{borrowerId}/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<Map<String, Object>> getGuarantorsSummary(@PathVariable Long borrowerId) {
        Map<String, Object> summary = guarantorService.getGuarantorsSummary(borrowerId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<GuarantorDto> getGuarantorById(@PathVariable Long id) {
        GuarantorDto guarantor = guarantorService.getGuarantorById(id);
        return ResponseEntity.ok(guarantor);
    }

    @PostMapping("/borrower/{borrowerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<GuarantorDto> createGuarantor(
            @PathVariable Long borrowerId,
            @Valid @RequestBody GuarantorCreateRequest request) {
        
        GuarantorDto guarantor = guarantorService.createGuarantor(borrowerId, request);
        return ResponseEntity.ok(guarantor);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<GuarantorDto> updateGuarantor(
            @PathVariable Long id,
            @Valid @RequestBody GuarantorCreateRequest request) {
        
        GuarantorDto guarantor = guarantorService.updateGuarantor(id, request);
        return ResponseEntity.ok(guarantor);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<Void> deleteGuarantor(@PathVariable Long id) {
        guarantorService.deleteGuarantor(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<GuarantorDto> updateGuarantorStatus(
            @PathVariable Long id,
            @RequestParam GeneralConfig.GuarantorStatus status) {
        
        GuarantorDto guarantor = guarantorService.updateGuarantorStatus(id, status);
        return ResponseEntity.ok(guarantor);
    }
}