package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseFilterDto {
    private String search;
    private String status;
    private String stage;
    private String priority;
    private Long assignedTo;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}