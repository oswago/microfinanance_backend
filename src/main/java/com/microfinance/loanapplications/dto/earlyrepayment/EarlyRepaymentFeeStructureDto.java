package com.microfinance.loanapplications.dto.earlyrepayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarlyRepaymentFeeStructureDto {
    private Long loanProductId;
    private String productName;
    private BigDecimal feeRate;
    private BigDecimal feeAmount;
    private BigDecimal minimumFee;
    private String feeType; // PERCENTAGE, FIXED
    private String calculationBasis; // OUTSTANDING_PRINCIPAL, TOTAL_PAYABLE
    private String description;
}