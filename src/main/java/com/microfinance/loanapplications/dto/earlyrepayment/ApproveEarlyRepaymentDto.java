package com.microfinance.loanapplications.dto.earlyrepayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveEarlyRepaymentDto {
    private String approvedBy;
    private LocalDate approvalDate;
    private String comments;
    private BigDecimal customDiscountPercentage; // Optional - for overriding discount
}