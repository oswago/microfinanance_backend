// entity/ProvisionCalculation.java
package com.microfinance.financials.provisions.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fin_provision_calculations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProvisionCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "calculation_number", nullable = false, unique = true, length = 50)
    private String calculationNumber;

    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "loan_account_number", length = 50)
    private String loanAccountNumber;

    @Column(name = "borrower_id")
    private Long borrowerId;

    @Column(name = "borrower_name", length = 255)
    private String borrowerName;

    @Column(name = "principal_outstanding", precision = 15, scale = 2)
    private BigDecimal principalOutstanding;

    @Column(name = "interest_outstanding", precision = 15, scale = 2)
    private BigDecimal interestOutstanding;

    @Column(name = "total_outstanding", precision = 15, scale = 2)
    private BigDecimal totalOutstanding;

    @Column(name = "days_past_due")
    private Integer daysPastDue;

    @Column(name = "aging_bucket", length = 50)
    private String agingBucket; // 1-30, 31-60, 61-90, 91-180, 180+

    @Column(name = "provision_rate", precision = 5, scale = 2)
    private BigDecimal provisionRate;

    @Column(name = "provision_amount", precision = 15, scale = 2)
    private BigDecimal provisionAmount;

    @Column(name = "existing_provision", precision = 15, scale = 2)
    private BigDecimal existingProvision;

    @Column(name = "provision_adjustment", precision = 15, scale = 2)
    private BigDecimal provisionAdjustment;

    @Column(length = 20)
    private String status; // DRAFT, APPROVED, POSTED

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "calculated_by")
    private Long calculatedBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}



