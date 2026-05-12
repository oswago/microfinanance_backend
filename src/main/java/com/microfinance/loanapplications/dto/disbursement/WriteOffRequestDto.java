// WriteOffRequestDto.java (update)
package com.microfinance.loanapplications.dto.disbursement;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WriteOffRequestDto {
    
    @NotNull(message = "Write-off amount is required")
    @DecimalMin(value = "0.01", message = "Write-off amount must be greater than zero")
    private BigDecimal writeOffAmount;
    
    @NotBlank(message = "Write-off reason is required")
    private String writeOffReason;
    
    private String approvalReference;
    
    private String comments;
    
    private String recoveryPlan; // FUTURE_RECOVERY, LEGAL_ACTION, NO_RECOVERY
    
    private LocalDate writeOffDate;
}





