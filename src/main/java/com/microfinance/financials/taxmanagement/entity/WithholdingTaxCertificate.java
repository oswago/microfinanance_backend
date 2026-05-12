// entity/WithholdingTaxCertificate.java
package com.microfinance.financials.taxmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fin_withholding_tax_certificates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class WithholdingTaxCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certificate_number", nullable = false, unique = true, length = 100)
    private String certificateNumber;

    @Column(name = "tax_transaction_id", nullable = false)
    private Long taxTransactionId;

    @Column(name = "borrower_id", nullable = false)
    private Long borrowerId;

    @Column(name = "borrower_name", length = 255)
    private String borrowerName;

    @Column(name = "borrower_tin", length = 50)
    private String borrowerTin;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "loan_account_number", length = 100)
    private String loanAccountNumber;

    @Column(name = "interest_amount", precision = 15, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "withholding_tax_rate", precision = 5, scale = 2)
    private BigDecimal withholdingTaxRate;

    @Column(name = "withholding_tax_amount", precision = 15, scale = 2)
    private BigDecimal withholdingTaxAmount;

    @Column(name = "certificate_date", nullable = false)
    private LocalDate certificateDate;

    @Column(name = "period_start_date")
    private LocalDate periodStartDate;

    @Column(name = "period_end_date")
    private LocalDate periodEndDate;

    @Column(length = 50)
    private String status = "GENERATED"; // GENERATED, PRINTED, CANCELLED

    @Column(name = "printed_at")
    private LocalDateTime printedAt;

    @Column(name = "printed_by")
    private Long printedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;
}