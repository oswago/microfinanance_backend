// RepaymentStatisticsDto.java
package com.microfinance.loanapplications.dto.repayment;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentStatisticsDto {
    private Long dueToday;
    private BigDecimal collectedToday;
    private Long overdue;
    private Double onTimeRate;
    private BigDecimal totalCollected;
    private Long totalRepayments;
    private BigDecimal averageRepayment;
}