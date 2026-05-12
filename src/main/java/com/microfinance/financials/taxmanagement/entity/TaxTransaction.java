// entity/TaxTransaction.java
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
@Table(name = "fin_tax_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TaxTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tax_config_id", nullable = false)
    private Long taxConfigId;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "tax_name", length = 100)
    private String taxName;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "reference_type", nullable = false, length = 50)
    private String referenceType; // LOAN_REPAYMENT, FEE_CHARGE, INTEREST_PAYMENT

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "taxable_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxableAmount;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "withheld_amount", precision = 15, scale = 2)
    private BigDecimal withheldAmount;

    @Column(name = "remitted_amount", precision = 15, scale = 2)
    private BigDecimal remittedAmount;

    @Column(length = 50)
    private String status; // CALCULATED, WITHHELD, REMITTED, ADJUSTED

    @Column(name = "remittance_date")
    private LocalDate remittanceDate;

    @Column(name = "remittance_reference", length = 100)
    private String remittanceReference;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;
}