// dto/ReconciliationDTO.java
package com.microfinance.financials.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationDTO {
    private Long id;
    private String reconciliationNumber;
    private LocalDate reconciliationDate;
    private Long bankAccountId;
    private String bankAccountName;
    private String bankAccountNumber;
    private BigDecimal systemBalance;
    private BigDecimal statementBalance;
    private BigDecimal difference;
    private String status;
    private LocalDateTime completedAt;
    private Long completedBy;
    private String completedByName;
    private String notes;
    private List<ReconciliationItemDTO> items;
    private ReconciliationSummaryDTO summary;
}