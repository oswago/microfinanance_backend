// RescheduleRequestDto
package com.microfinance.loanapplications.dto.disbursement;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class RescheduleRequestDto {
    @NotNull
    private LocalDate newMaturityDate;
    
    @NotNull
    private String reason;
    
    private String additionalNotes;
    private Integer extensionMonths;
}