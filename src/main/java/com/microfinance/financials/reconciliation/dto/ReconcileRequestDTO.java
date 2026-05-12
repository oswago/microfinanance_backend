// dto/ReconcileRequestDTO.java
package com.microfinance.financials.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconcileRequestDTO {
    private Long bankAccountId;
    private LocalDate reconciliationDate;
    private BigDecimal statementBalance;
    private String notes;
    private List<ReconciliationItemDTO> manualAdjustments;
}
