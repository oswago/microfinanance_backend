// dto/TrialBalanceDto.java
package com.microfinance.financials.generalledger.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TrialBalanceDto {
    private String accountCode;
    private String accountName;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balance;
    private Boolean isTotal;
}