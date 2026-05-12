// CreateScheduleRequestDto.java
package com.microfinance.loanapplications.dto.repayment;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateScheduleRequestDto {
    @NotNull
    private Long loanId;
    
    @NotNull
    private LocalDate startDate;
    
    private LocalDate firstPaymentDate;
    private String paymentFrequency; // MONTHLY, WEEKLY, BI_WEEKLY
    private BigDecimal customPaymentAmount;
    private String notes;
}