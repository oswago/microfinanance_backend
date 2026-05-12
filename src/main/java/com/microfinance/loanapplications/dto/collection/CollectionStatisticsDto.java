package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionStatisticsDto {
    private Long totalOverdue;
    private BigDecimal overdueAmount;
    private BigDecimal portfolioAtRisk;
    private Integer activeOfficers;
    private BigDecimal collectionRate;
    private BigDecimal responseRate;
    private RiskBreakdownDto riskBreakdown;
    private StageBreakdownDto stageBreakdown;
}











