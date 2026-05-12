package com.microfinance.loanapplications.dto.earlyrepayment;

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
public class EligibleLoanDto {
    private Long id;
    private String loanNumber;
    private String borrowerName;
    private Long borrowerId;
    private String borrowerIdNumber;
    private Long loanProductId;
    private String loanProductName;
    private BigDecimal outstandingBalance;
    private Integer remainingTenure;
    private BigDecimal monthlyPayment;
    private BigDecimal totalInterestDue;
    private BigDecimal earlyRepaymentFee;
    private LocalDate disbursementDate;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private String displayName; // For dropdown display
        private Integer paidInstallments;
        private Integer originalTenure;


}




