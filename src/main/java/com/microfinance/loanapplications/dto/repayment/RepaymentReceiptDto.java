package com.microfinance.loanapplications.dto.repayment;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepaymentReceiptDto {
    private Long repaymentId;
    private String receiptNumber;
    private Long loanId;
    private String loanAccountNumber;
    private String borrowerName;
    private String borrowerNumber;
    private String branchName;
    private BigDecimal feesAmount;  // Add this field
    private BigDecimal amountPaid;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal penaltyAmount;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String transactionReference;
    private String receivedBy;
    private LocalDateTime processedAt;
    
    // Allocation fields flattened instead of having a nested object
    private BigDecimal totalAllocated;
    private BigDecimal remainingAmount;
    private Integer installmentsAffected;
    
    // Constructor matching the JPQL query
    public RepaymentReceiptDto(Long repaymentId, String receiptNumber, Long loanId, 
                              String loanAccountNumber, String borrowerName, 
                              String borrowerNumber, String branchName,
                              BigDecimal amountPaid, BigDecimal principalAmount, 
                              BigDecimal interestAmount, BigDecimal penaltyAmount,
                              LocalDate paymentDate, Object paymentMethod, 
                              String transactionReference, String receivedBy,
                              LocalDateTime createdAt) {
        this.repaymentId = repaymentId;
        this.receiptNumber = receiptNumber;
        this.loanId = loanId;
        this.loanAccountNumber = loanAccountNumber;
        this.borrowerName = borrowerName;
        this.borrowerNumber = borrowerNumber;
        this.branchName = branchName;
        this.amountPaid = amountPaid;
        this.principalAmount = principalAmount;
        this.interestAmount = interestAmount;
        this.penaltyAmount = penaltyAmount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod != null ? paymentMethod.toString() : null;
        this.transactionReference = transactionReference;
        this.receivedBy = receivedBy;
        this.processedAt = createdAt;
    }
}