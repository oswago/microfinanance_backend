package com.microfinance.loanapplications.dto.disbursement;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DisbursementReceiptDto {
    private String receiptNumber;
    private LocalDateTime receiptDate;
    
    // Loan information
    private String loanAccountNumber;
    private String borrowerName;
    private String borrowerNumber;
    
    // Disbursement details
    private BigDecimal principalAmount;
    private BigDecimal processingFee;
    private BigDecimal insuranceFee;
    private BigDecimal otherDeductions;
    private BigDecimal netDisbursementAmount;
    
    // Loan terms
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private LocalDate maturityDate;
    
    // Disbursement method
    private String disbursementMethod;
    private String transactionReference;
    private String accountNumber;
    
    // Signatures
    private String disbursedByName;
    private String receivedByName;
    private String branchManagerName;
    
    // Additional info
    private String branchName;
    private String termsAndConditions;
}