
// dto/TaxReportDTO.java
package com.microfinance.financials.taxmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxReportDTO {
    private ReportHeader header;
    private TaxSummary summary;
    private List<TaxTransactionDTO> transactions;
    private Map<String, TaxByTypeDTO> taxesByType;
    private LocalDate startDate;
    private LocalDate endDate;
}
