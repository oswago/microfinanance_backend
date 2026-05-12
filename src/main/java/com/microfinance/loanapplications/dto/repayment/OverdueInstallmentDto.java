package com.microfinance.loanapplications.dto.repayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverdueInstallmentDto {
    private Long installmentId;
    private Long loanId;
    private String loanAccountNumber;
    private Long borrowerId;
    private String borrowerName;
    private String borrowerPhone;

    private Long id;
    private String phoneNumber;
    
    // Installment details
    private Integer installmentNumber;
    private LocalDate dueDate;
    private Integer daysOverdue;
    
    // Amount details
    private BigDecimal principalDue;
    private BigDecimal interestDue;
    private BigDecimal penaltyAccrued;
    private BigDecimal totalDueAmount;
    private BigDecimal totalPaidAmount;
    private BigDecimal outstandingAmount;
    private BigDecimal amountOverdue;
    private BigDecimal principalOverdue;
    private BigDecimal interestOverdue;
    private BigDecimal penaltyOverdue;
    
    // Loan officer details
    private Long loanOfficerId;
    private String loanOfficerName;
    private String loanOfficerPhone;
    
    // Branch information
    private Long branchId;
    private String branchName;
    private String branchCode;
    
    // Collection status
    private Integer contactAttempts;
    private LocalDate lastContactDate;
    private String collectionStatus;
    private String remarks;
    
    // Risk assessment
    private String riskLevel; // LOW, MEDIUM, HIGH
    private Integer overallInstallmentNumber;
    private Integer totalInstallments;

    private BigDecimal totalDue;

    
    public boolean isSeverelyOverdue() {
        return daysOverdue > 30;
    }
    
    public boolean isModeratelyOverdue() {
        return daysOverdue > 15 && daysOverdue <= 30;
    }
    
    public boolean isRecentlyOverdue() {
        return daysOverdue > 0 && daysOverdue <= 15;
    }
}