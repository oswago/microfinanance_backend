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
public class CreateRecoveryCaseDto {
    @NotNull(message = "Loan ID is required")
    private Long loanId;
    
    private String priority;
    private String notes;
    private Long assignedToAgentId;
    private Long assignedAgentId;

}