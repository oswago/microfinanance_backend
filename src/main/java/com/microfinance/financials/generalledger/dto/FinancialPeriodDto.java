// dto/FinancialPeriodDto.java
package com.microfinance.financials.generalledger.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class FinancialPeriodDto {
    private Long id;
    private Integer year;
    private Integer month;
    private String periodName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime closedAt;
    private Long closedBy;
    private String closedByName;
    private LocalDateTime lockedAt;
    private Long lockedBy;
    private String lockedByName;
    private String notes;
    private LocalDateTime createdAt;
    private String createdByName;
}