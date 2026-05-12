package com.microfinance.loanapplications.dto.repayment;

import com.microfinance.loanapplications.entity.RepaymentSchedule;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class InstallmentAllocationDto {
    private Long installmentId;
    private Integer installmentNumber;
    private BigDecimal totalAllocated;
    private BigDecimal principalAllocated;
    private BigDecimal interestAllocated;
    private BigDecimal penaltyAllocated;
    private BigDecimal feesPaid;  // Add this field

    private LocalDate dueDate;
    private BigDecimal outstandingAmount;
    private BigDecimal penaltyPaid;
    private BigDecimal interestPaid;
    private BigDecimal principalPaid;
    private BigDecimal totalPaid;
    private Boolean isFullyPaid;




}