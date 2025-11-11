package com.microfinance.borrower.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class GroupPerformanceDto {
    private Long groupId;
    private String groupName;
    private String groupCode;
    
    // Member Statistics
    private Integer totalMembers;
    private Integer activeMembers;
    private Integer newMembersThisMonth;
    private Integer membersLeftThisMonth;
    
    // Loan Performance
    private Integer totalLoansTaken;
    private Integer activeLoans;
    private Integer completedLoans;
    private Integer defaultedLoans;
    private Integer overdueLoans;
    
    // Financial Metrics
    private BigDecimal totalLoanAmount;
    private BigDecimal totalAmountDisbursed;
    private BigDecimal totalAmountRepaid;
    private BigDecimal totalOutstandingBalance;
    private BigDecimal totalOverdueAmount;
    
    // Performance Ratios
    private BigDecimal repaymentRate; // Percentage
    private BigDecimal onTimeRepaymentRate;
    private BigDecimal defaultRate;
    private BigDecimal portfolioAtRisk; // PAR > 30 days
    
    // Savings Performance
    private BigDecimal totalSavingsBalance;
    private BigDecimal averageSavingsPerMember;
    private BigDecimal totalSavingsDeposits;
    private BigDecimal totalSavingsWithdrawals;
    
    // Meeting Performance
    private Integer totalMeetingsHeld;
    private Integer meetingsThisMonth;
    private BigDecimal averageAttendanceRate; // Percentage
    private Integer consecutiveMeetingsMissed;
    
    // Risk Assessment
    private String riskRating; // LOW, MEDIUM, HIGH, VERY_HIGH
    private String performanceGrade; // A, B, C, D, F
    private Boolean isEligibleForNewLoans;
    
    // Timeline Metrics
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate formationDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastMeetingDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastLoanDisbursementDate;
    
    // Trend Analysis
    private String loanGrowthTrend; // INCREASING, DECREASING, STABLE
    private String savingsGrowthTrend;
    private String repaymentTrend;
    
    // Comparative Metrics
    private Integer groupRankInBranch;
    private Integer groupRankInSystem;
    private BigDecimal performanceScore; // 0-100
    
    // Detailed Breakdowns
    private Map<String, Integer> loanStatusBreakdown;
    private Map<String, BigDecimal> loanProductDistribution;
    private Map<String, Integer> memberStatusBreakdown;
    
    // Recommendations
    private String recommendations;
    private String areasForImprovement;
    private String strengths;

    // Helper methods
    public BigDecimal getRepaymentRatePercentage() {
        return repaymentRate != null ? repaymentRate.multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
    }
    
    public BigDecimal getPortfolioAtRiskPercentage() {
        return portfolioAtRisk != null ? portfolioAtRisk.multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
    }
    
    public BigDecimal getAverageLoanSize() {
        return totalLoansTaken > 0 ? 
            totalLoanAmount.divide(BigDecimal.valueOf(totalLoansTaken), 2, BigDecimal.ROUND_HALF_UP) : 
            BigDecimal.ZERO;
    }
    
    public Integer getSuccessfulLoansCount() {
        return completedLoans != null ? completedLoans : 0;
    }
    
    public BigDecimal getSuccessRate() {
        return totalLoansTaken > 0 ? 
            BigDecimal.valueOf(completedLoans)
                .divide(BigDecimal.valueOf(totalLoansTaken), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100)) : 
            BigDecimal.ZERO;
    }
}