package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingApprovalDto {
    private Long id;
    private String applicationNumber;
    private String borrowerName;
    private String borrowerNumber;
    private String borrowerPhone;
    private String borrowerEmail;
    private String loanProductName;
    private String loanProductCode;
    private Double appliedAmount;
    private Integer tenureMonths;
    private String tenureUnit;
    private String purpose;
    private String purposeCategory;
    private LocalDateTime submittedDate;
    private Long daysSinceSubmission;
    private Integer currentApprovalLevel;
    private String status;
    private String branchName;
    private String branchCode;
    private Long branchId;
    private String createdBy;
    private LocalDateTime createdDate;
    private Double interestRate;
    private String interestType;
    private Double processingFee;
    private Double insuranceFee;
    private Integer previousLoans;
    private Double repaymentRate;
    private Integer riskScore;
    private String additionalNotes;
}









    

    

    
