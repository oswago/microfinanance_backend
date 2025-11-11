package com.microfinance.base.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FinanceUtils {
    // Calculate monthly payment for amortizing loan (annuity formula)
    public static BigDecimal monthlyPayment(BigDecimal principal, BigDecimal monthlyRate, int months) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }
        BigDecimal onePlusR = monthlyRate.add(BigDecimal.ONE);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusR.pow(months));
        BigDecimal denominator = onePlusR.pow(months).subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
