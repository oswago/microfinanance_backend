package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionReportDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long branchId;
    private String branchName;
    private Long loanOfficerId;
    private String loanOfficerName;

    // Summary stats
    private Integer totalOverdue;
    private BigDecimal totalOverdueAmount;
    private Integer totalCollected;
    private BigDecimal totalCollectedAmount;
    private BigDecimal collectionRate;

    // Breakdowns
    private List<DailyCollectionDto> dailyCollections;
    private List<OfficerPerformanceDto> officerPerformance;
    private List<AgingBreakdownDto> agingBreakdown;

    // Actions summary
    private Integer totalCalls;
    private Integer totalVisits;
    private Integer totalFollowUps;
    private Integer promisesToPay;


}
