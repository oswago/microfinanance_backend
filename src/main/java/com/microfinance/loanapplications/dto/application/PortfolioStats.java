package com.microfinance.loanapplications.dto.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

// PortfolioStats class for statistics
@Data
@AllArgsConstructor
    public class PortfolioStats {
        private final Long activeLoans;
        private final BigDecimal outstandingPrincipal;
        private final BigDecimal totalPortfolioValue;
        private final Long delinquentLoans;
        private final Long loansDisbursedThisMonth;
        private final BigDecimal amountDisbursedThisMonth;

        public BigDecimal getPortfolioAtRiskRate() {
            if (activeLoans == 0) return BigDecimal.ZERO;
            return BigDecimal.valueOf(delinquentLoans)
                    .divide(BigDecimal.valueOf(activeLoans), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }
