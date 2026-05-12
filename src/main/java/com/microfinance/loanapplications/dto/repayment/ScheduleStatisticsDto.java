package com.microfinance.loanapplications.dto.repayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// ScheduleStatisticsDto.java
public class ScheduleStatisticsDto {
    private Long activeSchedules;
    private BigDecimal totalAmountDue;
    private Long upcomingPayments;
    private Long overduePayments;
    private BigDecimal totalCollected;
    private BigDecimal collectionRate;
}