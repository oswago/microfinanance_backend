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
public class CollectionActionStatsDto {
    private Integer overdueInstallments;
    private Integer todaysCalls;
    private BigDecimal amountCollectedToday;
    private Integer activeAgents;
}