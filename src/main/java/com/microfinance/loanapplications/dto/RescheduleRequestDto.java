package com.microfinance.loanapplications.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class RescheduleRequestDto {
    @NotNull(message = "Loan ID is required")
    private Long loanId;
    
    @NotNull(message = "New tenure is required")
    @Min(value = 1, message = "Tenure must be at least 1 month")
    @Max(value = 84, message = "Maximum tenure is 84 months")
    private Integer newTenureMonths;
    
    @NotBlank(message = "Reason is required")
    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    private String reason;
    
    private Integer gracePeriodDays = 0;
    private Boolean interestRecalculation = false;
    private LocalDate effectiveDate;

    private String AdditionalNotes;
    public LocalDate newMaturityDate;

    private Integer extensionMonths;

    private LocalDate newStartDate;
}

