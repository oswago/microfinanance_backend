package com.microfinance.loanapplications.entity;

import com.fasterxml.jackson.annotation.*;
import com.microfinance.base.entity.BaseEntity;
import com.microfinance.base.entity.User;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.system.entity.Branch;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.apache.commons.math3.FieldElement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loans")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Loan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "loan_application_id")
    @JsonIgnoreProperties({"loan", "borrower"})
    @ToString.Exclude
    private LoanApplication loanApplication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    @JsonIgnoreProperties({"loans", "documents", "guarantors"})
    @ToString.Exclude
    private Borrower borrower;

    @Column(nullable = false, unique = true, length = 20)
    private String loanAccountNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private Integer tenureMonths;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id")
    @JsonIgnoreProperties({"loans"})
    @ToString.Exclude
    private LoanProduct loanProduct;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GeneralConfig.LoanStatus status;

    private LocalDate disbursementDate;
    private LocalDate maturityDate;
    private LocalDate closedDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDue;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(precision = 15, scale = 2)
    private BigDecimal penaltyAccrued = BigDecimal.ZERO;

    private Integer daysDelinquent = 0;

    @Column(name = "risk_rating", length = 20)
    private String riskRating; // LOW, MEDIUM, HIGH, CRITICAL


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disbursed_by")
    @JsonIgnore
    @ToString.Exclude
    private User disbursedBy;

    private String disbursementMethod;
    private String transactionReference;
    private String disbursementNotes;
    private BigDecimal netDisbursementAmount;

    private BigDecimal writeOffAmount;
    private LocalDate writeOffDate;
    private String writeOffReason;
    private String writeOffApprovalReference;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "write_off_by")
    @JsonIgnore
    @ToString.Exclude
    private User writeOffBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    @JsonIgnore
    @ToString.Exclude
    private User closedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    @JsonIgnore
    @ToString.Exclude
    private Branch branch;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    private List<RepaymentSchedule> repaymentSchedules = new ArrayList<>();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    private List<LoanRepayment> repayments = new ArrayList<>();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    private List<LoanReschedule> reschedules = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GeneralConfig.WriteOffStatus writeOffStatus;

    @Column(length = 20)
    private String recoveryPlan;

    private String writeOffComments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_officer_id")
    private User loanOfficer;

    @Column(name = "last_contact_date")
    private LocalDate lastContactDate;

    // ========== NEW FINANCIAL TRACKING COLUMNS (Added for backward compatibility) ==========

    /**
     * Total principal amount paid to date
     * Calculated from repayment schedules
     */
    @Column(name = "principal_paid", precision = 15, scale = 2)
    private BigDecimal principalPaid = BigDecimal.ZERO;

    /**
     * Total interest amount paid to date
     * Calculated from repayment schedules
     */
    @Column(name = "interest_paid", precision = 15, scale = 2)
    private BigDecimal interestPaid = BigDecimal.ZERO;

    /**
     * Total penalty amount paid to date
     * Calculated from repayment schedules
     */
    @Column(name = "penalty_paid", precision = 15, scale = 2)
    private BigDecimal penaltyPaid = BigDecimal.ZERO;

    /**
     * Total fees amount paid to date
     * Calculated from repayment schedules
     */
    @Column(name = "fees_paid", precision = 15, scale = 2)
    private BigDecimal feesPaid = BigDecimal.ZERO;

    /**
     * Principal outstanding (remaining principal not yet paid)
     * principalAmount - principalPaid
     */
    @Column(name = "principal_outstanding", precision = 15, scale = 2)
    private BigDecimal principalOutstanding;

    /**
     * Interest outstanding (accrued interest not yet paid)
     * Total interest due minus interest paid
     */
    @Column(name = "interest_outstanding", precision = 15, scale = 2)
    private BigDecimal interestOutstanding;

    /**
     * Penalty outstanding (accrued penalties not yet paid)
     */
    @Column(name = "penalty_outstanding", precision = 15, scale = 2)
    private BigDecimal penaltyOutstanding = BigDecimal.ZERO;

    /**
     * Fees outstanding (accrued fees not yet paid)
     */
    @Column(name = "fees_outstanding", precision = 15, scale = 2)
    private BigDecimal feesOutstanding = BigDecimal.ZERO;

    /**
     * Total interest due over the life of the loan
     */
    @Column(name = "total_interest_due", precision = 15, scale = 2)
    private BigDecimal totalInterestDue;

    /**
     * Total penalty accrued over the life of the loan
     */
    @Column(name = "total_penalty_accrued", precision = 15, scale = 2)
    private BigDecimal totalPenaltyAccrued = BigDecimal.ZERO;

    /**
     * Last date when interest was accrued
     * Used for calculating interest accruals
     */
    @Column(name = "last_accrual_date")
    private LocalDate lastAccrualDate;

    /**
     * Last date when a repayment was made
     */
    @Column(name = "last_repayment_date")
    private LocalDate lastRepaymentDate;

    /**
     * Next payment due date from repayment schedule
     */
    @Column(name = "next_payment_due_date")
    private LocalDate nextPaymentDueDate;

    /**
     * Interest accrued since last accrual or last payment
     */
    @Column(name = "interest_accrued", precision = 15, scale = 2)
    private BigDecimal interestAccrued = BigDecimal.ZERO;

    // ========== END OF NEW COLUMNS ==========

    // === ADD SAFE toString() METHOD ===
    @Override
    public String toString() {
        return "Loan{" +
                "id=" + id +
                ", loanAccountNumber='" + loanAccountNumber + '\'' +
                ", status=" + status +
                ", principalAmount=" + principalAmount +
                ", principalOutstanding=" + principalOutstanding +
                ", outstandingBalance=" + outstandingBalance +
                '}';
    }

    // ========== EXISTING METHODS ==========

    public void calculateLoanTotals() {
        // Calculate total interest using reducing balance method
        BigDecimal monthlyInterestRate = interestRate
                .divide(BigDecimal.valueOf(100), 6, BigDecimal.ROUND_HALF_UP)
                .divide(BigDecimal.valueOf(12), 6, BigDecimal.ROUND_HALF_UP);

        BigDecimal totalInterestDue = calculateTotalInterest(principalAmount, monthlyInterestRate, tenureMonths);
        this.totalDue = principalAmount.add(totalInterestDue);
        this.totalInterestDue = totalInterestDue;
        this.outstandingBalance = totalDue;
        this.principalOutstanding = principalAmount;
        this.interestOutstanding = totalInterestDue;
    }

    private BigDecimal calculateTotalInterest(BigDecimal principal, BigDecimal monthlyRate, int months) {
        BigDecimal balance = principal;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (int i = 0; i < months; i++) {
            BigDecimal monthlyInterest = balance.multiply(monthlyRate)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            totalInterest = totalInterest.add(monthlyInterest);
            BigDecimal monthlyPrincipal = principal.divide(BigDecimal.valueOf(months), 2, BigDecimal.ROUND_HALF_UP);
            balance = balance.subtract(monthlyPrincipal);
        }

        return totalInterest;
    }

    public BigDecimal getTotalInterestDue() {
        if (totalInterestDue != null) {
            return totalInterestDue;
        }
        return totalDue.subtract(principalAmount).max(BigDecimal.ZERO);
    }

    public boolean isOverdue() {
        return daysDelinquent > 0;
    }

    public BigDecimal getCurrentDueAmount() {
        return repaymentSchedules.stream()
                .filter(schedule -> schedule.isOverdue() || schedule.getStatus() == GeneralConfig.InstallmentStatus.PENDING)
                .map(RepaymentSchedule::getOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getNetDisbursementAmount() {
        return principalAmount;
    }

    public BigDecimal getWriteOffAmount() {
        return status == GeneralConfig.LoanStatus.WRITTEN_OFF ? outstandingBalance : BigDecimal.ZERO;
    }

    // Helper method to get loan officer name safely
    public String getLoanOfficerName() {
        if (loanOfficer == null) return null;
        try {
            return loanOfficer.getFirstName() + " " +
                    (loanOfficer.getLastName() != null ? loanOfficer.getLastName() : "");
        } catch (Exception e) {
            return "Unknown Officer";
        }
    }

    // ========== NEW HELPER METHODS ==========

    /**
     * Update all financial tracking fields based on repayment schedules
     * Call this method after any repayment or schedule update
     */
    @PreUpdate
    @PrePersist
    public void updateFinancialTrackingFields() {
        if (repaymentSchedules != null && !repaymentSchedules.isEmpty()) {
            // Calculate totals from repayment schedules
            BigDecimal totalPrincipalPaid = BigDecimal.ZERO;
            BigDecimal totalInterestPaid = BigDecimal.ZERO;
            BigDecimal totalPenaltyPaid = BigDecimal.ZERO;
            BigDecimal totalFeesPaid = BigDecimal.ZERO;

            for (RepaymentSchedule schedule : repaymentSchedules) {
                if (schedule.getPrincipalPaid() != null) {
                    totalPrincipalPaid = totalPrincipalPaid.add(schedule.getPrincipalPaid());
                }
                if (schedule.getInterestPaid() != null) {
                    totalInterestPaid = totalInterestPaid.add(schedule.getInterestPaid());
                }
                if (schedule.getPenaltyAmount() != null && schedule.getPenaltyPaid() != null) {
                    totalPenaltyPaid = totalPenaltyPaid.add(schedule.getPenaltyPaid());
                }
            }
            this.principalPaid = totalPrincipalPaid;
            this.interestPaid = totalInterestPaid;
            this.penaltyPaid = totalPenaltyPaid;
            this.feesPaid = totalFeesPaid;

            // Calculate outstanding amounts
            if (principalAmount != null) {
                this.principalOutstanding = principalAmount.subtract(totalPrincipalPaid);
            }

            if (totalInterestDue != null) {
                this.interestOutstanding = totalInterestDue.subtract(totalInterestPaid);
            }

            // Calculate total paid
            this.totalPaid = totalPrincipalPaid.add(totalInterestPaid)
                    .add(totalPenaltyPaid).add(totalFeesPaid);

            // Calculate outstanding balance (principal + interest + penalties + fees - paid)
            BigDecimal totalOutstanding = (principalOutstanding != null ? principalOutstanding : BigDecimal.ZERO)
                    .add(interestOutstanding != null ? interestOutstanding : BigDecimal.ZERO)
                    .add(penaltyOutstanding != null ? penaltyOutstanding : BigDecimal.ZERO)
                    .add(feesOutstanding != null ? feesOutstanding : BigDecimal.ZERO);

            this.outstandingBalance = totalOutstanding;
        }
    }

    /**
     * Get principal outstanding (used for interest accrual calculations)
     */
    public BigDecimal getPrincipalOutstanding() {
        if (principalOutstanding != null) {
            return principalOutstanding;
        }
        if (principalAmount != null && principalPaid != null) {
            return principalAmount.subtract(principalPaid);
        }
        return principalAmount != null ? principalAmount : BigDecimal.ZERO;
    }

    /**
     * Get interest outstanding (accrued but not paid)
     */
    public BigDecimal getInterestOutstanding() {
        if (interestOutstanding != null) {
            return interestOutstanding;
        }
        if (totalInterestDue != null && interestPaid != null) {
            return totalInterestDue.subtract(interestPaid);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Update next payment due date from repayment schedules
     */
    public void updateNextPaymentDueDate() {
        if (repaymentSchedules != null && !repaymentSchedules.isEmpty()) {
            this.nextPaymentDueDate = repaymentSchedules.stream()
                    .filter(s -> s.getStatus() == GeneralConfig.InstallmentStatus.PENDING
                            || s.getStatus() == GeneralConfig.InstallmentStatus.PARTIAL)
                    .map(RepaymentSchedule::getDueDate)
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Calculate overdue days
     */
    public void calculateDaysDelinquent() {
        if (nextPaymentDueDate != null && nextPaymentDueDate.isBefore(LocalDate.now())) {
            this.daysDelinquent = (int) java.time.temporal.ChronoUnit.DAYS.between(nextPaymentDueDate, LocalDate.now());
        } else {
            this.daysDelinquent = 0;
        }
    }
}