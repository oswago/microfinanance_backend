package com.microfinance.loanapplications.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for application statistics request parameters
 */
@Data
public class ApplicationStatsRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long branchId;
    private Long officerId;
    private Long productId;
    private String periodType; // DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
    private Boolean includeTrends;
    private Boolean includeOfficerPerformance;
    private Boolean includeProductStats;
    
    // Validation methods
    public boolean isValid() {
        return startDate != null && endDate != null && !startDate.isAfter(endDate);
    }
    
    public int getPeriodInDays() {
        if (startDate == null || endDate == null) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
}