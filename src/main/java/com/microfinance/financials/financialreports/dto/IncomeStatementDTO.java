// dto/financialreport/FinancialReportDTOs.java
package com.microfinance.financials.financialreports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// Income Statement DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeStatementDTO {
    private ReportHeader header;
    private RevenueSection revenues;
    private ExpenseSection expenses;
    private OtherIncomeSection otherIncome;
    private OtherExpenseSection otherExpenses;
    private FinancialSummary financialSummary;
    private LocalDate reportDate;
    private LocalDate startDate;
    private LocalDate endDate;
}



