package com.microfinance.loanapplications.dto.earlyrepayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarlyRepaymentRequestDto {
    private Long id;
    private String requestNumber;
    private Long loanId;
    private String loanNumber;
    private Long borrowerId;
    private String borrowerName;
    private String borrowerIdNumber;
    private Long loanProductId;
    private String loanProductName;
    private Long branchId;
    private String branchName;

    // Loan details
    private BigDecimal originalLoanAmount;
    private LocalDate disbursementDate;
    private Integer originalTenure;
    private Integer remainingTenure;

    // Outstanding amounts
    private BigDecimal outstandingPrincipal;
    private BigDecimal accruedInterest;
    private BigDecimal penaltyCharges;
    private BigDecimal totalPayable;

    // Early repayment calculations
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal earlyRepaymentAmount;
    private BigDecimal totalInterestIfNormal;
    private BigDecimal totalInterestIfEarly;
    private BigDecimal interestSavings;
    private BigDecimal interestSavingsPercentage;

    // Request details
    private LocalDate requestedDate;
    private String requestedBy;
    private String reason;
    private String preferredPaymentMethod;
    private LocalDate targetSettlementDate;
    private String status;

    // Approval details
    private String approvedBy;
    private LocalDate approvalDate;
    private String approvalComments;

    // Rejection details
    private String rejectedBy;
    private LocalDate rejectionDate;
    private String rejectionReason;

    // Payment details (if settled)
    private EarlyRepaymentPaymentDto paymentDetails;

    // History
    private List<EarlyRepaymentHistoryDto> history;
}
















