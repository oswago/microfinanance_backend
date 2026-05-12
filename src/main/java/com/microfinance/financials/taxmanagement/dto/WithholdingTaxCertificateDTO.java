
// dto/WithholdingTaxCertificateDTO.java
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
public class WithholdingTaxCertificateDTO {
    private Long id;
    private String certificateNumber;
    private Long taxTransactionId;
    private Long borrowerId;
    private String borrowerName;
    private String borrowerTin;
    private Long loanId;
    private String loanAccountNumber;
    private BigDecimal interestAmount;
    private BigDecimal withholdingTaxRate;
    private BigDecimal withholdingTaxAmount;
    private LocalDate certificateDate;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private String status;
    private LocalDateTime printedAt;
    private String notes;
}