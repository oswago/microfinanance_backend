package com.microfinance.loanapplications.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanRestructureRequestDto {
    @NotNull(message = "Restructure type is required")
    private String restructureType; // RATE_CHANGE, PRINCIPAL_REDUCTION, CONSOLIDATION

    private BigDecimal newInterestRate;
    private BigDecimal principalReduction;
    private Long consolidationLoanId;
    private String reason;

    @NotNull(message = "Reason is required")
    private String justification;

    private String approvalReference;
    private String comments;
}