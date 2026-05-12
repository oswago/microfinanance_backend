// WriteOffResponseDto.java
package com.microfinance.loanapplications.dto.disbursement;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WriteOffResponseDto {
    private Long loanId;
    private String loanAccountNumber;
    private String borrowerName;
    private String borrowerNumber;
    private BigDecimal writeOffAmount;
    private BigDecimal originalPrincipal;
    private BigDecimal outstandingBalance;
    private String writeOffReason;
    private String writeOffStatus;
    private LocalDate writeOffDate;
    private String writtenOffBy;
    private String approvalReference;
    private String recoveryPlan;
    private LocalDateTime processedAt;
}