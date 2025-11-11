package com.microfinance.loanproducttype.controller;

import com.microfinance.loanproducts.dto.LoanProductDTO;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.loanproducttype.dto.ProductTypeCreateRequest;
import com.microfinance.loanproducttype.dto.ProductTypeUpdateRequest;
import com.microfinance.loanproducttype.entity.ProductType;
import com.microfinance.loanproducttype.service.ProductTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/product-types")
@RequiredArgsConstructor
public class ProductTypeController {
    
    private final ProductTypeService productTypeService;

    @GetMapping
    public ResponseEntity<List<ProductType>> getAllProductTypes() {
        List<ProductType> productTypes = productTypeService.getAllActiveProductTypes();
        return ResponseEntity.ok(productTypes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductType> getProductTypeById(@PathVariable Long id) {
        ProductType productType = productTypeService.getProductTypeById(id);
        return ResponseEntity.ok(productType);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ProductType> getProductTypeByCode(@PathVariable String code) {
        ProductType productType = productTypeService.getProductTypeByCode(code);
        return ResponseEntity.ok(productType);
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ProductType> createProductType(@Valid @RequestBody ProductTypeCreateRequest createRequest) {
        ProductType productType = productTypeService.createProductType(createRequest);
        return ResponseEntity.ok(productType);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ProductType> updateProductType(@PathVariable Long id, @Valid @RequestBody ProductTypeUpdateRequest updateRequest) {
        ProductType updatedProductType = productTypeService.updateProductType(id, updateRequest);
        return ResponseEntity.ok(updatedProductType);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ProductType> activateProductType(@PathVariable Long id) {
        ProductType productType = productTypeService.activateProductType(id);
        return ResponseEntity.ok(productType);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ProductType> deactivateProductType(@PathVariable Long id) {
        productTypeService.deactivateProductType(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductType>> searchProductTypes(@RequestParam String searchTerm) {
        List<ProductType> productTypes = productTypeService.searchProductTypes(searchTerm);
        return ResponseEntity.ok(productTypes);
    }

    @PostMapping("/initialize-defaults")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> initializeDefaultProductTypes() {
        productTypeService.initializeDefaultProductTypes();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/loan-products")
    public ResponseEntity<List<LoanProductDTO>> getLoanProductsByProductType(@PathVariable Long id) {
        List<LoanProduct> loanProducts = productTypeService.getLoanProductsByProductTypeId(id);
        List<LoanProductDTO> loanProductDTOs = loanProducts.stream()
                .map(LoanProductDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(loanProductDTOs);
    }
}