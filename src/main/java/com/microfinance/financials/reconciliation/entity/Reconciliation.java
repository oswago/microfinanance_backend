
// entity/Reconciliation.java
package com.microfinance.financials.reconciliation.entity;

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
@Table(name = "fin_reconciliations", indexes = {
  //  @Index(name = "idx_rec_bank_account", columnList = "bank_account_id"),
   // @Index(name = "idx_rec_date", columnList = "reconciliation_date"),
   // @Index(name = "idx_rec_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Reconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reconciliation_number", nullable = false, unique = true, length = 50)
    private String reconciliationNumber;

    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @Column(name = "bank_account_id", nullable = false)
    private Long bankAccountId;

    @Column(name = "bank_account_name")
    private String bankAccountName;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal systemBalance;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal statementBalance;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal difference;

    @Column(length = 20)
    private String status = "PENDING"; // PENDING, COMPLETED, ADJUSTED

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;
}