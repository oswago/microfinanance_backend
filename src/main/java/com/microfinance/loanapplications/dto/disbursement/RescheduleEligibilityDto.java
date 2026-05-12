package com.microfinance.loanapplications.dto.disbursement;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// RescheduleEligibilityDto
@Data
public class RescheduleEligibilityDto {
    private Long loanId;
    private String loanAccountNumber;
    private String currentStatus;
    private Boolean eligible;
    private String message;
    private Integer daysDelinquent;
    private BigDecimal outstandingBalance;
    private LocalDate currentMaturityDate;
    private Integer maxExtensionMonths;
}