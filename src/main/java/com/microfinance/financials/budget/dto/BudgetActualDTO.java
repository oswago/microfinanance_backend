// dto/BudgetActualDTO.java
package com.microfinance.financials.budget.dto;

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
public class BudgetActualDTO {
    private Long id;
    private Long budgetId;
    private String budgetName;
    private String category;
    private Integer periodMonth;
    private Integer periodYear;
    private LocalDate periodDate;
    private BigDecimal budgetAmount;
    private BigDecimal actualAmount;
    private BigDecimal varianceAmount;
    private BigDecimal variancePercentage;
    private String status;
    private String notes;
}