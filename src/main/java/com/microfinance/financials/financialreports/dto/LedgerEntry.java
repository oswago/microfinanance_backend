package com.microfinance.financials.financialreports.dto;

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
public class LedgerEntry {
    private LocalDate transactionDate;
    private String journalNumber;
    private String accountCode;
    private String accountName;
    private String description;
    private String debitCredit;
    private BigDecimal amount;
    private String referenceType;
    private String referenceNumber;
}
