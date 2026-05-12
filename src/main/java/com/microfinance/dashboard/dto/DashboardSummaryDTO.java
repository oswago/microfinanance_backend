// dto/DashboardSummaryDTO.java
package com.microfinance.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import com.microfinance.dashboard.dto.*;

/**
 * Complete dashboard summary DTO
 * Aggregates all dashboard data for the main dashboard view
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {
    
    /**
     * Loan statistics summary
     */
    private LoanStatsDTO stats;
    
    /**
     * List of recent loan activities
     */
    private List<RecentActivityDto> recentActivities;
    
    /**
     * List of upcoming payments
     * Each payment contains: id, loanId, loanAccountNumber, borrowerName, dueDate, amountDue, installmentNumber
     */
    private List<Map<String, Object>> upcomingPayments;
    
    /**
     * List of top borrowers
     * Each borrower contains: borrowerId, borrowerName, totalBorrowed, activeLoans
     */
    private List<Map<String, Object>> topBorrowers;
    
    /**
     * Optional: Portfolio distribution by product type
     */
    private PortfolioDistributionDto portfolioDistribution;
    
    /**
     * Optional: Repayment performance over time
     */
    private RepaymentPerformanceDto repaymentPerformance;
    
    /**
     * Optional: System alerts
     */
    private List<SystemAlertDto> systemAlerts;
    
    /**
     * Optional: Pending tasks for the current user
     */
    private List<PendingTaskDto> pendingTasks;
    
    /**
     * Helper method to check if dashboard has any data
     * @return true if stats are present
     */
    public boolean hasData() {
        return stats != null;
    }
    
    /**
     * Helper method to get total number of upcoming payments
     * @return size of upcoming payments list or 0 if null
     */
    public int getUpcomingPaymentsCount() {
        return upcomingPayments != null ? upcomingPayments.size() : 0;
    }
    
    /**
     * Helper method to get total number of recent activities
     * @return size of recent activities list or 0 if null
     */
    public int getRecentActivitiesCount() {
        return recentActivities != null ? recentActivities.size() : 0;
    }
    
    /**
     * Helper method to get total number of top borrowers
     * @return size of top borrowers list or 0 if null
     */
    public int getTopBorrowersCount() {
        return topBorrowers != null ? topBorrowers.size() : 0;
    }
}