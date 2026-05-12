// BorrowerLoanSummaryDto.java
package com.microfinance.borrower.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerLoanSummaryDto {
    private Map<String, Long> loansByStatus;
    private Map<String, BigDecimal> amountByStatus;
    private Map<String, Long> loansByProduct;
    private Map<Integer, Long> loansByYear;
    private BigDecimal totalCurrentBalance;
    private BigDecimal totalArrears;
    private Long totalActiveLoans;
    private Long totalCompletedLoans;
    private Long totalWrittenOffLoans;
    private BigDecimal averageLoanSize;
    private Integer averageLoanTerm;
}