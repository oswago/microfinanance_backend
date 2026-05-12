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
public class PenaltyResultDto {
    private Long loanId;
    private String loanAccountNumber;
    private BigDecimal penaltyApplied;
    private BigDecimal totalPenalty;
    private String status;
    private String message;
}