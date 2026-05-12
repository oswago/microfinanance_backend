package com.microfinance.loanproducts.service;

import com.microfinance.base.security.UserPrincipal;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanproducts.dto.LoanProductCreateRequest;
import com.microfinance.loanproducts.dto.LoanProductUpdateRequest;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.loanproducts.repository.LoanProductRepository;
import com.microfinance.loanproducttype.entity.ProductType;
import com.microfinance.loanproducttype.service.ProductTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanProductService {

    private final LoanProductRepository loanProductRepository;
    private final ProductTypeService productTypeService;
    private final SecurityUtils securityUtils; // Inject SecurityUtils

    @Transactional
    public LoanProduct createLoanProduct(LoanProductCreateRequest createRequest) {
        // Check if product code already exists
        if (loanProductRepository.existsByProductCode(createRequest.getProductCode())) {
            throw new RuntimeException("Product code already exists");
        }

        // Check if product name already exists
        if (loanProductRepository.existsByName(createRequest.getName())) {
            throw new RuntimeException("Product name already exists");
        }

        // ✅ CORRECT: Fetch the ProductType entity using the ID from request
        ProductType productType = productTypeService.getProductTypeById(createRequest.getProductTypeId());

        LoanProduct product = new LoanProduct();
        product.setProductCode(createRequest.getProductCode());
        product.setName(createRequest.getName());
        product.setDescription(createRequest.getDescription());
        product.setProductType(productType);
        product.setInterestMethod(createRequest.getInterestMethod());
        product.setInterestRate(createRequest.getInterestRate());
        product.setMinLoanAmount(createRequest.getMinLoanAmount());
        product.setMaxLoanAmount(createRequest.getMaxLoanAmount());
        product.setMinTenure(createRequest.getMinTenure());
        product.setMaxTenure(createRequest.getMaxTenure());
        product.setTenureUnit(createRequest.getTenureUnit());
        product.setGracePeriod(createRequest.getGracePeriod() != null ? createRequest.getGracePeriod() : 0);
        product.setProcessingFeeRate(createRequest.getProcessingFeeRate() != null ? createRequest.getProcessingFeeRate() : BigDecimal.ZERO);
        product.setLatePaymentFee(createRequest.getLatePaymentFee() != null ? createRequest.getLatePaymentFee() : BigDecimal.ZERO);
        product.setPrepaymentPenaltyRate(createRequest.getPrepaymentPenaltyRate() != null ? createRequest.getPrepaymentPenaltyRate() : BigDecimal.ZERO);
        product.setInsuranceRequired(createRequest.getInsuranceRequired() != null ? createRequest.getInsuranceRequired() : false);
        product.setCollateralRequired(createRequest.getCollateralRequired() != null ? createRequest.getCollateralRequired() : false);
        product.setMinCreditScore(createRequest.getMinCreditScore());
        product.setEligibilityCriteria(createRequest.getEligibilityCriteria());
        product.setRequiredDocuments(createRequest.getRequiredDocuments());
        product.setStatus(GeneralConfig.ProductStatus.ACTIVE);
        product.setVersion(1);
        product.setIsTemplate(false);
        product.setCreatedAt(LocalDateTime.now());
        product.setActive(true);
        product.setCreatedBy(getCurrentUserId());

        return loanProductRepository.save(product);
    }

    public List<LoanProduct> getAllActiveLoanProducts() {
        return loanProductRepository.findByActiveTrueAndStatus(GeneralConfig.ProductStatus.ACTIVE);
    }

    public LoanProduct getLoanProductById(Long id) {
        return loanProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan product not found with id: " + id));
    }

    @Transactional
    public LoanProduct updateLoanProduct(Long id, LoanProductUpdateRequest updateRequest) {
        LoanProduct existingProduct = getLoanProductById(id);

        // For versioning, create a new version if product is already used in loans
        if (isProductUsedInLoans(id)) {
            return createNewVersion(existingProduct, updateRequest);
        }

        // Update existing product
        if (updateRequest.getName() != null &&
                !existingProduct.getName().equals(updateRequest.getName()) &&
                loanProductRepository.existsByName(updateRequest.getName())) {
            throw new RuntimeException("Product name already exists");
        }

        updateProductFields(existingProduct, updateRequest);
        existingProduct.setVersion(existingProduct.getVersion() + 1);
        existingProduct.setUpdatedAt(LocalDateTime.now());
        existingProduct.setUpdatedBy(getCurrentUserId());

        return loanProductRepository.save(existingProduct);
    }

    private boolean isProductUsedInLoans(Long productId) {
        // This would check if any loans are using this product version
        // Implementation depends on Loan entity relationship
        return false; // Placeholder - implement based on your business logic
    }

    private LoanProduct createNewVersion(LoanProduct existingProduct, LoanProductUpdateRequest updateRequest) {
        // ✅ CORRECT: Fetch the ProductType entity using the ID from request
        ProductType productType = null;
        if (updateRequest.getProductTypeId() != null) {
            productType = productTypeService.getProductTypeById(updateRequest.getProductTypeId());
        } else {
            productType = existingProduct.getProductType();
        }

        LoanProduct newVersion = new LoanProduct();
        newVersion.setProductCode(existingProduct.getProductCode());
        newVersion.setName(updateRequest.getName() != null ? updateRequest.getName() : existingProduct.getName());
        newVersion.setDescription(updateRequest.getDescription() != null ? updateRequest.getDescription() : existingProduct.getDescription());
        newVersion.setProductType(productType);
        newVersion.setInterestMethod(updateRequest.getInterestMethod() != null ? updateRequest.getInterestMethod() : existingProduct.getInterestMethod());
        newVersion.setInterestRate(updateRequest.getInterestRate() != null ? updateRequest.getInterestRate() : existingProduct.getInterestRate());
        newVersion.setMinLoanAmount(updateRequest.getMinLoanAmount() != null ? updateRequest.getMinLoanAmount() : existingProduct.getMinLoanAmount());
        newVersion.setMaxLoanAmount(updateRequest.getMaxLoanAmount() != null ? updateRequest.getMaxLoanAmount() : existingProduct.getMaxLoanAmount());
        newVersion.setMinTenure(updateRequest.getMinTenure() != null ? updateRequest.getMinTenure() : existingProduct.getMinTenure());
        newVersion.setMaxTenure(updateRequest.getMaxTenure() != null ? updateRequest.getMaxTenure() : existingProduct.getMaxTenure());
        newVersion.setTenureUnit(updateRequest.getTenureUnit() != null ? updateRequest.getTenureUnit() : existingProduct.getTenureUnit());
        newVersion.setGracePeriod(updateRequest.getGracePeriod() != null ? updateRequest.getGracePeriod() : existingProduct.getGracePeriod());
        newVersion.setProcessingFeeRate(updateRequest.getProcessingFeeRate() != null ? updateRequest.getProcessingFeeRate() : existingProduct.getProcessingFeeRate());
        newVersion.setLatePaymentFee(updateRequest.getLatePaymentFee() != null ? updateRequest.getLatePaymentFee() : existingProduct.getLatePaymentFee());
        newVersion.setPrepaymentPenaltyRate(updateRequest.getPrepaymentPenaltyRate() != null ? updateRequest.getPrepaymentPenaltyRate() : existingProduct.getPrepaymentPenaltyRate());
        newVersion.setInsuranceRequired(updateRequest.getInsuranceRequired() != null ? updateRequest.getInsuranceRequired() : existingProduct.getInsuranceRequired());
        newVersion.setCollateralRequired(updateRequest.getCollateralRequired() != null ? updateRequest.getCollateralRequired() : existingProduct.getCollateralRequired());
        newVersion.setMinCreditScore(updateRequest.getMinCreditScore() != null ? updateRequest.getMinCreditScore() : existingProduct.getMinCreditScore());
        newVersion.setEligibilityCriteria(updateRequest.getEligibilityCriteria() != null ? updateRequest.getEligibilityCriteria() : existingProduct.getEligibilityCriteria());
        newVersion.setRequiredDocuments(updateRequest.getRequiredDocuments() != null ? updateRequest.getRequiredDocuments() : existingProduct.getRequiredDocuments());
        newVersion.setStatus(GeneralConfig.ProductStatus.ACTIVE);
        newVersion.setVersion(existingProduct.getVersion() + 1);
        newVersion.setPreviousVersionId(existingProduct.getId());
        newVersion.setIsTemplate(false);
        newVersion.setCreatedAt(LocalDateTime.now());
        newVersion.setActive(true);
        newVersion.setCreatedBy(getCurrentUserId());

        // Archive the old version
        existingProduct.setStatus(GeneralConfig.ProductStatus.ARCHIVED);
        existingProduct.setUpdatedAt(LocalDateTime.now());
        existingProduct.setUpdatedBy(getCurrentUserId());
        loanProductRepository.save(existingProduct);

        return loanProductRepository.save(newVersion);
    }

    private void updateProductFields(LoanProduct product, LoanProductUpdateRequest updateRequest) {
        if (updateRequest.getName() != null) {
            product.setName(updateRequest.getName());
        }
        if (updateRequest.getDescription() != null) {
            product.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getProductTypeId() != null) {
            ProductType productType = productTypeService.getProductTypeById(updateRequest.getProductTypeId());
            product.setProductType(productType);
        }
        if (updateRequest.getInterestMethod() != null) {
            product.setInterestMethod(updateRequest.getInterestMethod());
        }
        if (updateRequest.getInterestRate() != null) {
            product.setInterestRate(updateRequest.getInterestRate());
        }
        if (updateRequest.getMinLoanAmount() != null) {
            product.setMinLoanAmount(updateRequest.getMinLoanAmount());
        }
        if (updateRequest.getMaxLoanAmount() != null) {
            product.setMaxLoanAmount(updateRequest.getMaxLoanAmount());
        }
        if (updateRequest.getMinTenure() != null) {
            product.setMinTenure(updateRequest.getMinTenure());
        }
        if (updateRequest.getMaxTenure() != null) {
            product.setMaxTenure(updateRequest.getMaxTenure());
        }
        if (updateRequest.getTenureUnit() != null) {
            product.setTenureUnit(updateRequest.getTenureUnit());
        }
        if (updateRequest.getGracePeriod() != null) {
            product.setGracePeriod(updateRequest.getGracePeriod());
        }
        if (updateRequest.getProcessingFeeRate() != null) {
            product.setProcessingFeeRate(updateRequest.getProcessingFeeRate());
        }
        if (updateRequest.getLatePaymentFee() != null) {
            product.setLatePaymentFee(updateRequest.getLatePaymentFee());
        }
        if (updateRequest.getPrepaymentPenaltyRate() != null) {
            product.setPrepaymentPenaltyRate(updateRequest.getPrepaymentPenaltyRate());
        }
        if (updateRequest.getInsuranceRequired() != null) {
            product.setInsuranceRequired(updateRequest.getInsuranceRequired());
        }
        if (updateRequest.getCollateralRequired() != null) {
            product.setCollateralRequired(updateRequest.getCollateralRequired());
        }
        if (updateRequest.getMinCreditScore() != null) {
            product.setMinCreditScore(updateRequest.getMinCreditScore());
        }
        if (updateRequest.getEligibilityCriteria() != null) {
            product.setEligibilityCriteria(updateRequest.getEligibilityCriteria());
        }
        if (updateRequest.getRequiredDocuments() != null) {
            product.setRequiredDocuments(updateRequest.getRequiredDocuments());
        }
        if (updateRequest.getStatus() != null) {
            product.setStatus(updateRequest.getStatus());
        }
        if (updateRequest.getActive() != null) {
            product.setActive(updateRequest.getActive());
        }
    }

    @Transactional
    public void archiveLoanProduct(Long id) {
        LoanProduct product = getLoanProductById(id);
        product.setStatus(GeneralConfig.ProductStatus.ARCHIVED);
        product.setActive(false);
        product.setUpdatedAt(LocalDateTime.now());
        product.setUpdatedBy(getCurrentUserId());
        loanProductRepository.save(product);
    }

    @Transactional
    public LoanProduct activateLoanProduct(Long id) {
        LoanProduct product = getLoanProductById(id);
        product.setStatus(GeneralConfig.ProductStatus.ACTIVE);
        product.setActive(true);
        product.setUpdatedAt(LocalDateTime.now());
        product.setUpdatedBy(getCurrentUserId());
        return loanProductRepository.save(product);
    }

    @Transactional
    public LoanProduct deactivateLoanProduct(Long id) {
        LoanProduct product = getLoanProductById(id);
        product.setStatus(GeneralConfig.ProductStatus.INACTIVE);
        product.setActive(false);
        product.setUpdatedAt(LocalDateTime.now());
        product.setUpdatedBy(getCurrentUserId());
        return loanProductRepository.save(product);
    }

    public List<LoanProduct> getLoanProductsByStatus(GeneralConfig.ProductStatus status) {
        return loanProductRepository.findByStatus(status);
    }

    public List<LoanProduct> getProductTemplates() {
        return loanProductRepository.findByIsTemplateTrueAndActiveTrue();
    }

    @Transactional
    public LoanProduct saveAsTemplate(Long id) {
        LoanProduct original = getLoanProductById(id);

        LoanProduct template = new LoanProduct();
        template.setProductCode("TEMPLATE_" + original.getProductCode());
        template.setName(original.getName() + " (Template)");
        template.setDescription(original.getDescription());
        template.setProductType(original.getProductType());
        template.setInterestMethod(original.getInterestMethod());
        template.setInterestRate(original.getInterestRate());
        template.setMinLoanAmount(original.getMinLoanAmount());
        template.setMaxLoanAmount(original.getMaxLoanAmount());
        template.setMinTenure(original.getMinTenure());
        template.setMaxTenure(original.getMaxTenure());
        template.setTenureUnit(original.getTenureUnit());
        template.setGracePeriod(original.getGracePeriod());
        template.setProcessingFeeRate(original.getProcessingFeeRate());
        template.setLatePaymentFee(original.getLatePaymentFee());
        template.setPrepaymentPenaltyRate(original.getPrepaymentPenaltyRate());
        template.setInsuranceRequired(original.getInsuranceRequired());
        template.setCollateralRequired(original.getCollateralRequired());
        template.setMinCreditScore(original.getMinCreditScore());
        template.setEligibilityCriteria(original.getEligibilityCriteria());
        template.setRequiredDocuments(original.getRequiredDocuments());
        template.setStatus(GeneralConfig.ProductStatus.ACTIVE);
        template.setVersion(1);
        template.setIsTemplate(true);
        template.setCreatedAt(LocalDateTime.now());
        template.setActive(true);
        template.setCreatedBy(getCurrentUserId());

        return loanProductRepository.save(template);
    }

    public List<LoanProduct> searchLoanProducts(String name, Long productTypeId, GeneralConfig.InterestMethod interestMethod) {
        // ✅ Validate productTypeId exists if provided
        if (productTypeId != null) {
            productTypeService.getProductTypeById(productTypeId);
        }

        return loanProductRepository.searchLoanProducts(name, productTypeId, interestMethod);
    }

    public BigDecimal calculateInterest(LoanProduct product, BigDecimal principal, Integer tenure) {
        switch (product.getInterestMethod()) {
            case FLAT:
                return principal.multiply(product.getInterestRate())
                        .multiply(BigDecimal.valueOf(tenure))
                        .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            case REDUCING_BALANCE:
                // Simplified reducing balance calculation
                BigDecimal monthlyRate = product.getInterestRate().divide(BigDecimal.valueOf(1200), 6, BigDecimal.ROUND_HALF_UP);
                BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(tenure);
                return principal.multiply(monthlyRate)
                        .multiply(factor)
                        .divide(factor.subtract(BigDecimal.ONE), 2, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(tenure))
                        .subtract(principal);
            default:
                throw new IllegalArgumentException("Unsupported interest method: " + product.getInterestMethod());
        }
    }

    public List<LoanProduct> searchLoanProductsByCode(String name, String productTypeCode, GeneralConfig.InterestMethod interestMethod) {
        Long productTypeId = null;
        if (productTypeCode != null) {
            ProductType productType = productTypeService.getProductTypeByCode(productTypeCode);
            productTypeId = productType.getId();
        }
        return searchLoanProducts(name, productTypeId, interestMethod);
    }

        private Long getCurrentUserId() {
            UserPrincipal currentUser = securityUtils.getCurrentUserPrincipal();
            Long id =securityUtils.getCurrentUserId();
            return currentUser.getId();
        }


}