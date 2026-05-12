package com.microfinance.loanapplications.dto.disbursement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDisbursementResult {
    private Long loanId;
    private String loanAccountNumber;
    private String status;
    private String message;
}