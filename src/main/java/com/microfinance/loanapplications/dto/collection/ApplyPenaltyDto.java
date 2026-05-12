package com.microfinance.loanapplications.dto.collection;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyPenaltyDto {
    @NotNull(message = "Loan ID is required")
    private Long loanId;
    
    @NotNull(message = "Penalty amount is required")
    @DecimalMin(value = "0.01", message = "Penalty amount must be greater than 0")
    private BigDecimal amount;
    
    private String reason;
    private String notes;
}