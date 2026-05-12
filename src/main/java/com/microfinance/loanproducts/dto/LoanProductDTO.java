package com.microfinance.loanproducts.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.loanproducttype.dto.ProductTypeDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LoanProductDTO {
    private Long id;
    private String productCode;
    private String name;
    private String description;
    private ProductTypeDTO productType;
    private GeneralConfig.InterestMethod interestMethod;
    private BigDecimal interestRate;
    private BigDecimal minLoanAmount;
    private BigDecimal maxLoanAmount;
    private Integer minTenure;
    private Integer maxTenure;
    private GeneralConfig.TenureUnit tenureUnit;
    private Integer gracePeriod;
    private BigDecimal processingFeeRate;
    private BigDecimal latePaymentFee;
    private BigDecimal prepaymentPenaltyRate;
    private Boolean insuranceRequired;
    private Boolean collateralRequired;
    private Integer minCreditScore;
    private String eligibilityCriteria;
    private String requiredDocuments;
    private GeneralConfig.ProductStatus status;
    private Integer version;
    private Boolean isTemplate;
    private Boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // Fixed: removed duplicate "-dd"
    private LocalDateTime updatedAt;

    // Add these fields if they exist in your entity
    private Long createdBy;
    private Long updatedBy;
    private Long previousVersionId;
    private Boolean deleted; // if you have soft delete

    public static LoanProductDTO fromEntity(LoanProduct product) {
        if (product == null) {
            return null;
        }

        System.out.println("========== CONVERTING LOAN PRODUCT ==========");
        System.out.println("Product ID: " + product.getId());
        System.out.println("Product Class: " + product.getClass().getName());

        LoanProductDTO dto = new LoanProductDTO();
        dto.setId(product.getId());
        dto.setProductCode(product.getProductCode());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());

        // Convert ProductType to DTO - this is safe and prevents circular reference
        if (product.getProductType() != null) {
           // dto.setProductType(ProductTypeDTO.fromEntity(product.getProductType()));
        }

        dto.setInterestMethod(product.getInterestMethod());
        dto.setInterestRate(product.getInterestRate());
        dto.setMinLoanAmount(product.getMinLoanAmount());
        dto.setMaxLoanAmount(product.getMaxLoanAmount());
        dto.setMinTenure(product.getMinTenure());
        dto.setMaxTenure(product.getMaxTenure());
        dto.setTenureUnit(product.getTenureUnit());
        dto.setGracePeriod(product.getGracePeriod());
        dto.setProcessingFeeRate(product.getProcessingFeeRate());
        dto.setLatePaymentFee(product.getLatePaymentFee());
        dto.setPrepaymentPenaltyRate(product.getPrepaymentPenaltyRate());
        dto.setInsuranceRequired(product.getInsuranceRequired());
        dto.setCollateralRequired(product.getCollateralRequired());
        dto.setMinCreditScore(product.getMinCreditScore());
        dto.setEligibilityCriteria(product.getEligibilityCriteria());
        dto.setRequiredDocuments(product.getRequiredDocuments());
        dto.setStatus(product.getStatus());
        dto.setVersion(product.getVersion());
        dto.setIsTemplate(product.getIsTemplate());
        dto.setActive(product.getActive());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());

        // Add these if they exist in your entity
        dto.setCreatedBy(product.getCreatedBy());
        dto.setUpdatedBy(product.getUpdatedBy());
        dto.setPreviousVersionId(product.getPreviousVersionId());
        dto.setDeleted(product.getDeleted());

        return dto;
    }

    // Remove the toEntity() method - it's not recommended for response DTOs
    // Response DTOs should only be used for reading data, not for creating/updating entities
}