package com.microfinance.loanapplications.dto.approval;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BulkApprovalError {
    private Long applicationId;
    private String errorMessage;
}