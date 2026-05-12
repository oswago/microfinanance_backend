// dto/report/ProductPortfolioDto.java
package com.microfinance.reports.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ProductPortfolioDto {
    private String name;
    private Integer loanCount;
    private BigDecimal outstandingAmount;
    private BigDecimal par30;
    private Double averageInterestRate;
}