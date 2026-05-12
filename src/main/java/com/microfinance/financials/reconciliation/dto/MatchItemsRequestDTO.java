
// dto/MatchItemsRequestDTO.java
package com.microfinance.financials.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchItemsRequestDTO {
    private Long reconciliationId;
    private List<Long> systemItemIds;
    private List<Long> bankItemIds;
}