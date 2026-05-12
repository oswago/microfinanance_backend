package com.microfinance.loanproducts.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanproducts.deserializer.InterestMethodDeserializer;
import com.microfinance.loanproducts.deserializer.TenureUnitDeserializer;
import com.microfinance.loanproducts.entity.LoanProduct;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanProductCreateRequest {

    @NotBlank
    private String productCode;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Long productTypeId; // Use ID instead of full object

    @NotNull
    @JsonDeserialize(using = InterestMethodDeserializer.class)
    private GeneralConfig.InterestMethod interestMethod;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal interestRate;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal minLoanAmount;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal maxLoanAmount;

    @NotNull
    private Integer minTenure;

    @NotNull
    private Integer maxTenure;

    @NotNull
    @JsonDeserialize(using = TenureUnitDeserializer.class)
    private GeneralConfig.TenureUnit tenureUnit;

    private Integer gracePeriod = 0;

    @DecimalMin("0.00")
    private BigDecimal processingFeeRate = BigDecimal.ZERO;

    @DecimalMin("0.00")
    private BigDecimal latePaymentFee = BigDecimal.ZERO;

    @DecimalMin("0.00")
    private BigDecimal prepaymentPenaltyRate = BigDecimal.ZERO;

    private Boolean insuranceRequired = false;
    private Boolean collateralRequired = false;
    private Integer minCreditScore;

    private String eligibilityCriteria;
    private String requiredDocuments;

    // Add toEntity method
    public LoanProduct toEntity() {
        LoanProduct product = new LoanProduct();
        product.setProductCode(this.productCode);
        product.setName(this.name);
        product.setDescription(this.description);
        product.setInterestMethod(this.interestMethod);
        product.setInterestRate(this.interestRate);
        product.setMinLoanAmount(this.minLoanAmount);
        product.setMaxLoanAmount(this.maxLoanAmount);
        product.setMinTenure(this.minTenure);
        product.setMaxTenure(this.maxTenure);
        product.setTenureUnit(this.tenureUnit);
        product.setGracePeriod(this.gracePeriod);
        product.setProcessingFeeRate(this.processingFeeRate);
        product.setLatePaymentFee(this.latePaymentFee);
        product.setPrepaymentPenaltyRate(this.prepaymentPenaltyRate);
        product.setInsuranceRequired(this.insuranceRequired);
        product.setCollateralRequired(this.collateralRequired);
        product.setMinCreditScore(this.minCreditScore);
        product.setEligibilityCriteria(this.eligibilityCriteria);
        product.setRequiredDocuments(this.requiredDocuments);

        return product;
    }
}