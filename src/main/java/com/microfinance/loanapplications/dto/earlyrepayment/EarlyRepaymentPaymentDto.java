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
public class EarlyRepaymentPaymentDto {
    private String reference;
    private LocalDate paymentDate;
    private String receivedBy;
    private String paymentMethod;
    private BigDecimal amount;
    private String receiptNumber;


}