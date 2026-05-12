// dto/ProvisionCalculationDTO.java
package com.microfinance.financials.provisions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvisionCalculationDTO {
    private Long id;
    private String calculationNumber;
    private LocalDate calculationDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Long loanId;
    private String loanAccountNumber;
    private Long borrowerId;
    private String borrowerName;
    private BigDecimal principalOutstanding;
    private BigDecimal interestOutstanding;
    private BigDecimal totalOutstanding;
    private Integer daysPastDue;
    private String agingBucket;
    private BigDecimal provisionRate;
    private BigDecimal provisionAmount;
    private BigDecimal existingProvision;
    private BigDecimal provisionAdjustment;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private String calculatedByName;
}


