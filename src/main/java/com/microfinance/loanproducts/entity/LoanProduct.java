package com.microfinance.loanproducts.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microfinance.base.entity.BaseEntity;
import com.microfinance.loanproducttype.entity.ProductType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_products")
@Data
@EqualsAndHashCode(callSuper = true)
//@JsonIgnoreProperties({"productType.loanProducts"}) // ← Break the circular reference
public class LoanProduct extends BaseEntity {

    @NotBlank
    @Column(unique = true)
    private String productCode;

    @NotBlank
    @Column(unique = true)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_type_id", nullable = false)
    @NotNull(message = "Product type is required")
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @NotNull
    private InterestMethod interestMethod;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal interestRate; // Annual percentage rate

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

    @Enumerated(EnumType.STRING)
    @NotNull
    private TenureUnit tenureUnit;

    private Integer gracePeriod; // Days

    @DecimalMin("0.00")
    private BigDecimal processingFeeRate; // Percentage

    @DecimalMin("0.00")
    private BigDecimal latePaymentFee; // Fixed amount

    @DecimalMin("0.00")
    private BigDecimal prepaymentPenaltyRate; // Percentage

    private Boolean insuranceRequired = false;
    private Boolean collateralRequired = false;
    private Integer minCreditScore;

    @Column(columnDefinition = "TEXT")
    private String eligibilityCriteria;

    @Column(columnDefinition = "TEXT")
    private String requiredDocuments; // JSON array of required documents

    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.ACTIVE;

    private Integer version = 1;
    private Long previousVersionId; // For versioning

    private Boolean isTemplate = false;
    private Boolean active = true;

    // Audit fields
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    public LoanProduct() {
        this.insuranceRequired = false;
        this.collateralRequired = false;
        this.isTemplate = false;
        this.active = true;
        this.status = ProductStatus.ACTIVE;
        this.version = 1;
    }

    public enum InterestMethod {
        FLAT,
        REDUCING_BALANCE,
        COMPOUND
    }

    public enum TenureUnit {
        DAYS,
        WEEKS,
        MONTHS,
        YEARS
    }

    public enum ProductStatus {
        DRAFT,
        ACTIVE,
        INACTIVE,
        ARCHIVED
    }

    // Helper methods
    public boolean isEligibleForAmount(BigDecimal amount) {
        return amount.compareTo(minLoanAmount) >= 0 && amount.compareTo(maxLoanAmount) <= 0;
    }

    public boolean isEligibleForTenure(Integer tenure) {
        return tenure >= minTenure && tenure <= maxTenure;
    }

    public BigDecimal calculateProcessingFee(BigDecimal loanAmount) {
        return loanAmount.multiply(processingFeeRate).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}