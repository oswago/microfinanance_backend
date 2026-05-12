package com.microfinance.financials.taxmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxByTypeDTO {
    private String taxType;
    private String taxCode;
    private String taxName;
    private BigDecimal taxableAmount;
    private BigDecimal taxAmount;
    private BigDecimal withheldAmount;
    private int transactionCount;
}
