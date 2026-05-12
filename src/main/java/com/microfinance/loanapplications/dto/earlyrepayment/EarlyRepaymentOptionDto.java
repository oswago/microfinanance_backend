package com.microfinance.loanapplications.dto.earlyrepayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarlyRepaymentOptionDto {
    private String optionType; // STANDARD, PREMIUM, CUSTOM
    private BigDecimal discountPercentage;
    private BigDecimal earlyRepaymentAmount;
    private BigDecimal interestSavings;
    private String description;
    private BigDecimal totalInterestIfNormal;
    private BigDecimal totalInterestIfEarly;
    private BigDecimal interestSavingsPercentage;

}