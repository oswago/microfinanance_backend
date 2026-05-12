package com.microfinance.financials.financialreports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeStatementRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long financialPeriodId;
    private Boolean includeDetails;
}
