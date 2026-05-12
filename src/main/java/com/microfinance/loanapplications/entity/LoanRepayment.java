package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.base.entity.User;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.common.config.GeneralConfig;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "loan_repayments")
@Data
@EqualsAndHashCode(callSuper = true)
public class LoanRepayment extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = true)
    private Borrower borrower;
    
    @Column(nullable = false, unique = true, length = 20)
    private String receiptNumber;
    
    @Column(nullable = false)
    private LocalDate paymentDate;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal interestAmount;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal penaltyAmount = BigDecimal.ZERO;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal feesAmount = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GeneralConfig.PaymentMethod paymentMethod;
    
    @Column(length = 500)
    private String notes;
    
    @Column(length = 50)
    private String transactionReference;

    @Column(name = "is_on_time")
    private Boolean isOnTime = true;


    // ✅ ADD THIS RELATIONSHIP
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installment_id")
    private RepaymentSchedule installment;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private User receivedBy;
    
    @ManyToMany
    @JoinTable(
        name = "repayment_installment_allocation",
        joinColumns = @JoinColumn(name = "repayment_id"),
        inverseJoinColumns = @JoinColumn(name = "schedule_id")
    )
    private List<RepaymentSchedule> allocatedInstallments = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GeneralConfig.RepaymentStatus status = GeneralConfig.RepaymentStatus.COMPLETED;
    
    // For reversals
    private Boolean isReversed = false;
    private LocalDateTime reversedAt;
    private String reversalReason;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversed_by")
    private User reversedBy;
    
    // Enhanced methods
    public void generateReceiptNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));
        this.receiptNumber = "RCP" + timestamp.substring(timestamp.length() - 8) + random;
    }
    
    public BigDecimal getTotalAmount() {
        return principalAmount.add(interestAmount).add(penaltyAmount).add(feesAmount);
    }
    
    public boolean isValidForReversal() {
        return !isReversed && status == GeneralConfig.RepaymentStatus.COMPLETED;
    }
    
    public void reverseRepayment(String reason, User reversedByUser) {
        if (!isValidForReversal()) {
            throw new IllegalStateException("Repayment cannot be reversed");
        }
        
        this.isReversed = true;
        this.reversedAt = LocalDateTime.now();
        this.reversalReason = reason;
        this.reversedBy = reversedByUser;
        this.status = GeneralConfig.RepaymentStatus.REVERSED;
    }
    
    public void allocateToInstallments(List<RepaymentSchedule> installments) {
        this.allocatedInstallments.clear();
        this.allocatedInstallments.addAll(installments);
    }


}