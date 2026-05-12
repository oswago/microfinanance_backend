package com.microfinance.loanapplications.dto.rescheduling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleStatisticsDto {
    
    private Long pendingRequests;
    private Long underReview;
    private Long approvedRequests;
    private Long rejectedRequests;
    private Long totalRequests;
    private Long requestsThisMonth;
    private Long requestsLastMonth;
    private Double averageProcessingTime;
    private Double approvalRate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    
    // You can also add a static factory method if needed
    public static RescheduleStatisticsDtoBuilder builder() {
        return new RescheduleStatisticsDtoBuilder();
    }
}