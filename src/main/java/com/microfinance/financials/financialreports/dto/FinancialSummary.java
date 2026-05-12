package com.microfinance.financials.financialreports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialSummary {
    private BigDecimal grossProfit;
    private BigDecimal operatingIncome;
    private BigDecimal netIncomeBeforeTax;
    private BigDecimal taxExpense;
    private BigDecimal netIncomeAfterTax;
}