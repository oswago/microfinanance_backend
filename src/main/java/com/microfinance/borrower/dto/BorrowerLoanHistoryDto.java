// BorrowerLoanHistoryDto.java
package com.microfinance.borrower.dto;

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
public class BorrowerLoanHistoryDto {
    private Long id;
    private String loanAccountNumber;
    private BigDecimal principalAmount;
    private BigDecimal totalPaid;
    private BigDecimal outstandingBalance;
    private LocalDate disbursementDate;
    private LocalDate closedDate;
    private String status;
    private String loanProductName;
    private Integer tenureMonths;
    private BigDecimal interestRate;
    private LocalDate lastPaymentDate;
    private Integer numberOfPayments;
    private Integer latePayments;
    private String disbursedBy;
    private String closedBy;
}