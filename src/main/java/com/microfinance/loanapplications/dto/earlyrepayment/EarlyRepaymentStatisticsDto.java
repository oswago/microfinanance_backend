package com.microfinance.loanapplications.dto.earlyrepayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarlyRepaymentStatisticsDto {
    private BigDecimal totalEarlyRepayments;
    private BigDecimal totalInterestSaved;
    private Integer activeRequests;
    private BigDecimal averageDiscount;
    private Integer approvedCount;
    private Integer rejectedCount;
    private Integer pendingCount;
    private Integer paidCount;
    private BigDecimal averageProcessingTime;
    private List<EarlyRepaymentTrendDto> trends;
    private char[] approvedRequests ;
    private char[] rejectedRequests;
    private  char[] pendingRequests;
}