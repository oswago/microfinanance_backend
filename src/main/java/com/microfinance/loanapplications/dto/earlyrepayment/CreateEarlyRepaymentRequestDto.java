package com.microfinance.loanapplications.dto.earlyrepayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEarlyRepaymentRequestDto {
    private Long loanId;
    private String reason;
    private String preferredPaymentMethod;
    private LocalDate targetSettlementDate;
}