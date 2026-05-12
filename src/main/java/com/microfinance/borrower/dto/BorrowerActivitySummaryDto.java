package com.microfinance.borrower.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class BorrowerActivitySummaryDto {
    private Long borrowerId;
    private String borrowerName;
    private String borrowerLastName;
    private String borrowerFirstName;
    private LocalDate summaryDate;
    private Integer totalActivities;

    // Count fields (Integer)
    private Integer loanActivitiesCount;
    private Integer repaymentActivitiesCount;
    private Integer kycActivitiesCount;
    private Integer savingsActivitiesCount;

    private Integer activitiesThisWeek;
    private Integer activitiesThisMonth;

    // List fields (for detailed activities)
    private List<BorrowerActivityDto> loanActivitiesList;
    private List<BorrowerActivityDto> repaymentActivitiesList;
    private List<BorrowerActivityDto> kycActivitiesList;
    private List<BorrowerActivityDto> savingsActivitiesList;

    private Map<BorrowerActivityDto.ActivityType, Integer> activityCountByType;
    private String mostFrequentActivity;
    private LocalDate lastActivityDate;
    private String lastActivityType;

    // Performance metrics
    private Double onTimeRepaymentRate;
    private Integer meetingsAttended;
    private Integer meetingsMissed;
    private String activityTrend; // INCREASING, DECREASING, STABLE

    private LocalDate startDate;
    private LocalDate endDate;
    private Map<String, Integer> activityCounts;
    private List<BorrowerActivityDto> recentActivities;

    // For backward compatibility - these will be deprecated but kept for existing code
    @Deprecated
    private Integer loanActivities;
    @Deprecated
    private Integer repaymentActivities;
    @Deprecated
    private Integer kycActivities;
    @Deprecated
    private Integer savingsActivities;
}