// entity/BankAccount.java
package com.microfinance.financials.reconciliation.entity;

import com.microfinance.financials.chartofaccounts.entity.Account;
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
@Table(name = "fin_bank_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String accountName;

    @Column(nullable = false, unique = true, length = 50)
    private String accountNumber;

    @Column(length = 50)
    private String bankName;

    @Column(length = 20)
    private String branchCode;

    @Column(length = 50)
    private String swiftCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chart_of_account_id", nullable = false)
    private Account chartOfAccount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(length = 20)
    private String currency = "KES";

    @Column(length = 20)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, CLOSED

    @Column(name = "last_reconciliation_date")
    private LocalDateTime lastReconciliationDate;

    @Column(name = "opening_balance", precision = 15, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

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



