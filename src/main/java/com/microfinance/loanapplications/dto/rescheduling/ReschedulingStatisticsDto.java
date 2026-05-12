// ReschedulingStatisticsDto.java
package com.microfinance.loanapplications.dto.rescheduling;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReschedulingStatisticsDto {
    private long pendingRequests;
    private long approvedRequests;
    private long rejectedRequests;
    private long underReview;
    private long totalRequests;
    private double averageProcessingTime;
    private long requestsThisMonth;
    private long requestsLastMonth;
}