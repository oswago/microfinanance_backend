package com.microfinance.financials.financialreports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSection {
    private List<AccountBalance> operatingExpenses;
    private BigDecimal totalOperatingExpense;
    private List<AccountBalance> administrativeExpenses;
    private BigDecimal totalAdministrativeExpense;
    private List<AccountBalance> sellingExpenses;
    private BigDecimal totalSellingExpense;
    private BigDecimal totalExpenses;
}