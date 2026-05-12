package com.microfinance.loanapplications.dto.collection;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAssignOfficerRequestDto {
    
    @NotEmpty(message = "Loan IDs list cannot be empty")
    private List<Long> loanIds;
    
    @NotNull(message = "Officer ID is required")
    private Long officerId;
    
    private String notes;
}