// dto/TaxTransactionDTO.java
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
public class TaxTransactionDTO {
    private Long id;
    private Long taxConfigId;
    private String taxCode;
    private String taxName;
    private Long referenceId;
    private String referenceType;
    private LocalDate transactionDate;
    private BigDecimal taxableAmount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal withheldAmount;
    private BigDecimal remittedAmount;
    private String status;
    private LocalDate remittanceDate;
    private String remittanceReference;
    private String notes;
    private LocalDateTime createdAt;
}