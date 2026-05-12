// dto/ForecastResultDTO.java
package com.microfinance.financials.budget.dto;

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
public class ForecastResultDTO {
    private List<ForecastDataPoint> forecastedValues;
    private BigDecimal totalForecast;
    private BigDecimal averageForecast;
    private BigDecimal growthRate;
    private BigDecimal confidenceLevel;
    private String method;
    private String notes;
}