// BorrowerActiveLoanDto.java
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
public class BorrowerActiveLoanDto {
    private Long id;
    private String loanAccountNumber;
    private BigDecimal principalAmount;
    private BigDecimal outstandingBalance;
    private BigDecimal monthlyPayment;
    private LocalDate disbursementDate;
    private LocalDate maturityDate;
    private String status;
    private Integer daysDelinquent;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private Integer remainingInstallments;
    private String loanProductName;
    private Long branchId;
    private String branchName;
}