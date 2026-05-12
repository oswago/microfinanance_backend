package com.microfinance.financials.financialreports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// Common DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportHeader {
    private String companyName;
    private String reportName;
    private String reportPeriod;
    private LocalDate generatedDate;
    private String currency;
}
