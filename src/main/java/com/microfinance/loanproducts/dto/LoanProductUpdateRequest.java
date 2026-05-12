package com.microfinance.loanproducts.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanproducts.deserializer.InterestMethodDeserializer;
import com.microfinance.loanproducts.deserializer.TenureUnitDeserializer;
import com.microfinance.loanproducts.entity.LoanProduct;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanProductUpdateRequest {

    private String productCode;
    private String name;
    private String description;
    private Long productTypeId;

    @JsonDeserialize(using = InterestMethodDeserializer.class)
    private GeneralConfig.InterestMethod interestMethod;

    @DecimalMin("0.00")
    private BigDecimal interestRate;

    @DecimalMin("0.00")
    private BigDecimal minLoanAmount;

    @DecimalMin("0.00")
    private BigDecimal maxLoanAmount;

    private Integer minTenure;
    private Integer maxTenure;

    @JsonDeserialize(using = TenureUnitDeserializer.class)
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
    private Boolean active;

    // Add toEntity method for updating existing entity
    public LoanProduct toEntity(LoanProduct existingProduct) {
        if (this.productCode != null) existingProduct.setProductCode(this.productCode);
        if (this.name != null) existingProduct.setName(this.name);
        if (this.description != null) existingProduct.setDescription(this.description);
        if (this.interestMethod != null) existingProduct.setInterestMethod(this.interestMethod);
        if (this.interestRate != null) existingProduct.setInterestRate(this.interestRate);
        if (this.minLoanAmount != null) existingProduct.setMinLoanAmount(this.minLoanAmount);
        if (this.maxLoanAmount != null) existingProduct.setMaxLoanAmount(this.maxLoanAmount);
        if (this.minTenure != null) existingProduct.setMinTenure(this.minTenure);
        if (this.maxTenure != null) existingProduct.setMaxTenure(this.maxTenure);
        if (this.tenureUnit != null) existingProduct.setTenureUnit(this.tenureUnit);
        if (this.gracePeriod != null) existingProduct.setGracePeriod(this.gracePeriod);
        if (this.processingFeeRate != null) existingProduct.setProcessingFeeRate(this.processingFeeRate);
        if (this.latePaymentFee != null) existingProduct.setLatePaymentFee(this.latePaymentFee);
        if (this.prepaymentPenaltyRate != null) existingProduct.setPrepaymentPenaltyRate(this.prepaymentPenaltyRate);
        if (this.insuranceRequired != null) existingProduct.setInsuranceRequired(this.insuranceRequired);
        if (this.collateralRequired != null) existingProduct.setCollateralRequired(this.collateralRequired);
        if (this.minCreditScore != null) existingProduct.setMinCreditScore(this.minCreditScore);
        if (this.eligibilityCriteria != null) existingProduct.setEligibilityCriteria(this.eligibilityCriteria);
        if (this.requiredDocuments != null) existingProduct.setRequiredDocuments(this.requiredDocuments);
        if (this.status != null) existingProduct.setStatus(this.status);
        if (this.active != null) existingProduct.setActive(this.active);

        return existingProduct;
    }
}