
// dto/ReconciliationItemDTO.java
package com.microfinance.financials.reconciliation.dto;

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
public class ReconciliationItemDTO {
    private Long id;
    private LocalDate transactionDate;
    private String description;
    private String referenceNumber;
    private String itemType;
    private String category;
    private BigDecimal amount;
    private String status;
    private String notes;
    private Boolean isMatched;
    private String matchedWith;
}