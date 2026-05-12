package com.microfinance.loanapplications.dto;

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
public class OverdueLoanDto {
    private Long id;
    private String loanAccountNumber;
    
    // Borrower information
    private Long borrowerId;
    private String borrowerName;
    private String borrowerPhone;
    private String borrowerIdNumber;
    
    // Loan information
    private Long loanOfficerId;
    private String loanOfficerName;
    private Long branchId;
    private String branchName;
    private Long loanProductId;
    private String loanProductName;
    
    // Overdue details
    private Integer daysOverdue;
    private BigDecimal overdueAmount;
    private BigDecimal principalOverdue;
    private BigDecimal interestOverdue;
    private BigDecimal penaltyOverdue;
    // Add these fields
    private BigDecimal outstandingBalance;  // Current outstanding balance
    private BigDecimal totalLoanAmount;     // Original loan amount
    
    // Dates
    private LocalDate dueDate;
    private LocalDate lastPaymentDate;
    private LocalDate lastContactDate;
    private LocalDate nextFollowUpDate;
    
    // Contact information
    private String contactPreference;
    private String alternatePhone;
    private String email;
    private String address;
    
    // Collection status
    private String collectionStage; // NEW, FOLLOW_UP, ESCALATED, LEGAL
    private Integer followUpCount;
    private String riskLevel; // LOW, MEDIUM, HIGH
    private String notes;
}