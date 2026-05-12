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
public class BalanceSheetRequest {
    private LocalDate asOfDate;
    private Long financialPeriodId;
    private Boolean showComparative;
}
