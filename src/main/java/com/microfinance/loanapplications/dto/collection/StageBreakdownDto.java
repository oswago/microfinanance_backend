package com.microfinance.loanapplications.dto.collection;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageBreakdownDto {
    private Long new_;
    private Long followUp;
    private Long escalated;
    private Long legal;
    private BigDecimal newAmount;
    private BigDecimal followUpAmount;
    private BigDecimal escalatedAmount;
    private BigDecimal legalAmount;
}