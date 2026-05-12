package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.base.entity.User;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.common.config.GeneralConfig;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "early_repayment_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarlyRepaymentRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingPrincipal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal accruedInterest;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal penaltyCharges;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPayable;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discountPercentage;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal earlyRepaymentAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInterestIfNormal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInterestIfEarly;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal interestSavings;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestSavingsPercentage;

    @Column(nullable = false)
    private Integer originalTenure;

    @Column(nullable = false)
    private Integer remainingTenure;

    @Column(nullable = false)
    private LocalDate requestedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id")
    private User requestedBy;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GeneralConfig.PaymentMethod preferredPaymentMethod;

    @Column(nullable = false)
    private LocalDate targetSettlementDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GeneralConfig.EarlyRepaymentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    private LocalDate approvalDate;

    @Column(columnDefinition = "TEXT")
    private String approvalComments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by_id")
    private User rejectedBy;

    private LocalDate rejectionDate;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    // Settlement details
    private LocalDate settlementDate;
    private String settlementReference;
    private String settlementLetterPath;

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (requestNumber == null) {
            requestNumber = generateRequestNumber();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateRequestNumber() {
        String prefix = "EARLY-";
        String date = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return prefix + date + "-" + System.currentTimeMillis() % 10000;
    }
}