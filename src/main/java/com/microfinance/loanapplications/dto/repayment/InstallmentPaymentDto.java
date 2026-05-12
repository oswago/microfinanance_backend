// InstallmentPaymentDto.java
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
public class InstallmentPaymentDto {
    @NotNull
    private Long installmentId;
    
    @NotNull
    private Long loanId;
    
    @NotNull
    private BigDecimal amountPaid;
    
    @NotNull
    private LocalDate paymentDate;
    
    @NotNull
    private String paymentMethod;
    
    private String transactionReference;
    private String notes;

}