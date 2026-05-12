package com.microfinance.loanapplications.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@Builder
@Data
public class DailyCollectionDto {
    private LocalDate collectionDate;
    private BigDecimal totalCollection;
    private Integer totalTransactions;
    private List<CollectionItemDto> items;
    private LocalDate reportDate;
    private Long branchId;
}