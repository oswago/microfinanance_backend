package com.microfinance.loanapplications.dto.collection;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogPhoneCallDto {
    @NotNull(message = "Loan ID is required")
    private Long loanId;
    
    private String notes;
    private String outcome;
    private String contactPerson;
    private String contactNumber;
}