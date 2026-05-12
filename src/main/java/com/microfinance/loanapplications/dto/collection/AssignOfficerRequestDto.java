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
public class AssignOfficerRequestDto {
    
    @NotNull(message = "Officer ID is required")
    private Long officerId;
    
    private String notes;
}