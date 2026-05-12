// ReschedulingRequestDto.java
package com.microfinance.loanapplications.dto.rescheduling;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReschedulingRequestDto {
    private Long id;
    private String requestNumber;
    private Long loanId;
    private String loanNumber;
    private Long borrowerId;
    private String borrowerName;
    private String borrowerIdNumber;
    private String requestType;
    private String status;
    private String reason;
    
    // Current terms
    private BigDecimal currentMonthlyPayment;
    private Integer currentInstallments;
    private BigDecimal currentInterestRate;
    private BigDecimal currentTotalInterest;
    
    // Proposed terms
    private BigDecimal proposedMonthlyPayment;
    private Integer proposedInstallments;
    private BigDecimal proposedInterestRate;
    private BigDecimal proposedTotalInterest;
    
    // Differences
    private BigDecimal paymentDifference;
    private Integer installmentDifference;
    private BigDecimal interestDifference;
    
    // Request details
    private Integer additionalMonths;
    private BigDecimal reducedPayment;
    private Integer holidayMonths;
    private LocalDate resumeDate;
    private String additionalNotes;
    
    // Review details
    private String reviewedBy;
    private LocalDateTime reviewDate;
    private String reviewComments;
    private String rejectionReason;
    
    // Audit
    private String requestedBy;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime requestDate;
    private LocalDateTime approvedDate;
    private LocalDateTime rejectedDate;
    
    // Documents
    private List<ReschedulingDocumentDto> documents;
    
    // Loan details
    private BigDecimal loanAmount;
    private BigDecimal outstandingBalance;
    private Integer daysOverdue;
    private Long branchId;
    private String branchName;
}





