// EligibleLoanDto.java
package com.microfinance.loanapplications.dto.rescheduling;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibleLoanDto {
    private Long id;
    private String loanAccountNumber;
    private String borrowerName;
    private String borrowerId;
    private BigDecimal principalAmount;
    private BigDecimal outstandingBalance;
    private BigDecimal currentMonthlyPayment;
    private Integer remainingInstallments;
    private BigDecimal interestRate;
    private String status;
    private Integer daysOverdue;
    private String displayName;
}