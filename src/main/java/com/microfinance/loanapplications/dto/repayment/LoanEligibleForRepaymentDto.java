package com.microfinance.loanapplications.dto.repayment;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoanEligibleForRepaymentDto {
    private Long id;
    private String loanNumber;
    private Long borrowerId;
    private String borrowerName;
    private String borrowerIdNumber;
    private Long branchId;
    private String branchName;
    private Long loanProductId;
    private String loanProductName;
    private BigDecimal outstandingBalance;
    private String nextPaymentDate;
    private BigDecimal nextPaymentAmount;
    private Integer daysOverdue;
    private String status;
}