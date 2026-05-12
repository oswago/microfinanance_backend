package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskBreakdownDto {
    private Long low;
    private Long medium;
    private Long high;
    private Long critical;
    private BigDecimal lowAmount;
    private BigDecimal mediumAmount;
    private BigDecimal highAmount;
    private BigDecimal criticalAmount;
}
