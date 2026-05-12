package com.microfinance.loanapplications.dto.repayment;

import lombok.Data;

import java.util.List;

@Data
public class BulkRepaymentResultDto {
    private List<RepaymentReceiptDto> successfulRepayments;
    private List<BulkRepaymentErrorDto> errors;
    private Integer totalProcessed;
    private Integer totalFailed;
}