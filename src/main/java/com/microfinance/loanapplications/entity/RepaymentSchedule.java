package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.common.config.GeneralConfig;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "repayment_schedules")
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = true)
public class RepaymentSchedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(nullable = false)
    private LocalDate dueDate;

    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal totalDueAmount;
    private BigDecimal outstandingAmount;


    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalDue;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal interestDue;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDue;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalPaid = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal interestPaid = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GeneralConfig.InstallmentStatus status = GeneralConfig.InstallmentStatus.PENDING;

    private LocalDate paymentDate;

    private String paymentMethod;

    @Column(precision = 15, scale = 2)
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    private BigDecimal paidAmount;

    private LocalDate paidDate;

    private String transactionReference;

    private String notes;

    private Integer daysOverdue = 0;

    @Column(precision = 15, scale = 2)
    private BigDecimal penaltyAccrued = BigDecimal.ZERO;

    // ========== NEW FINANCIAL TRACKING COLUMNS ==========

    /**
     * Total penalty amount paid on this installment
     */
    @Column(name = "penalty_paid", precision = 15, scale = 2)
    private BigDecimal penaltyPaid = BigDecimal.ZERO;

    /**
     * Total fees amount due on this installment
     */
    @Column(name = "fees_due", precision = 15, scale = 2)
    private BigDecimal feesDue = BigDecimal.ZERO;

    /**
     * Total fees amount paid on this installment
     */
    @Column(name = "fees_paid", precision = 15, scale = 2)
    private BigDecimal feesPaid = BigDecimal.ZERO;

    /**
     * Principal outstanding after payments
     */
    @Column(name = "principal_outstanding", precision = 15, scale = 2)
    private BigDecimal principalOutstanding;

    /**
     * Interest outstanding after payments
     */
    @Column(name = "interest_outstanding", precision = 15, scale = 2)
    private BigDecimal interestOutstanding;

    /**
     * Penalty outstanding after payments
     */
    @Column(name = "penalty_outstanding", precision = 15, scale = 2)
    private BigDecimal penaltyOutstanding = BigDecimal.ZERO;

    /**
     * Date when this installment was last modified
     */
    @Column(name = "last_modified_date")
    private LocalDate lastModifiedDate;

    /**
     * Allocation breakdown of payment (stored as JSON string)
     * Format: {"principal": xxx, "interest": xxx, "penalty": xxx, "fees": xxx}
     */
    @Column(name = "payment_allocation", columnDefinition = "TEXT")
    private String paymentAllocation;

    // ========== END OF NEW COLUMNS ==========

    public BigDecimal getOutstandingAmount() {
        return totalDue.subtract(totalPaid);
    }

    public boolean isOverdue() {
        return status == GeneralConfig.InstallmentStatus.PENDING && dueDate.isBefore(LocalDate.now());
    }

    public boolean isFullyPaid() {
        return totalPaid.compareTo(totalDue) >= 0;
    }

    /**
     * Apply payment with proper allocation tracking
     */
    public void applyPayment(BigDecimal amount) {
        applyPayment(amount, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * Apply payment with specific allocation for fees and penalties
     */
    public void applyPayment(BigDecimal amount, BigDecimal feeAmount, BigDecimal penaltyPaymentAmount) {
        BigDecimal remainingAmount = amount;

        // Track payment allocation
        BigDecimal principalPaymentAllocated = BigDecimal.ZERO;
        BigDecimal interestPaymentAllocated = BigDecimal.ZERO;
        BigDecimal penaltyPaymentAllocated = penaltyPaymentAmount;
        BigDecimal feesPaymentAllocated = feeAmount;

        // Adjust remaining amount after fees and penalties
        remainingAmount = remainingAmount.subtract(feeAmount).subtract(penaltyPaymentAmount);

        // Update fees paid
        if (feesDue != null && feesPaymentAllocated.compareTo(BigDecimal.ZERO) > 0) {
            feesPaid = (feesPaid != null ? feesPaid : BigDecimal.ZERO).add(feesPaymentAllocated);
        }

        // Update penalty paid
        if (penaltyPaymentAllocated.compareTo(BigDecimal.ZERO) > 0) {
            penaltyPaid = (penaltyPaid != null ? penaltyPaid : BigDecimal.ZERO).add(penaltyPaymentAllocated);
            penaltyOutstanding = penaltyAmount.subtract(penaltyPaid);
        }

        // Pay penalty accrued first (legacy)
        if (penaltyAccrued.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal penaltyPaymentFromAccrued = remainingAmount.min(penaltyAccrued);
            penaltyAccrued = penaltyAccrued.subtract(penaltyPaymentFromAccrued);
            penaltyPaid = (penaltyPaid != null ? penaltyPaid : BigDecimal.ZERO).add(penaltyPaymentFromAccrued);
            remainingAmount = remainingAmount.subtract(penaltyPaymentFromAccrued);
            penaltyPaymentAllocated = penaltyPaymentAllocated.add(penaltyPaymentFromAccrued);
        }

        // Pay interest
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal interestOutstandingAmt = interestDue.subtract(interestPaid);
            BigDecimal interestPayment = remainingAmount.min(interestOutstandingAmt);
            interestPaid = interestPaid.add(interestPayment);
            remainingAmount = remainingAmount.subtract(interestPayment);
            interestPaymentAllocated = interestPayment;

            // Update interest outstanding
            interestOutstanding = interestDue.subtract(interestPaid);
        }

        // Pay principal
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal principalOutstandingAmt = principalDue.subtract(principalPaid);
            BigDecimal principalPayment = remainingAmount.min(principalOutstandingAmt);
            principalPaid = principalPaid.add(principalPayment);
            remainingAmount = remainingAmount.subtract(principalPayment);
            principalPaymentAllocated = principalPayment;

            // Update principal outstanding
            principalOutstanding = principalDue.subtract(principalPaid);
        }

        // Update total paid
        totalPaid = principalPaid.add(interestPaid)
                .add(penaltyPaid != null ? penaltyPaid : BigDecimal.ZERO)
                .add(feesPaid != null ? feesPaid : BigDecimal.ZERO);

        // Store payment allocation as JSON for audit trail
        this.paymentAllocation = String.format(
                "{\"principal\":%.2f,\"interest\":%.2f,\"penalty\":%.2f,\"fees\":%.2f}",
                principalPaymentAllocated, interestPaymentAllocated,
                penaltyPaymentAllocated, feesPaymentAllocated
        );

        // Update last modified date
        this.lastModifiedDate = LocalDate.now();

        // Update status
        if (getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            status = GeneralConfig.InstallmentStatus.PAID;
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            status = GeneralConfig.InstallmentStatus.PARTIAL;
        }

        // Update overdue status
        if (isOverdue() && status != GeneralConfig.InstallmentStatus.PAID) {
            status = GeneralConfig.InstallmentStatus.OVERDUE;
            daysOverdue = (int) java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now());
        }
    }

    /**
     * Update outstanding balances after any payment
     */
    @PreUpdate
    @PrePersist
    public void updateOutstandingBalances() {
        this.principalOutstanding = principalDue.subtract(principalPaid);
        this.interestOutstanding = interestDue.subtract(interestPaid);
        this.penaltyOutstanding = penaltyAmount.subtract(penaltyPaid != null ? penaltyPaid : BigDecimal.ZERO);

        // Update total due if not set
        if (this.totalDue == null || this.totalDue.compareTo(BigDecimal.ZERO) == 0) {
            this.totalDue = principalDue.add(interestDue)
                    .add(penaltyAmount)
                    .add(feesDue != null ? feesDue : BigDecimal.ZERO);
        }

        // Update outstanding amount
        this.outstandingAmount = getOutstandingAmount();
    }

    /**
     * Get penalty paid (non-null safe)
     */
    public BigDecimal getPenaltyPaid() {
        return penaltyPaid != null ? penaltyPaid : BigDecimal.ZERO;
    }

    /**
     * Get fees paid (non-null safe)
     */
    public BigDecimal getFeesPaid() {
        return feesPaid != null ? feesPaid : BigDecimal.ZERO;
    }

    /**
     * Get principal outstanding (non-null safe)
     */
    public BigDecimal getPrincipalOutstanding() {
        return principalOutstanding != null ? principalOutstanding : principalDue.subtract(principalPaid);
    }

    /**
     * Get interest outstanding (non-null safe)
     */
    public BigDecimal getInterestOutstanding() {
        return interestOutstanding != null ? interestOutstanding : interestDue.subtract(interestPaid);
    }

    /**
     * Get penalty outstanding (non-null safe)
     */
    public BigDecimal getPenaltyOutstanding() {
        return penaltyOutstanding != null ? penaltyOutstanding : penaltyAmount.subtract(getPenaltyPaid());
    }

    /**
     * Check if this installment is fully paid including penalties and fees
     */
  /*  public boolean isCompletelyPaid() {
        return getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0
                && getPenaltyOutstanding().compareTo(BigDecimal.ZERO) <= 0
                && (feesOutstanding == null || feesOutstanding.compareTo(BigDecimal.ZERO) <= 0);
    }
    */
}