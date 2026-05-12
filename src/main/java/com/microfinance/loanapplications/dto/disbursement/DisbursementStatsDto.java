package com.microfinance.loanapplications.dto.disbursement;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisbursementStatsDto {
    private long pending;
    private BigDecimal todayAmount;
    private BigDecimal weekAmount;
    private BigDecimal monthAmount;
}