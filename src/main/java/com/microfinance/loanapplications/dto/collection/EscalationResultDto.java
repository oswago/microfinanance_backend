package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationResultDto {
    private Long loanId;
    private String loanAccountNumber;
    private String escalationLevel;
    private String status;
    private String message;
}