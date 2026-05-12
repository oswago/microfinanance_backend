// dto/report/ProductPortfolioDto.java
package com.microfinance.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPortfolioDto {
    
    private String name;
    private Integer loanCount;
    private BigDecimal outstandingAmount;
    private BigDecimal par30;
    private BigDecimal averageInterestRate;  // Change from Double to BigDecimal
    
    // Constructor matching the JPQL query in LoanRepository
    public ProductPortfolioDto(String name, Long loanCount, BigDecimal outstandingAmount, 
                               BigDecimal par30, BigDecimal averageInterestRate) {
        this.name = name;
        this.loanCount = loanCount != null ? loanCount.intValue() : 0;
        this.outstandingAmount = outstandingAmount != null ? outstandingAmount : BigDecimal.ZERO;
        this.par30 = par30 != null ? par30 : BigDecimal.ZERO;
        this.averageInterestRate = averageInterestRate != null ? averageInterestRate : BigDecimal.ZERO;
    }
}