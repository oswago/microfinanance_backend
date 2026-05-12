package com.microfinance.financials.provisions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRecoveryDTO {
    private Long id;
    private String recoveryNumber;
    private Long writeOffId;
    private Long loanId;
    private String loanAccountNumber;
    private Long borrowerId;
    private String borrowerName;
    private LocalDate recoveryDate;
    private BigDecimal recoveredAmount;
    private BigDecimal principalRecovered;
    private BigDecimal interestRecovered;
    private BigDecimal penaltyRecovered;
    private BigDecimal feesRecovered;
    private String recoveryType;
    private String referenceNumber;
    private String notes;
    private LocalDateTime createdAt;
    private String createdByName;
}
