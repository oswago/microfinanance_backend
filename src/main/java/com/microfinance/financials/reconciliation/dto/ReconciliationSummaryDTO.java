// dto/ReconciliationSummaryDTO.java
package com.microfinance.financials.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationSummaryDTO {
    private int totalItems;
    private int matchedItems;
    private int pendingItems;
    private BigDecimal totalAmount;
    private BigDecimal matchedAmount;
    private BigDecimal outstandingAmount;
}