// dto/BankAccountDTO.java
package com.microfinance.financials.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountDTO {
    private Long id;
    private String accountName;
    private String accountNumber;
    private String bankName;
    private String branchCode;
    private String swiftCode;
    private Long chartOfAccountId;
    private String chartOfAccountCode;
    private String chartOfAccountName;
    private BigDecimal currentBalance;
    private BigDecimal availableBalance;
    private String currency;
    private String status;
    private LocalDateTime lastReconciliationDate;
    private BigDecimal openingBalance;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}








