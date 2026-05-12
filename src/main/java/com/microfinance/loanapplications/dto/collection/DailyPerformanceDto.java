package com.microfinance.loanapplications.dto.collection;

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
public class DailyPerformanceDto {
    private LocalDate date;
    private BigDecimal amountCollected;
    private Integer callsMade;
    private Integer visitsMade;
    private Integer loansResolved;
}