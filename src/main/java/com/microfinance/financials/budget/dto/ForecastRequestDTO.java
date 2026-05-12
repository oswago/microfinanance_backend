// dto/ForecastRequestDTO.java
package com.microfinance.financials.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastRequestDTO {
    private String category;
    private LocalDate startDate;
    private LocalDate endDate;
    private String forecastMethod;
    private Integer forecastPeriods;
    private List<HistoricalDataPoint> historicalData;
}