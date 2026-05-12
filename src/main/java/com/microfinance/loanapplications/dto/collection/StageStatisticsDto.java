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
public class StageStatisticsDto {
    private String stage;
    private Integer count;
    private BigDecimal amount;
    private BigDecimal recoveryRate;


    public double getRecoveryRateValue() {
        return recoveryRate != null ? recoveryRate.doubleValue() : 0.0;
    }
}