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
public class PerformanceSummaryDto {
    private BigDecimal totalCollected;
    private Integer totalTransactions;
    private BigDecimal collectionRate;
    private Integer averageRecoveryDays;
    private BigDecimal successRate;
    private Integer totalOverdue;
    private BigDecimal overdueAmount;
    private Integer activeOfficers;
    private BigDecimal improvementTrend;
    private BigDecimal averageResponseTime;
}