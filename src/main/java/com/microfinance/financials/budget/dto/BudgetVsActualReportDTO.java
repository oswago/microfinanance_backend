
// dto/BudgetVsActualReportDTO.java
package com.microfinance.financials.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetVsActualReportDTO {
    private ReportHeader header;
    private BudgetSummary summary;
    private List<BudgetVsActualItem> items;
    private Map<String, MonthlyComparison> monthlyComparisons;
    private Integer fiscalYear;
    private String category;
}