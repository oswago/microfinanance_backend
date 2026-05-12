// dto/GeneralLedgerDto.java
package com.microfinance.financials.generalledger.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class GeneralLedgerDto {
    private Long id;
    private Long journalId;
    private LocalDate transactionDate;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private String debitCredit;
    private BigDecimal amount;
    private String description;
    private String referenceNumber;
    private String referenceType;
    private LocalDateTime createdAt;
}