package com.microfinance.financials.provisions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvisionSummaryDTO {
    private String agingBucket;
    private Integer loanCount;
    private BigDecimal totalOutstanding;
    private BigDecimal provisionRate;
    private BigDecimal provisionAmount;
    private BigDecimal existingProvision;
    private BigDecimal adjustment;
}
