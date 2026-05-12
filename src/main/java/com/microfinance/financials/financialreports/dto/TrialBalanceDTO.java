// dto/TrialBalanceDTO.java
package com.microfinance.financials.financialreports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrialBalanceDTO {
    private ReportHeader header;
    private List<TrialBalanceEntry> entries;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private Boolean isBalanced;
    private LocalDate asOfDate;
}

