
// entity/ReconciliationItem.java
package com.microfinance.financials.reconciliation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fin_reconciliation_items", indexes = {
    //@Index(name = "idx_rec_item_reconciliation", columnList = "reconciliation_id"),
   // @Index(name = "idx_rec_item_type", columnList = "item_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reconciliation_id", nullable = false)
    private Long reconciliationId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(length = 100)
    private String description;

    @Column(length = 50)
    private String referenceNumber;

    @Column(nullable = false, length = 20)
    private String itemType; // DEPOSIT, WITHDRAWAL, CHEQUE, TRANSFER, SERVICE_CHARGE, INTEREST

    @Column(nullable = false, length = 20)
    private String category; // SYSTEM_ONLY, BANK_ONLY, MISMATCH

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 20)
    private String status = "PENDING"; // PENDING, MATCHED, ADJUSTED

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(name = "is_matched")
    private Boolean isMatched = false;

    @Column(name = "matched_with")
    private String matchedWith;

    @Column(name = "adjusted_at")
    private LocalDate adjustedAt;

    @Column(name = "adjusted_by")
    private Long adjustedBy;
}