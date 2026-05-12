// RescheduleApprovalDto
package com.microfinance.loanapplications.dto.disbursement;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class
RescheduleApprovalDto {
    private Long id;
    private Long loanId;
    private String loanAccountNumber;
    private LocalDate originalMaturityDate;
    private LocalDate newMaturityDate;
    private Integer extensionMonths;
    private String reason;
    private String status;
    private String requestedByName;
    private LocalDate requestDate;
    private String approvedByName;
    private LocalDate approvalDate;
    private String approvalNotes;
    private String rejectionReason;
    private BigDecimal originalMonthlyPayment;
    private BigDecimal newMonthlyPayment;
    private Integer originalTermMonths;
    private Integer newTermMonths;

    // Loan information fields (ADD THESE)
    private BigDecimal loanAmount; // Principal amount
    private BigDecimal outstandingBalance;
    private Integer daysOverdue;

    private int processingDays;

    private String statusDescription;


    private BigDecimal monthlyPaymentChange;

    private BigDecimal monthlyPaymentChangePercent;



    // Borrower information
    private String borrowerName;
    private String borrowerIdNumber;
    private Long borrowerId;

     private Long branchId;
     private String branchName;

}



