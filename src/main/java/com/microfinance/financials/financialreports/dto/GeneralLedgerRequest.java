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
public class GeneralLedgerRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long accountId;
    private String accountCode;
    private String journalNumber;
}
