package com.microfinance.financials.financialreports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// Cash Flow Statement DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowStatementDTO {
    private ReportHeader header;
    private OperatingCashFlow operatingCashFlow;
    private InvestingCashFlow investingCashFlow;
    private FinancingCashFlow financingCashFlow;
    private CashFlowSummary summary;
    private LocalDate startDate;
    private LocalDate endDate;
}
