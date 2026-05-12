package com.microfinance.loanapplications.dto.earlyrepayment;

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
public class EarlyRepaymentHistoryDto {
    private Long id;
    private LocalDate date;
    private String action;
    private String performedBy;
    private String comments;
    private String status;
    private String requestNumber;
    private String loanNumber;
    private String borrowerName;
    private String borrowerIdNumber;
    private BigDecimal outstandingPrincipal;
    private BigDecimal accruedInterest;
    private BigDecimal earlyRepaymentAmount;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal interestSavings;
    private BigDecimal interestSavingsPercentage;
    private LocalDate requestedDate;
    private LocalDate approvedDate;
    private LocalDate paymentDate;
    private String requestedBy;
    private String approvedBy;
    private String rejectedBy;
    private String rejectionReason;
    private String approvalComments;
    private Long branchId;
    private String branchName;
    private Long loanProductId;
    private String loanProductName;

}