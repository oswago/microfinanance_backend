// entity/BankStatement.java
package com.microfinance.financials.reconciliation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fin_bank_statements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_account_id", nullable = false)
    private Long bankAccountId;

    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;

    @Column(name = "opening_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal closingBalance;

    @Column(name = "total_deposits", precision = 15, scale = 2)
    private BigDecimal totalDeposits = BigDecimal.ZERO;

    @Column(name = "total_withdrawals", precision = 15, scale = 2)
    private BigDecimal totalWithdrawals = BigDecimal.ZERO;

    @Column(length = 50)
    private String fileName;

    @Column(length = 20)
    private String status = "PENDING"; // PENDING, PROCESSED, RECONCILED

    @Column(columnDefinition = "TEXT")
    private String notes;
}