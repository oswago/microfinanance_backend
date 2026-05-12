// dto/ForecastDTO.java
package com.microfinance.financials.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastDTO {
    private Long id;
    private String forecastCode;
    private String forecastName;
    private LocalDate forecastDate;
    private LocalDate forecastPeriodStart;
    private LocalDate forecastPeriodEnd;
    private String category;
    private String forecastMethod;
    private BigDecimal predictedAmount;
    private BigDecimal confidenceLower;
    private BigDecimal confidenceUpper;
    private BigDecimal confidenceLevel;
    private BigDecimal actualAmount;
    private BigDecimal forecastAccuracy;
    private String assumptions;
    private String notes;
    private LocalDateTime createdAt;
}