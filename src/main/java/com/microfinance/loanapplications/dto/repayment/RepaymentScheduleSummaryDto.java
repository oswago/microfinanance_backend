package com.microfinance.loanapplications.dto.repayment;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentScheduleSummaryDto {
    private Long id;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal dueAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private String status;
    private Boolean isOverdue;
    private LocalDate paidDate;
}