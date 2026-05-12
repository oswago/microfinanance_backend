// dto/SingleInstallmentAllocation.java
package com.microfinance.loanapplications.dto.repayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SingleInstallmentAllocation {
    private Long installmentId;
    private Integer installmentNumber;
    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
    private BigDecimal penaltyPaid;
    private BigDecimal feesPaid;
    private BigDecimal totalPaid;
    private BigDecimal remainingAfterPayment;
    private boolean isFullyPaid;
}