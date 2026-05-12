// dto/AccountDto.java
package com.microfinance.financials.chartofaccounts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String accountType;
    private String accountTypeDisplay;
    private String normalBalance;
    private String normalBalanceDisplay;
    private Long parentAccountId;
    private String parentAccountName;
    private BigDecimal currentBalance;
    private BigDecimal openingBalance;
    private Boolean isActive;
    private Boolean isLeaf;
    private String bankAccountDetails;
    private String createdAt;
    private String updatedAt;
}