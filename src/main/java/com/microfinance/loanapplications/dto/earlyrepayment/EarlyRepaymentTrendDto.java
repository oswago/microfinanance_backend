package com.microfinance.loanapplications.dto.earlyrepayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarlyRepaymentTrendDto {
    private String period;
    private Integer count;
    private BigDecimal amount;
    private BigDecimal interestSaved;
}