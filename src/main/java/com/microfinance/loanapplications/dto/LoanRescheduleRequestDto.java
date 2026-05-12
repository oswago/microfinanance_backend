package com.microfinance.loanapplications.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LoanRescheduleRequestDto {
    @NotNull(message = "Reschedule type is required")
    private String rescheduleType; // EXTEND_TENURE, REDUCE_INSTALLMENT, PAYMENT_HOLIDAY

    private Integer newTenureMonths;
    private BigDecimal newInstallmentAmount;
    private Integer paymentHolidayMonths;
    private LocalDate newMaturityDate;

    @NotNull(message = "Reason is required")
    private String reason;

    private String supportingDocumentRef;
    private String comments;
}