package com.microfinance.loanapplications.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

// Simple validation DTOs
@Data
public class ApplicationValidationRequest {
    @NotNull
    private Long borrowerId;
    
    @NotNull
    private Long loanProductId;
}