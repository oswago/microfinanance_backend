package com.microfinance.financials.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastDataPoint {
    private String period;
    private LocalDate date;
    private BigDecimal forecast;
    private BigDecimal lowerBound;
    private BigDecimal upperBound;
}
