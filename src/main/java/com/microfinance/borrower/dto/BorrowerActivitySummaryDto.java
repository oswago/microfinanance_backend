package com.microfinance.borrower.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class BorrowerActivitySummaryDto {
    private Long borrowerId;
    private String borrowerName;
    private LocalDate summaryDate;
    private Integer totalActivities;
    private Integer loanActivities;
    private Integer repaymentActivities;
    private Integer kycActivities;
    private Integer savingsActivities;
    private Map<BorrowerActivityDto.ActivityType, Integer> activityCountByType;
    private String mostFrequentActivity;
    private LocalDate lastActivityDate;
    private String lastActivityType;
    
    // Performance metrics
    private Double onTimeRepaymentRate;
    private Integer meetingsAttended;
    private Integer meetingsMissed;
    private String activityTrend; // INCREASING, DECREASING, STABLE
}