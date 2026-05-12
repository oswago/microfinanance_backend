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
public class AssetSection {
    private List<AccountBalance> currentAssets;
    private BigDecimal totalCurrentAssets;
    private List<AccountBalance> fixedAssets;
    private BigDecimal totalFixedAssets;
    private List<AccountBalance> otherAssets;
    private BigDecimal totalOtherAssets;
    private BigDecimal totalAssets;
}