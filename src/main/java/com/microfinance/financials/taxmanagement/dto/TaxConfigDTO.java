// dto/TaxConfigDTO.java
package com.microfinance.financials.taxmanagement.dto;

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
public class TaxConfigDTO {
    private Long id;
    private String taxCode;
    private String taxName;
    private String taxType;
    private BigDecimal rate;
    private String calculationMethod;
    private Boolean isActive;
    private Boolean isCompound;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private BigDecimal exemptionThreshold;
    private Long glAccountId;
    private String glAccountCode;
    private String glAccountName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}





