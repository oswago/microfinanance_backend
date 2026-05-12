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
public class RevenueSection {
    private List<AccountBalance> operatingRevenues;
    private BigDecimal totalOperatingRevenue;
    private List<AccountBalance> otherRevenues;
    private BigDecimal totalOtherRevenue;
    private BigDecimal totalRevenue;
}