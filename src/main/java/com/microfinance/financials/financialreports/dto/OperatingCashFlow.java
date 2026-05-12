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
public class OperatingCashFlow {
    private BigDecimal netIncome;
    private List<CashFlowAdjustment> adjustments;
    private BigDecimal netCashFromOperations;
    private List<CashFlowItem> changesInWorkingCapital;
    private BigDecimal netCashProvided;
}
