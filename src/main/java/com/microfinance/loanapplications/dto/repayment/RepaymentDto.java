package com.microfinance.loanapplications.dto.repayment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RepaymentDto {
    @NotNull(message = "Loan ID is required")
    private Long loanId;
    
    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;
    
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    private BigDecimal amountPaid;
    
    @NotNull(message = "Payment method is required")
    private String paymentMethod;
    
    private String transactionReference;
    private String notes;
    
    private Boolean allocateToOldest = true;

    private Long id;
    private String receiptNumber;
    private String status;
    // Loan reference
    private String loanAccountNumber;
    // User who recorded
    private String recordedBy;
    private String recordedByName;
    private LocalDateTime createdAt;
}



