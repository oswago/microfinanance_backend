package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.loanproducts.entity.LoanProduct;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Data
@EqualsAndHashCode(callSuper = true)
public class Loan extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id")
    private LoanProduct loanProduct;
    
    private BigDecimal principalAmount;
    private BigDecimal outstandingBalance;
    
    @Enumerated(EnumType.STRING)
    private LoanStatus status;
    
    private LocalDateTime applicationDate;
    private LocalDateTime approvalDate;
    private LocalDateTime disbursementDate;
    
    public enum LoanStatus {
        DRAFT,
        PENDING,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        ACTIVE,
        CLOSED,
        DEFAULTED,
        WRITTEN_OFF
    }
}