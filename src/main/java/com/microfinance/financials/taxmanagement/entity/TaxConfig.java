// entity/TaxConfig.java
package com.microfinance.financials.taxmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fin_tax_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TaxConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String taxCode;

    @Column(nullable = false, length = 100)
    private String taxName;

    @Column(nullable = false, length = 50)
    private String taxType; // WITHHOLDING_TAX, VAT, CORPORATE_TAX, PAYE, NSSF, NHIF

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(length = 20)
    private String calculationMethod; // PERCENTAGE, FIXED_AMOUNT, TIERED

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_compound")
    private Boolean isCompound = false;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "minimum_amount", precision = 15, scale = 2)
    private BigDecimal minimumAmount;

    @Column(name = "maximum_amount", precision = 15, scale = 2)
    private BigDecimal maximumAmount;

    @Column(name = "exemption_threshold", precision = 15, scale = 2)
    private BigDecimal exemptionThreshold;

    @Column(name = "gl_account_id")
    private Long glAccountId;

    @Column(name = "gl_account_code")
    private String glAccountCode;

    @Column(name = "gl_account_name")
    private String glAccountName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}



