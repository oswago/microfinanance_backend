package com.microfinance.loanapplications.dto.repayment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BulkRepaymentErrorDto {
    private Integer index;
    private Long loanId;
    private String errorMessage;
}