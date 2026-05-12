package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResultDto {
    private Long loanId;
    private String loanAccountNumber;
    private String borrowerName;
    private Boolean success;
    private String errorMessage;
}