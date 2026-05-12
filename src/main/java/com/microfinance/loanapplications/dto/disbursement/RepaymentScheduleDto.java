package com.microfinance.loanapplications.dto.disbursement;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// RepaymentScheduleDto
@Data
public class RepaymentScheduleDto {
    private Long id;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal totalDueAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private String status;
    private LocalDate paidDate;
    private BigDecimal penaltyAmount;
}

