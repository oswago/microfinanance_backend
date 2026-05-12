// entity/Budget.java
package com.microfinance.financials.budget.entity;

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
@Table(name = "fin_budgets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "budget_code", nullable = false, unique = true, length = 50)
    private String budgetCode;

    @Column(name = "budget_name", nullable = false, length = 100)
    private String budgetName;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "period_type", nullable = false, length = 20)
    private String periodType; // MONTHLY, QUARTERLY, ANNUAL

    @Column(name = "category", nullable = false, length = 50)
    private String category; // DISBURSEMENTS, COLLECTIONS, INCOME, EXPENSES

    @Column(name = "sub_category", length = 100)
    private String subCategory;

    @Column(name = "account_code", length = 50)
    private String accountCode;

    @Column(name = "account_name", length = 100)
    private String accountName;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "january", precision = 15, scale = 2)
    private BigDecimal january;

    @Column(name = "february", precision = 15, scale = 2)
    private BigDecimal february;

    @Column(name = "march", precision = 15, scale = 2)
    private BigDecimal march;

    @Column(name = "april", precision = 15, scale = 2)
    private BigDecimal april;

    @Column(name = "may", precision = 15, scale = 2)
    private BigDecimal may;

    @Column(name = "june", precision = 15, scale = 2)
    private BigDecimal june;

    @Column(name = "july", precision = 15, scale = 2)
    private BigDecimal july;

    @Column(name = "august", precision = 15, scale = 2)
    private BigDecimal august;

    @Column(name = "september", precision = 15, scale = 2)
    private BigDecimal september;

    @Column(name = "october", precision = 15, scale = 2)
    private BigDecimal october;

    @Column(name = "november", precision = 15, scale = 2)
    private BigDecimal november;

    @Column(name = "december", precision = 15, scale = 2)
    private BigDecimal december;

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}


