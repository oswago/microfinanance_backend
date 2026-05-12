package com.microfinance.financials.financialreports.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// Report Request DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate asOfDate;
    private Long financialPeriodId;
    private Boolean includeZeroBalances;
    private String accountType;
    private String[] accountCategories;
}
