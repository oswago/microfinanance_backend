package com.microfinance.loanapplications.dto.repayment;

import com.microfinance.loanapplications.entity.RepaymentSchedule;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class RepaymentAllocationDto {
    private BigDecimal totalAmount;
    private BigDecimal allocatedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal feesAmount;
    private boolean isFullyPaid;
    private List<InstallmentAllocationDto> allocations;
    private List<com.microfinance.loanapplications.entity.RepaymentSchedule> allocatedInstallments;

}
