package com.microfinance.loanapplications.dto.repayment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RepaymentRequestDto {
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @NotNull(message = "Payment method is required")
    private String paymentMethod; // CASH, BANK_TRANSFER, MOBILE_MONEY, CHEQUE

    private String transactionReference;
    private String bankName;
    private String bankAccountNumber;
    private String chequeNumber;
    private String mobileMoneyProvider;
    private String mobileMoneyNumber;
    private String paymentNotes;

    private Boolean applyToArrearsFirst = true;
    private Long specificInstallmentId; // If paying specific installment
}