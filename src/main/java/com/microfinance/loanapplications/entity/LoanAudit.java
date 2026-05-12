package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.base.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loan_audits")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanAudit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false, length = 50)
    private String action; // CREATE, UPDATE, DISBURSE, REPAY, WRITE_OFF, CLOSE, RESTRUCTURE, RESCHEDULE

    @Column(length = 50)
    private String entityType; // LOAN, REPAYMENT_SCHEDULE, REPAYMENT, etc.

    private Long entityId;

    @Column(length = 100)
    private String fieldName;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id")
    private User performedBy;

    @Column(nullable = false)
    private LocalDateTime performedAt;

    @Column(length = 45)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(length = 255)
    private String userAgent;

    @Column(length = 50)
    private String sessionId;

    @Column(length = 50)
    private String correlationId; // For tracking related events

    @Column(nullable = false)
    private Boolean isSystemAction = false;

    // Additional metadata stored as JSON
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    protected void onCreate() {
        performedAt = LocalDateTime.now();
    }

    // Helper methods
    public static LoanAudit createAudit(Loan loan, String action, User performedBy, String details) {
        return LoanAudit.builder()
                .loan(loan)
                .action(action)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .details(details)
                .build();
    }

    public static LoanAudit createFieldAudit(Loan loan, String fieldName, 
                                              Object oldValue, Object newValue, 
                                              User performedBy) {
        return LoanAudit.builder()
                .loan(loan)
                .action("UPDATE")
                .entityType("LOAN")
                .entityId(loan.getId())
                .fieldName(fieldName)
                .oldValue(oldValue != null ? oldValue.toString() : null)
                .newValue(newValue != null ? newValue.toString() : null)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();
    }

    public boolean isFieldChange() {
        return fieldName != null;
    }

    public boolean hasValueChanged() {
        if (oldValue == null && newValue == null) {
            return false;
        }
        if (oldValue == null || newValue == null) {
            return true;
        }
        return !oldValue.equals(newValue);
    }
}