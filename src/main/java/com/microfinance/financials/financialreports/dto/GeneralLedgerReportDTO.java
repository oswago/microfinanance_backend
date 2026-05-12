package com.microfinance.financials.financialreports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// General Ledger Report DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralLedgerReportDTO {
    private ReportHeader header;
    private List<LedgerEntry> entries;
    private Map<String, AccountSummary> accountSummaries;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private LocalDate startDate;
    private LocalDate endDate;
}
