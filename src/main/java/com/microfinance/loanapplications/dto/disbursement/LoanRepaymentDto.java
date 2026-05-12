package com.microfinance.loanapplications.dto.disbursement;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// LoanRepaymentDto
@Builder
@Data
public class LoanRepaymentDto {
    private Long id;
    private LocalDate paymentDate;
    private BigDecimal amountPaid;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal penaltyAmount;
    private String paymentMethod;
    private String transactionReference;
    private String receiptNumber;
    private String receivedByName;

    private Long loanId;
    private String loanAccountNumber;
    private String borrowerName;

    private String status;
    private String notes;
}