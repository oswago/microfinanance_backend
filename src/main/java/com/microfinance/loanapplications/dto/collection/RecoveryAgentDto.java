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
public class RecoveryAgentDto {
    private Long id;
    private String name;
    private Integer assignedCases;
    private Integer resolvedCases;
    private BigDecimal recoveryAmount;
}