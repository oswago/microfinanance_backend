package com.microfinance.loanproducts.controller;

import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanproducts.dto.LoanProductCreateRequest;
import com.microfinance.loanproducts.dto.LoanProductDTO;
import com.microfinance.loanproducts.dto.LoanProductUpdateRequest;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.loanproducts.service.LoanProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/loan-products")
@RequiredArgsConstructor
public class LoanProductController {

    private final LoanProductService loanProductService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('LOAN_OFFICER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<LoanProductDTO>> getAllLoanProducts() {
        List<LoanProduct> products = loanProductService.getAllActiveLoanProducts();
        List<LoanProductDTO> productDTOs = products.stream()
                .map(LoanProductDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(productDTOs);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('LOAN_OFFICER')")
    public ResponseEntity<LoanProductDTO> getLoanProductById(@PathVariable Long id) {
        LoanProduct product = loanProductService.getLoanProductById(id);
        return ResponseEntity.ok(LoanProductDTO.fromEntity(product));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<LoanProductDTO> createLoanProduct(@Valid @RequestBody LoanProductCreateRequest createRequest) {
        LoanProduct product = loanProductService.createLoanProduct(createRequest);
        return ResponseEntity.ok(LoanProductDTO.fromEntity(product)); // Convert to DTO
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<LoanProductDTO> updateLoanProduct(@PathVariable Long id, @Valid @RequestBody LoanProductUpdateRequest updateRequest) {
        LoanProduct updatedProduct = loanProductService.updateLoanProduct(id, updateRequest);
        return ResponseEntity.ok(LoanProductDTO.fromEntity(updatedProduct)); // Convert to DTO
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> archiveLoanProduct(@PathVariable Long id) {
        loanProductService.archiveLoanProduct(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('LOAN_OFFICER')")
    public ResponseEntity<List<LoanProductDTO>> getActiveLoanProducts() {
        List<LoanProduct> products = loanProductService.getAllActiveLoanProducts();
        List<LoanProductDTO> productDTOs = products.stream()
                .map(LoanProductDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(productDTOs); // Convert to DTO
    }

    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<List<LoanProductDTO>> getLoanProductsByStatus(@PathVariable GeneralConfig.ProductStatus status) {
        List<LoanProduct> products = loanProductService.getLoanProductsByStatus(status);
        List<LoanProductDTO> productDTOs = products.stream()
                .map(LoanProductDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(productDTOs); // Convert to DTO
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<LoanProductDTO> activateLoanProduct(@PathVariable Long id) {
        LoanProduct product = loanProductService.activateLoanProduct(id);
        return ResponseEntity.ok(LoanProductDTO.fromEntity(product)); // Convert to DTO
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<LoanProductDTO> deactivateLoanProduct(@PathVariable Long id) {
        LoanProduct product = loanProductService.deactivateLoanProduct(id);
        return ResponseEntity.ok(LoanProductDTO.fromEntity(product)); // Convert to DTO
    }

    @GetMapping("/templates")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<List<LoanProductDTO>> getProductTemplates() {
        List<LoanProduct> templates = loanProductService.getProductTemplates();
        List<LoanProductDTO> templateDTOs = templates.stream()
                .map(LoanProductDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(templateDTOs); // Convert to DTO
    }

    @PostMapping("/{id}/save-as-template")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER')")
    @Transactional
    public ResponseEntity<LoanProductDTO> saveAsTemplate(@PathVariable Long id) {
        LoanProduct template = loanProductService.saveAsTemplate(id);
        return ResponseEntity.ok(LoanProductDTO.fromEntity(template)); // Convert to DTO
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('LOAN_OFFICER')")
    public ResponseEntity<List<LoanProductDTO>> searchLoanProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String productTypeCode,
            @RequestParam(required = false)GeneralConfig.InterestMethod interestMethod) {
        List<LoanProduct> products = loanProductService.searchLoanProductsByCode(name, productTypeCode, interestMethod);
        List<LoanProductDTO> productDTOs = products.stream()
                .map(LoanProductDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(productDTOs); // Convert to DTO
    }
}