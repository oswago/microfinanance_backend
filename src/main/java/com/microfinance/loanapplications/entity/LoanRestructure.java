package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.base.entity.User;
import com.microfinance.common.config.GeneralConfig;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_restructures")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRestructure extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false, unique = true, length = 50)
    private String restructureReference;

    @Column(nullable = false, length = 50)
    private String restructureType; // RATE_CHANGE, PRINCIPAL_REDUCTION, CONSOLIDATION, TENURE_EXTENSION

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GeneralConfig.RestructureStatus status;

    // Old values
    @Column(precision = 5, scale = 2)
    private BigDecimal oldInterestRate;

    @Column(precision = 15, scale = 2)
    private BigDecimal oldPrincipal;

    @Column(precision = 15, scale = 2)
    private BigDecimal oldOutstanding;

    private Integer oldTenureMonths;

    // New values
    @Column(precision = 5, scale = 2)
    private BigDecimal newInterestRate;

    @Column(precision = 15, scale = 2)
    private BigDecimal newPrincipal;

    @Column(precision = 15, scale = 2)
    private BigDecimal newOutstanding;

    private Integer newTenureMonths;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(length = 100)
    private String approvalReference;

    @Column(columnDefinition = "TEXT")
    private String comments;

    // Request details
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id")
    private User requestedBy;

    @Column(nullable = false)
    private LocalDateTime requestDate;

    // Approval details
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    private LocalDateTime approvalDate;

    @Column(columnDefinition = "TEXT")
    private String approvalComments;

    // Rejection details
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by_id")
    private User rejectedBy;

    private LocalDateTime rejectionDate;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    // Effective date of restructure
    private LocalDateTime effectiveDate;

    // New repayment schedule reference (if schedule changes)
    @Column(length = 50)
    private String newScheduleReference;

    // Audit fields
    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public boolean isApproved() {
        return status == GeneralConfig.RestructureStatus.APPROVED;
    }

    public boolean isRejected() {
        return status == GeneralConfig.RestructureStatus.REJECTED;
    }

    public boolean isPending() {
        return status == GeneralConfig.RestructureStatus.PENDING;
    }

    public void approve(User approver, String comments) {
        this.status = GeneralConfig.RestructureStatus.APPROVED;
        this.approvedBy = approver;
        this.approvalDate = LocalDateTime.now();
        this.approvalComments = comments;
    }

    public void reject(User rejector, String reason) {
        this.status = GeneralConfig.RestructureStatus.REJECTED;
        this.rejectedBy = rejector;
        this.rejectionDate = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    public BigDecimal getPrincipalReduction() {
        if (oldPrincipal != null && newPrincipal != null) {
            return oldPrincipal.subtract(newPrincipal);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getInterestRateReduction() {
        if (oldInterestRate != null && newInterestRate != null) {
            return oldInterestRate.subtract(newInterestRate);
        }
        return BigDecimal.ZERO;
    }

    public Integer getTenureExtension() {
        if (oldTenureMonths != null && newTenureMonths != null) {
            return newTenureMonths - oldTenureMonths;
        }
        return 0;
    }
}