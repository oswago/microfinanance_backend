// entity/LoanRecovery.java
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
@Table(name = "fin_loan_recoveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LoanRecovery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recovery_number", nullable = false, unique = true, length = 50)
    private String recoveryNumber;

    @Column(name = "write_off_id")
    private Long writeOffId;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "loan_account_number", length = 50)
    private String loanAccountNumber;

    @Column(name = "borrower_id")
    private Long borrowerId;

    @Column(name = "borrower_name", length = 255)
    private String borrowerName;

    @Column(name = "recovery_date", nullable = false)
    private LocalDate recoveryDate;

    @Column(name = "recovered_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal recoveredAmount;

    @Column(name = "principal_recovered", precision = 15, scale = 2)
    private BigDecimal principalRecovered;

    @Column(name = "interest_recovered", precision = 15, scale = 2)
    private BigDecimal interestRecovered;

    @Column(name = "penalty_recovered", precision = 15, scale = 2)
    private BigDecimal penaltyRecovered;

    @Column(name = "fees_recovered", precision = 15, scale = 2)
    private BigDecimal feesRecovered;

    @Column(length = 20)
    private String recoveryType; // CASH, ASSET_SEIZURE, GUARANTOR_PAYMENT, INSURANCE

    @Column(length = 50)
    private String referenceNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;
}