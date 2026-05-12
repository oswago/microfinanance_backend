package com.microfinance.loanapplications.dto.repayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// InstallmentDto.java
public class InstallmentDto {
    private Long id;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private LocalDate paymentDate;
    private Integer daysOverdue;
    private String status;
    private String paymentMethod;
    private String transactionReference;
    private String notes;


    // Payment details from LoanRepayment
    private String receiptNumber;
    private BigDecimal penaltyAmount;
    private BigDecimal feesAmount;
    private String receivedBy;
    private LocalDateTime createdAt;

    // For partial payments, we might have multiple repayments
    private List<RepaymentSummaryDto> repaymentHistory;
}
