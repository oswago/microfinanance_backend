package com.microfinance.financials.provisions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvisionCalculationRequestDTO {
    private LocalDate asOfDate;
    private Long loanId;
    private Boolean includeAllLoans;
}
