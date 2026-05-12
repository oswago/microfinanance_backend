package com.microfinance.borrower.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.loanproducts.entity.LoanProduct;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Entity
@Table(name = "borrower_product_preferences")
@Data
@EqualsAndHashCode(callSuper = true)
public class BorrowerProductPreference extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;
    
    private Integer preferenceScore; // 1-10 scale
    private Boolean isActive = true;
    
    // Historical data
    private Integer timesApplied;
    private Integer timesApproved;
    private BigDecimal totalBorrowedAmount;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    // Preference reasons
    private Boolean preferredForInterestRate;
    private Boolean preferredForTenure;
    private Boolean preferredForFees;
    private Boolean recommendedByOfficer;
}