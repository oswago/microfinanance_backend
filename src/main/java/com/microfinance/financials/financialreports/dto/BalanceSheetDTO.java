package com.microfinance.financials.financialreports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// Balance Sheet DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSheetDTO {
    private ReportHeader header;
    private AssetSection assets;
    private LiabilitySection liabilities;
    private EquitySection equity;
    private LocalDate asOfDate;
}