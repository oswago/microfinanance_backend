package com.microfinance.financials.taxmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxSummary {
    private BigDecimal totalTaxableAmount;
    private BigDecimal totalTaxAmount;
    private BigDecimal totalWithheldAmount;
    private BigDecimal totalRemittedAmount;
    private BigDecimal outstandingAmount;
}
