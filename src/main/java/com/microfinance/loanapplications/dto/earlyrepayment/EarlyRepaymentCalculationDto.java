package com.microfinance.loanapplications.dto.earlyrepayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarlyRepaymentCalculationDto {
    private Long loanId;
    private BigDecimal outstandingPrincipal;
    private BigDecimal accruedInterest;
    private BigDecimal penaltyCharges;
    private BigDecimal totalPayable;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal earlyRepaymentAmount;
    private BigDecimal interestSavings;
    private BigDecimal interestSavingsPercentage;
    private List<EarlyRepaymentOptionDto> options;

    // ✅ Added these two fields
    private BigDecimal totalInterestIfNormal;
    private BigDecimal totalInterestIfEarly;

}