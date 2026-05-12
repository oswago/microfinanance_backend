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
public class MonthlyComparison {
    private String month;
    private BigDecimal budget;
    private BigDecimal actual;
    private BigDecimal variance;
    private BigDecimal variancePercentage;
}
