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
public class AgingBreakdownDto {
    private String bucket; // 1-7 days, 8-30 days, 31-90 days, 90+ days
    private Integer count;
    private BigDecimal amount;
    private BigDecimal percentage;
}