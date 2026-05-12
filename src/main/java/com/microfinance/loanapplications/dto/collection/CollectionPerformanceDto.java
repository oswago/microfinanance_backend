package com.microfinance.loanapplications.dto.collection;

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
public class CollectionPerformanceDto {
    // Summary statistics
    private BigDecimal collectionRate;
    private Integer averageRecoveryTime;
    private BigDecimal successRate;
    private BigDecimal totalCollectedAmount;
    private Integer averageResponseTime;
    private BigDecimal improvementTrend;
    
    // Officer performance
    private List<OfficerPerformanceDto> officerPerformance;
    
    // Daily trends
    private List<DailyCollectionTrendDto> dailyCollections;
    
    // Additional metrics
    private BigDecimal portfolioAtRisk;
    private Integer totalOverdue;
    private BigDecimal overdueAmount;
}