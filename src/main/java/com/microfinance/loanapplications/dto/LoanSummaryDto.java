package com.microfinance.loanapplications.dto;

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
public class LoanSummaryDto {
    private Long id;
    private String loanAccountNumber;
    private BigDecimal principalAmount;
    private BigDecimal outstandingBalance;
    private String status;
    private LocalDate disbursementDate;
    private LocalDate nextDueDate;
    private BigDecimal nextDueAmount;
    private Integer daysDelinquent;
    private Boolean isDelinquent;
    private Integer progressPercentage;
}