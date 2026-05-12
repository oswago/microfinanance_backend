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
public class WriteOffRequestDTO {
    private Long id;
    private String requestNumber;
    private Long loanId;
    private String loanAccountNumber;
    private Long borrowerId;
    private String borrowerName;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal feesAmount;
    private BigDecimal totalAmount;
    private BigDecimal provisionAmount;
    private BigDecimal netWriteOff;
    private LocalDate requestDate;
    private String reason;
    private String reasonDescription;
    private String status;
    private LocalDate approvalDate;
    private String approvalNotes;
    private LocalDate writeOffDate;
    private String notes;
    private LocalDateTime createdAt;
    private String requestedByName;
    private String approvedByName;
}
