package com.microfinance.loanapplications.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRestructureResponseDto {
    private Long id;
    private String restructureReference;
    private Long loanId;
    private String loanAccountNumber;
    private String restructureType;
    private String status;
    private BigDecimal oldInterestRate;
    private BigDecimal newInterestRate;
    private BigDecimal oldPrincipal;
    private BigDecimal newPrincipal;
    private BigDecimal oldOutstanding;
    private BigDecimal newOutstanding;
    private String reason;
    private LocalDateTime requestDate;
    private String requestedBy;
    private LocalDateTime approvalDate;
    private String approvedBy;
    private String comments;
}