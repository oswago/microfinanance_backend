// CreateReschedulingRequestDto.java
package com.microfinance.loanapplications.dto.rescheduling;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReschedulingRequestDto {
    
    @NotNull(message = "Loan ID is required")
    private Long loanId;
    
    @NotNull(message = "Request type is required")
    private String requestType;
    
    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
    
    // Proposed terms
    private BigDecimal proposedMonthlyPayment;
    private Integer proposedInstallments;
    private BigDecimal proposedInterestRate;
    
    // Request specific fields
    private Integer additionalMonths;
    private BigDecimal reducedPayment;
    private Integer holidayMonths;
    private LocalDate resumeDate;
    
    @Size(max = 1000, message = "Additional notes cannot exceed 1000 characters")
    private String additionalNotes;
}



