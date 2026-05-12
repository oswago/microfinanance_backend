package com.microfinance.loanapplications.dto.repayment;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
@Data
public class RepaymentScheduleDto {
    private Long id;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private BigDecimal principalDue;
    private BigDecimal interestDue;
    private BigDecimal totalDue;
    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
    private BigDecimal totalPaid;
    private String status;
    private BigDecimal outstandingAmount;
    private Boolean isOverdue;
    private Integer daysOverdue;
    private BigDecimal penaltyAccrued;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal paidAmount;
    private String branchName;
    private LocalDate maturityDate;
    private BigDecimal interestRate;
    private BigDecimal outstandingBalance;
    private Integer tenureMonths;
    private String loanAccountNumber;
    private BigDecimal outstandingPrincipal;
    private BigDecimal outstandingInterest;
    private BigDecimal totalOutstanding;
    private String paymentMethod;
    private String transactionReference;

    private boolean isFullyPaid;

    private String loanNumber;
    private String borrowerName;
    private String borrowerIdNumber;
    private String loanProductName;
    private String scheduleStatus;
    private LocalDate nextPaymentDate;
    private BigDecimal nextPaymentAmount;
    private Integer remainingInstallments;
    private Integer totalInstallments;
    private BigDecimal loanAmount;
    private BigDecimal totalInterest;
    private BigDecimal totalRepayable;
    private Integer paidInstallments;
    private String paymentFrequency;
    private BigDecimal totalOverdue;
    private LocalDate disbursementDate;
    private Long loanId;
    private Long branchId;
    private Long loanProductId;
    private List<InstallmentDto> installments;
}