// entity/BudgetActual.java
package com.microfinance.financials.budget.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fin_budget_actuals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetActual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "budget_id", nullable = false)
    private Long budgetId;

    @Column(name = "period_date", nullable = false)
    private LocalDate periodDate;

    @Column(name = "actual_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "variance_amount", precision = 15, scale = 2)
    private BigDecimal varianceAmount;

    @Column(name = "variance_percentage", precision = 5, scale = 2)
    private BigDecimal variancePercentage;

    @Column(length = 50)
    private String status; // ON_TRACK, BELOW_TARGET, ABOVE_TARGET

    @Column(columnDefinition = "TEXT")
    private String notes;
}