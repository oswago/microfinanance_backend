// Create new file: UpdateRecoveryCaseAfterPaymentDto.java
package com.microfinance.loanapplications.dto.collection;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateRecoveryCaseAfterPaymentDto {
    
    @NotNull(message = "Loan ID is required")
    private Long loanId;
    
    @NotNull(message = "Amount paid is required")
    @Positive(message = "Amount paid must be positive")
    private Double amountPaid;
    
    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;
}