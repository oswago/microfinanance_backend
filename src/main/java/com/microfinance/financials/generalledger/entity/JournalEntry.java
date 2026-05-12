// entity/JournalEntry.java
package com.microfinance.financials.generalledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fin_journal_entries", indexes = {
    @Index(name = "idx_je_date", columnList = "entry_date"),
    @Index(name = "idx_je_number", columnList = "journal_number"),
    @Index(name = "idx_je_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "journal_number", nullable = false, unique = true, length = 50)
    private String journalNumber;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(length = 100)
    private String description;

    @Column(length = 50)
    private String journalType; // DISBURSEMENT, REPAYMENT, ACCRUAL, ADJUSTMENT, etc.

    @Column(length = 20)
    private String status; // DRAFT, POSTED, REVERSED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_period_id")
    private FinancialPeriod financialPeriod;

    @Column(name = "posted_by")
    private Long postedBy;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "reversed_by")
    private Long reversedBy;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "reversal_reason", length = 500)
    private String reversalReason;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryLine> lines = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "reference_number", length = 50)
    private String referenceNumber;
}