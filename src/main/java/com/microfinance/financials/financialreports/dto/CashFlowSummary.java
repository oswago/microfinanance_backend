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
public class CashFlowSummary {
    private BigDecimal netCashIncrease;
    private BigDecimal beginningCashBalance;
    private BigDecimal endingCashBalance;
}