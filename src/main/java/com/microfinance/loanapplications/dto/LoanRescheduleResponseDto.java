package com.microfinance.loanapplications.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRescheduleResponseDto {
    private Long id;
    private String requestReference;
    private Long loanId;
    private String loanAccountNumber;
    private String rescheduleType;
    private String status; // PENDING, APPROVED, REJECTED
    private Integer oldTenureMonths;
    private Integer newTenureMonths;
    private BigDecimal oldInstallmentAmount;
    private BigDecimal newInstallmentAmount;
    private LocalDate oldMaturityDate;
    private LocalDate newMaturityDate;
    private String reason;
    private LocalDate requestDate;
    private String requestedBy;
    private LocalDate approvalDate;
    private String approvedBy;
    private String comments;
    private List<RescheduleInstallmentDto> newSchedule;
}