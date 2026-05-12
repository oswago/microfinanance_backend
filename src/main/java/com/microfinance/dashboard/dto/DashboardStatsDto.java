// dto/dashboard/DashboardStatsDto.java
package com.microfinance.dashboard.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DashboardStatsDto {
    private Integer totalUsers;
    private Integer totalBorrowers;
    private Integer activeLoans;
    private BigDecimal totalPortfolio;
    private Integer pendingApplications;
    private Integer highRiskLoans;
    private Double userGrowth;
    private Double borrowerGrowth;
    private Double loanGrowth;
    private Double portfolioGrowth;
}