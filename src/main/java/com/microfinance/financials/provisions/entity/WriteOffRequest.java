// entity/WriteOffRequest.java
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
@Table(name = "fin_write_off_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class WriteOffRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_number", nullable = false, unique = true, length = 50)
    private String requestNumber;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "loan_account_number", length = 50)
    private String loanAccountNumber;

    @Column(name = "borrower_id")
    private Long borrowerId;

    @Column(name = "borrower_name", length = 255)
    private String borrowerName;

    @Column(name = "principal_amount", precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_amount", precision = 15, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "penalty_amount", precision = 15, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "fees_amount", precision = 15, scale = 2)
    private BigDecimal feesAmount;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "provision_amount", precision = 15, scale = 2)
    private BigDecimal provisionAmount;

    @Column(name = "net_write_off", precision = 15, scale = 2)
    private BigDecimal netWriteOff;

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Column(length = 50)
    private String reason; // FRAUD, DEATH, INSOLVENCY, UNCOLLECTIBLE, OTHER

    @Column(columnDefinition = "TEXT")
    private String reasonDescription;

    @Column(length = 20)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, COMPLETED

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    @Column(name = "write_off_date")
    private LocalDate writeOffDate;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "requested_by")
    private Long requestedBy;

}