// dto/JournalEntryLineDto.java
package com.microfinance.financials.generalledger.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class JournalEntryLineDto {
    private Long id;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private String debitCredit;
    private BigDecimal amount;
    private String description;
}