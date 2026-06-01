package com.microfinance.loanapplications.dto;

import com.microfinance.loanapplications.dto.repayment.RepaymentDto;
import com.microfinance.loanapplications.dto.repayment.RepaymentScheduleDto;
import com.microfinance.loanapplications.dto.repayment.RepaymentScheduleSummaryDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LoanDto {
    private Long id;
    private String loanAccountNumber;
    private Long borrowerId;
    private String borrowerName;
    private String borrowerNumber;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private String status;
    private LocalDate disbursementDate;
    private LocalDate maturityDate;
    private LocalDate approvalDate;
    private LocalDate closedDate;
    private BigDecimal totalDue;
    private BigDecimal totalPaid;
    private BigDecimal outstandingBalance;
    private BigDecimal totalInterestDue;
    private Integer daysDelinquent;
    private BigDecimal penaltyAccrued;
    private List<RepaymentScheduleDto> repaymentSchedules;
    private List<RepaymentDto> recentRepayments;

    private String disbursementMethod;
    private String transactionReference;
    private String disbursementNotes;
    private BigDecimal netDisbursementAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long loanProductId;
    private String loanProductName;
    private Long branchId;
    private String branchName;
    private String disbursedByName;
    private Long loanApplicationId;

    // Additional fields for UI
    private BigDecimal nextDueAmount;
    private LocalDate nextDueDate;
    private BigDecimal totalArrears;
    private Integer installmentsPaid;
    private Integer totalInstallments;
    private Double progressPercentage;

    // Collections (summary only)
    private List<RepaymentScheduleSummaryDto> upcomingInstallments;
    private RepaymentScheduleSummaryDto lastPayment;

    private BigDecimal WriteOffAmount;
    private String writeOffReason;
    private LocalDate writeOffDate;
    private String writtenOffBy;
    private String recoveryPlan;
    private String WriteOffStatus;

    public Long disbursedById;

    private String borrowerPhoneNumber;

    private  LocalDate lastPaymentDate;
    private BigDecimal  lastPaymentAmount;

}