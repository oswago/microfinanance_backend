// dto/LoanStatsDTO.java
package com.microfinance.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for loan statistics summary
 * Used for dashboard loan metrics display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanStatsDTO {
    
    /**
     * Number of loan applications pending review
     */
    private int pendingApplications;


    private int highRiskLoans;

    /**
     * Number of draft loan applications
     */
    private int draftApplications;
    
    /**
     * Number of applications pending approval
     */
    private int pendingApproval;
    
    /**
     * Number of applications approved today
     */
    private int approvedToday;
    
    /**
     * Number of loans pending disbursement
     */
    private int pendingDisbursement;
    
    /**
     * Number of loans disbursed today
     */
    private int disbursedToday;
    
    /**
     * Number of payments due today
     */
    private int dueToday;
    
    /**
     * Number of overdue payments
     */
    private int overdue;
    
    /**
     * Number of pending reschedule requests
     */
    private int pendingReschedule;
    
    /**
     * Number of loans rescheduled this month
     */
    private int rescheduledThisMonth;
    
    /**
     * Number of loans overdue by 30+ days
     */
    private int overdue30Days;
    
    /**
     * Total amount collected today
     */
    private BigDecimal collectedToday;
    
    /**
     * Helper method to calculate collection rate
     * @return collection rate as percentage
     */
    public double getCollectionRate() {
        if (dueToday == 0) return 100.0;
        return (collectedToday != null ? collectedToday.doubleValue() : 0) / dueToday * 100;
    }
    
    /**
     * Helper method to check if there are pending approvals
     * @return true if there are pending approvals
     */
    public boolean hasPendingApprovals() {
        return pendingApproval > 0;
    }
    
    /**
     * Helper method to check if there are overdue payments
     * @return true if there are overdue payments
     */
    public boolean hasOverduePayments() {
        return overdue > 0;
    }
    
    /**
     * Helper method to get total active tasks
     * @return sum of pending applications, pending approvals, and pending disbursements
     */
    public int getTotalActiveTasks() {
        return pendingApplications + pendingApproval + pendingDisbursement;
    }
}