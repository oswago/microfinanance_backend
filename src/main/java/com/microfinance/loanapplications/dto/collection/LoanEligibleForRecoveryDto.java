package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanEligibleForRecoveryDto {
    private Long id;
    private String loanAccountNumber;
    private String borrowerName;
    private String borrowerPhone;
    private BigDecimal outstandingBalance;
    private Integer daysOverdue;
    private LocalDate lastPaymentDate;
    private LocalDate lastContactDate;
    private Integer recoveryAttempts;
}