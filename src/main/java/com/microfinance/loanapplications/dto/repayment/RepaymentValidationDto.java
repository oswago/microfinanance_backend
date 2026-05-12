package com.microfinance.loanapplications.dto.repayment;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RepaymentValidationDto {
    private Long loanId;
    private BigDecimal amount;
}