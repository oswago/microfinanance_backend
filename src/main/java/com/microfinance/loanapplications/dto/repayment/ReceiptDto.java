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
public class ReceiptDto {
    private String receiptNumber;
    private LocalDate receiptDate;
    private String loanAccountNumber;
    private String borrowerName;
    private String borrowerIdNumber;
    private String borrowerPhone;
    private String borrowerEmail;
    private BigDecimal amountPaid;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal feesAmount;
    private String paymentMethod;
    private String transactionReference;
    private LocalDate paymentDate;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private String receivedBy;
    private String branchName;
    private String branchCode;
    private String receiptType; // INSTALLMENT, LOAN_REPAYMENT, BULK
    private String status;
    private String notes;
    private byte[] qrCode; // Optional QR code for verification
    private String verificationUrl;
}

