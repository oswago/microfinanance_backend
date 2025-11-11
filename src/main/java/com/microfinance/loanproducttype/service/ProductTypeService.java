package com.microfinance.loanproducttype.service;

import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.loanproducts.repository.LoanProductRepository;
import com.microfinance.loanproducttype.dto.ProductTypeCreateRequest;
import com.microfinance.loanproducttype.dto.ProductTypeUpdateRequest;
import com.microfinance.loanproducttype.entity.ProductType;
import com.microfinance.loanproducttype.repository.ProductTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductTypeService {
    
    private final ProductTypeRepository productTypeRepository;
    private final LoanProductRepository loanProductRepository;
    private final SecurityUtils securityUtils; // Inject SecurityUtils

    @Transactional
    public ProductType createProductType(ProductTypeCreateRequest createRequest) {
        if (productTypeRepository.existsByCode(createRequest.getCode())) {
            throw new RuntimeException("Product type code already exists");
        }

        if (productTypeRepository.existsByName(createRequest.getName())) {
            throw new RuntimeException("Product type name already exists");
        }

        ProductType productType = new ProductType();
        productType.setCode(createRequest.getCode());
        productType.setName(createRequest.getName());
        productType.setDescription(createRequest.getDescription());
        productType.setEligibilityCriteria(createRequest.getEligibilityCriteria());
        productType.setIcon(createRequest.getIcon());
        productType.setDisplayOrder(createRequest.getDisplayOrder());
        productType.setActive(true);
        productType.setCreatedAt(LocalDateTime.now());
        productType.setCreatedBy(securityUtils.getCurrentUserId());

        return productTypeRepository.save(productType);
    }

    public List<ProductType> getAllActiveProductTypes() {
        return productTypeRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public ProductType getProductTypeById(Long id) {
        return productTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product type not found with id: " + id));
    }

    public ProductType getProductTypeByCode(String code) {
        return productTypeRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Product type not found with code: " + code));
    }

    @Transactional
    public ProductType updateProductType(Long id, ProductTypeUpdateRequest updateRequest) {
        ProductType productType = getProductTypeById(id);

        if (!productType.getName().equals(updateRequest.getName()) && 
            productTypeRepository.existsByName(updateRequest.getName())) {
            throw new RuntimeException("Product type name already exists");
        }

        productType.setName(updateRequest.getName());
        productType.setDescription(updateRequest.getDescription());
        productType.setEligibilityCriteria(updateRequest.getEligibilityCriteria());
        productType.setIcon(updateRequest.getIcon());
        productType.setDisplayOrder(updateRequest.getDisplayOrder());
        productType.setUpdatedBy(securityUtils.getCurrentUserId());
        productType.setUpdatedAt(LocalDateTime.now());
        
        if (updateRequest.getActive() != null) {
            productType.setActive(updateRequest.getActive());
        }

        return productTypeRepository.save(productType);
    }

    @Transactional
    public void deactivateProductType(Long id) {
        ProductType productType = getProductTypeById(id);
        productType.setActive(false);
        productType.setUpdatedBy(securityUtils.getCurrentUserId());
        productType.setUpdatedAt(LocalDateTime.now());

        productTypeRepository.save(productType);
    }

    @Transactional
    public ProductType activateProductType(Long id) {
        ProductType productType = getProductTypeById(id);
        productType.setActive(true);
        productType.setUpdatedBy(securityUtils.getCurrentUserId());
        productType.setUpdatedAt(LocalDateTime.now());

        return productTypeRepository.save(productType);
    }

    public List<ProductType> searchProductTypes(String searchTerm) {
        return productTypeRepository.searchActiveProductTypes(searchTerm);
    }

    // Initialize default product types
    @Transactional
    public void initializeDefaultProductTypes() {
        createDefaultProductType("PERSONAL", "Personal Loan", "Loans for personal needs", "user", 1);
        createDefaultProductType("BUSINESS", "Business Loan", "Loans for business expansion", "briefcase", 2);
        createDefaultProductType("AGRICULTURAL", "Agricultural Loan", "Loans for farming and agriculture", "tractor", 3);
        createDefaultProductType("EDUCATION", "Education Loan", "Loans for educational purposes", "graduation-cap", 4);
        createDefaultProductType("EMERGENCY", "Emergency Loan", "Quick loans for emergencies", "first-aid", 5);
        createDefaultProductType("MICROENTERPRISE", "Microenterprise Loan", "Small loans for micro businesses", "store", 6);
        createDefaultProductType("GROUP", "Group Loan", "Loans for group borrowing", "users", 7);
        createDefaultProductType("PAYDAY", "Payday Loan", "Short-term salary advance loans", "calendar", 8);
    }

    private void createDefaultProductType(String code, String name, String description, String icon, Integer order) {
        if (!productTypeRepository.existsByCode(code)) {
            ProductType productType = new ProductType();
            productType.setCode(code);
            productType.setName(name);
            productType.setDescription(description);
            productType.setIcon(icon);
            productType.setDisplayOrder(order);
            productType.setActive(true);
            productType.setCreatedAt(LocalDateTime.now());
            productTypeRepository.save(productType);
        }
    }

    public List<LoanProduct> getLoanProductsByProductTypeId(Long productTypeId) {
        // First verify the product type exists
        ProductType productType = getProductTypeById(productTypeId);
        // Then fetch the loan products
        return loanProductRepository.findByProductTypeIdAndActiveTrue(productTypeId);
    }


}