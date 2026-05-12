package com.microfinance.loanapplications.entity;

import com.microfinance.base.utils.GeneralConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_repayment_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRepaymentStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false, unique = true)
    private Loan loan;

    // Current status summary
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GeneralConfig.RepaymentStatus overallStatus;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDue;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPaid;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalArrears;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal penaltyAccrued;

    // Installment tracking
    private Integer totalInstallments;
    private Integer paidInstallments;
    private Integer pendingInstallments;
    private Integer overdueInstallments;

    // Dates
    private LocalDate lastPaymentDate;
    private LocalDate nextDueDate;
    private LocalDate lastOverdueDate;
    private LocalDate expectedMaturityDate;
    private LocalDate actualMaturityDate;

    // Delinquency tracking
    private Integer daysDelinquent;
    private Integer maxDaysDelinquent;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GeneralConfig.DelinquencyBucket delinquencyBucket; // 0-30, 31-60, 61-90, 90+

    // Payment performance
    private BigDecimal averagePaymentAmount;
    private BigDecimal largestPaymentAmount;
    private LocalDate largestPaymentDate;

    private Integer onTimePayments;
    private Integer latePayments;
    private Integer missedPayments;

    @Column(precision = 5, scale = 2)
    private BigDecimal onTimePaymentRate; // Percentage

    @Column(precision = 5, scale = 2)
    private BigDecimal collectionRate; // (Total Paid / Total Due) * 100

    // Flags
    private Boolean isDelinquent;
    private Boolean isNpa; // Non-Performing Asset
    private Boolean isWrittenOff;
    private Boolean isRestructured;
    private Boolean isRescheduled;

    // Timestamps
    private LocalDateTime lastCalculatedAt;
    private LocalDateTime updatedAt;

    // Next payment info
    private BigDecimal nextPaymentAmount;
    private Integer nextInstallmentNumber;

    // Risk indicators
    @Column(precision = 5, scale = 2)
    private BigDecimal probabilityOfDefault;

    @Column(length = 500)
    private String riskFactors;

    // Audit fields
    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Long createdBy;
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public boolean isCurrent() {
        return !isDelinquent && outstandingBalance != null && outstandingBalance.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isFullyPaid() {
        return outstandingBalance != null && outstandingBalance.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean hasArrears() {
        return totalArrears != null && totalArrears.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getPaymentProgressPercentage() {
        if (totalDue != null && totalDue.compareTo(BigDecimal.ZERO) > 0) {
            return totalPaid != null ?
                    totalPaid.multiply(BigDecimal.valueOf(100))
                            .divide(totalDue, 2, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getInstallmentProgressPercentage() {
        if (totalInstallments != null && totalInstallments > 0) {
            return BigDecimal.valueOf(paidInstallments != null ? paidInstallments : 0)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalInstallments), 2, BigDecimal.ROUND_HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public void calculateCollectionRate() {
        if (totalDue != null && totalDue.compareTo(BigDecimal.ZERO) > 0) {
            this.collectionRate = totalPaid != null ?
                    totalPaid.multiply(BigDecimal.valueOf(100))
                            .divide(totalDue, 2, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;
        } else {
            this.collectionRate = BigDecimal.ZERO;
        }
    }

    public void calculateOnTimePaymentRate() {
        int totalPayments = (onTimePayments != null ? onTimePayments : 0) +
                (latePayments != null ? latePayments : 0) +
                (missedPayments != null ? missedPayments : 0);

        if (totalPayments > 0) {
            this.onTimePaymentRate = BigDecimal.valueOf(onTimePayments != null ? onTimePayments : 0)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalPayments), 2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.onTimePaymentRate = BigDecimal.ZERO;
        }
    }

    public void updateDelinquencyBucket() {
        if (daysDelinquent == null || daysDelinquent <= 0) {
            this.delinquencyBucket = GeneralConfig.DelinquencyBucket.CURRENT;
            this.isDelinquent = false;
        } else if (daysDelinquent <= 30) {
            this.delinquencyBucket = GeneralConfig.DelinquencyBucket.DAYS_1_30;
            this.isDelinquent = true;
        } else if (daysDelinquent <= 60) {
            this.delinquencyBucket = GeneralConfig.DelinquencyBucket.DAYS_31_60;
            this.isDelinquent = true;
        } else if (daysDelinquent <= 90) {
            this.delinquencyBucket = GeneralConfig.DelinquencyBucket.DAYS_61_90;
            this.isDelinquent = true;
        } else {
            this.delinquencyBucket = GeneralConfig.DelinquencyBucket.DAYS_90_PLUS;
            this.isDelinquent = true;
            this.isNpa = daysDelinquent >= 90; // NPA if 90+ days delinquent
        }
    }
}