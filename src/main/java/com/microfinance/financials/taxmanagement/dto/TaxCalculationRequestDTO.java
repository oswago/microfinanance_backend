// dto/TaxCalculationRequestDTO.java
package com.microfinance.financials.taxmanagement.dto;

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
public class TaxCalculationRequestDTO {
    private String taxCode;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private Long referenceId;
    private String referenceType;
}
