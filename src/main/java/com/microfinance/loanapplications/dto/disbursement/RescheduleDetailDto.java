// RescheduleDetailDto (extends ApprovalDto with additional fields)
package com.microfinance.loanapplications.dto.disbursement;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RescheduleDetailDto extends RescheduleApprovalDto {
    private String borrowerName;
    private String borrowerNumber;
    private String loanProductName;
    private BigDecimal interestRate;
    private Integer gracePeriodDays;
    private Boolean interestRecalculation;
    private BigDecimal reschedulingFee;
    private LocalDate effectiveDate;
    private BigDecimal monthlyPaymentReduction;
    private BigDecimal totalInterestImpact;
}