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
public class OfficerPerformanceDetailDto {
    private Long officerId;
    private String officerName;
    private Integer assignedLoans;
    private Integer resolvedLoans;
    private BigDecimal resolvedAmount;
    private BigDecimal collectionRate;
    private Integer avgRecoveryDays;
    private Integer callsMade;
    private Integer visitsMade;
    private BigDecimal successRate;
    private List<ActivityDto> recentActivities;
    private List<DailyPerformanceDto> dailyPerformance;
}