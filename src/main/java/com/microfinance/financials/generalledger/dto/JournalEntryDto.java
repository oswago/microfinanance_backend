// dto/JournalEntryDto.java
package com.microfinance.financials.generalledger.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class JournalEntryDto {
    private Long id;
    private String journalNumber;
    private LocalDate entryDate;
    private String description;
    private String journalType;
    private String status;
    private Long financialPeriodId;
    private String periodName;
    private List<JournalEntryLineDto> lines;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private LocalDateTime postedAt;
    private Long postedBy;
    private LocalDateTime createdAt;
}