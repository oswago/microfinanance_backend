package com.microfinance.loanapplications.dto.disbursement;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DisburseLoanDto {
    @NotNull
    private LocalDate disbursementDate;
    
    @NotNull
    private BigDecimal disbursementAmount;
    
    private String disbursementMethod; // CASH, BANK_TRANSFER, MOBILE_MONEY
    private String transactionReference;
    private String bankAccountNumber;
    private String bankName;
    private String mobileMoneyProvider;
    private String mobileMoneyNumber;
    private String disbursementNotes;
    
    // Fees and deductions
    private BigDecimal processingFee;
    private BigDecimal insuranceFee;
    private BigDecimal otherDeductions;
    private String deductionNotes;
}