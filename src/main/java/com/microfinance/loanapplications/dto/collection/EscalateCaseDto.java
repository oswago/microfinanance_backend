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
public class EscalateCaseDto {
    @NotNull(message = "Loan ID is required")
    private Long loanId;

    private String reason;
    private String notes;
    private Long escalateToOfficerId;
    private Long caseId;
    private Long escalateToAgentId;
}