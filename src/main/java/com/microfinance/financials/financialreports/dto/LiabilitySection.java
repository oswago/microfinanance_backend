package com.microfinance.financials.financialreports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiabilitySection {
    private List<AccountBalance> currentLiabilities;
    private BigDecimal totalCurrentLiabilities;
    private List<AccountBalance> longTermLiabilities;
    private BigDecimal totalLongTermLiabilities;
    private BigDecimal totalLiabilities;
}
