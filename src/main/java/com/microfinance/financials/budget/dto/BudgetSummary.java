package com.microfinance.financials.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetSummary {
    private BigDecimal totalBudget;
    private BigDecimal totalActual;
    private BigDecimal totalVariance;
    private BigDecimal overallVariancePercentage;
    private int categoriesOnTrack;
    private int categoriesBelowTarget;
    private int categoriesAboveTarget;
}
