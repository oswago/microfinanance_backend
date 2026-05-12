package com.microfinance.loanproducts.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microfinance.base.entity.BaseEntity;
import com.microfinance.common.config.GeneralConfig;
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

    @JsonIgnoreProperties({"loanProducts"})  // ← CHANGE TO THIS (remove @JsonIgnore)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @NotNull
    private GeneralConfig.InterestMethod interestMethod;

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
    private GeneralConfig.TenureUnit tenureUnit;

    private Integer gracePeriod; // Days

    private Integer maxActiveLoans;

    @DecimalMin("0.00")
    private BigDecimal processingFeeRate; // Percentage

    @DecimalMin("0.00")
    private BigDecimal latePaymentFee; // Fixed amount

    @DecimalMin("0.00")
    private BigDecimal prepaymentPenaltyRate; // Percentage

    // NEW FIELD: Early Repayment Configuration
    @Column(name = "early_repayment_allowed")
    private Boolean earlyRepaymentAllowed = true;

    @DecimalMin("1.00")
    @Column(name = "provision_rate")
    private BigDecimal provisionRate;

    @DecimalMin("0.00")
    @Column(name = "early_repayment_fee_rate")
    private BigDecimal earlyRepaymentFeeRate = BigDecimal.valueOf(2.0); // Default 2%

    @Column(name = "early_repayment_min_period")
    private Integer earlyRepaymentMinPeriod = 3; // Minimum months before early repayment allowed

    @Column(name = "early_repayment_interest_rebate")
    private Boolean earlyRepaymentInterestRebate = true;

    @DecimalMin("0.00")
    @Column(name = "early_repayment_max_rebate")
    private BigDecimal earlyRepaymentMaxRebate = BigDecimal.valueOf(50.0); // Maximum 50% rebate

    private Boolean insuranceRequired = false;
    private Boolean collateralRequired = false;
    private Integer minCreditScore;

    @Column(columnDefinition = "TEXT")
    private String eligibilityCriteria;

    @Column(columnDefinition = "TEXT")
    private String requiredDocuments; // JSON array of required documents

    @Enumerated(EnumType.STRING)
    private GeneralConfig.ProductStatus status = GeneralConfig.ProductStatus.ACTIVE;

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
        this.status = GeneralConfig.ProductStatus.ACTIVE;
        this.version = 1;
        // Initialize early repayment fields with defaults
        this.earlyRepaymentAllowed = true;
        this.earlyRepaymentFeeRate = BigDecimal.valueOf(2.0);
        this.earlyRepaymentMinPeriod = 3;
        this.earlyRepaymentInterestRebate = true;
        this.earlyRepaymentMaxRebate = BigDecimal.valueOf(50.0);
    }

    // ENHANCED HELPER METHODS

    public boolean isEligibleForAmount(BigDecimal amount) {
        return amount.compareTo(minLoanAmount) >= 0 && amount.compareTo(maxLoanAmount) <= 0;
    }

    public boolean isEligibleForTenure(Integer tenure) {
        return tenure >= minTenure && tenure <= maxTenure;
    }

    public BigDecimal calculateProcessingFee(BigDecimal loanAmount) {
        return loanAmount.multiply(processingFeeRate).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }

    // NEW METHODS FOR EARLY REPAYMENT
    /**
     * Check if early repayment is allowed for this loan product
     */
    public boolean isEarlyRepaymentAllowed() {
        return Boolean.TRUE.equals(earlyRepaymentAllowed) && status == GeneralConfig.ProductStatus.ACTIVE;
    }

    /**
     * Calculate early repayment fee based on outstanding principal
     */
    public BigDecimal calculateEarlyRepaymentFee(BigDecimal outstandingPrincipal) {
        if (!isEarlyRepaymentAllowed()) {
            return BigDecimal.ZERO;
        }
        return outstandingPrincipal.multiply(earlyRepaymentFeeRate)
                .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP)
                .max(BigDecimal.valueOf(100)); // Minimum fee of 100
    }

    /**
     * Calculate interest rebate for early repayment
     */
    public BigDecimal calculateInterestRebate(BigDecimal remainingInterest, long remainingMonths) {
        if (!isEarlyRepaymentAllowed() || !Boolean.TRUE.equals(earlyRepaymentInterestRebate)) {
            return BigDecimal.ZERO;
        }

        double rebatePercentage = calculateRebatePercentage(remainingMonths);
        BigDecimal rebateAmount = remainingInterest.multiply(BigDecimal.valueOf(rebatePercentage / 100));

        // Apply maximum rebate limit
        BigDecimal maxRebateAmount = remainingInterest.multiply(earlyRepaymentMaxRebate)
                .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);

        return rebateAmount.min(maxRebateAmount);
    }

    /**
     * Calculate rebate percentage based on remaining months
     */
    private double calculateRebatePercentage(long remainingMonths) {
        if (remainingMonths >= 12) return 50.0;
        if (remainingMonths >= 6) return 30.0;
        if (remainingMonths >= 3) return 15.0;
        return 5.0;
    }

    /**
     * Check if loan meets minimum period requirement for early repayment
     */
    public boolean meetsEarlyRepaymentMinPeriod(Integer completedMonths) {
        return completedMonths != null && completedMonths >= earlyRepaymentMinPeriod;
    }

    /**
     * Get early repayment terms and conditions
     */
    public String getEarlyRepaymentTerms() {
        StringBuilder terms = new StringBuilder();
        terms.append("Early repayment ");

        if (!isEarlyRepaymentAllowed()) {
            terms.append("is not allowed for this product.");
            return terms.toString();
        }

        terms.append("is allowed with the following terms:\n");
        terms.append("- Early repayment fee: ").append(earlyRepaymentFeeRate).append("% of outstanding principal\n");
        terms.append("- Minimum period before early repayment: ").append(earlyRepaymentMinPeriod).append(" months\n");

        if (Boolean.TRUE.equals(earlyRepaymentInterestRebate)) {
            terms.append("- Interest rebate available: Yes (up to ").append(earlyRepaymentMaxRebate).append("%)\n");
        } else {
            terms.append("- Interest rebate available: No\n");
        }

        return terms.toString();
    }

    /**
     * Validate if a loan is eligible for early repayment based on completed months
     */
    public boolean validateEarlyRepaymentEligibility(Integer completedMonths) {
        if (!isEarlyRepaymentAllowed()) {
            return false;
        }

        if (earlyRepaymentMinPeriod != null && earlyRepaymentMinPeriod > 0) {
            return completedMonths != null && completedMonths >= earlyRepaymentMinPeriod;
        }

        return true;
    }

    /**
     * Calculate total early repayment amount including fees and rebates
     */
    public BigDecimal calculateTotalEarlyRepaymentAmount(BigDecimal outstandingPrincipal,
                                                         BigDecimal remainingInterest,
                                                         long remainingMonths) {
        BigDecimal earlyRepaymentFee = calculateEarlyRepaymentFee(outstandingPrincipal);
        BigDecimal interestRebate = calculateInterestRebate(remainingInterest, remainingMonths);
        BigDecimal netInterestDue = remainingInterest.subtract(interestRebate).max(BigDecimal.ZERO);

        return outstandingPrincipal.add(netInterestDue).add(earlyRepaymentFee);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        // Ensure early repayment fields are initialized
        if (earlyRepaymentAllowed == null) {
            earlyRepaymentAllowed = true;
        }
        if (earlyRepaymentFeeRate == null) {
            earlyRepaymentFeeRate = BigDecimal.valueOf(2.0);
        }
        if (earlyRepaymentMinPeriod == null) {
            earlyRepaymentMinPeriod = 3;
        }
        if (earlyRepaymentInterestRebate == null) {
            earlyRepaymentInterestRebate = true;
        }
        if (earlyRepaymentMaxRebate == null) {
            earlyRepaymentMaxRebate = BigDecimal.valueOf(50.0);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String paymentFrequency;
}