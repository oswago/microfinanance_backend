package com.microfinance.loanapplications.dto.repayment;

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
public class RepaymentSummaryDto {
    private Long id;
    private String receiptNumber;
    private LocalDate paymentDate;
    private BigDecimal amountPaid;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal feesAmount;
    private String paymentMethod;
    private String transactionReference;
    private String receivedBy;
    private LocalDateTime createdAt;
    private String status;
}