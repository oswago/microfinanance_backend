// dto/AccountCategoryDto.java
package com.microfinance.financials.chartofaccounts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCategoryDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String accountType;
    private String accountTypeDisplay;
    private String normalBalance;
    private String normalBalanceDisplay;
    private Integer sortOrder;
    private Boolean isActive;
    private Integer accountCount;
    private List<AccountDto> accounts;
}