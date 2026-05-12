package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryCaseDto {
    private Long id;
    private String caseNumber;
    private Long loanId;
    private String loanNumber;
    private Long borrowerId;
    private String borrowerName;
    private String borrowerPhone;
    private String borrowerEmail;
    private String borrowerAddress;
    private BigDecimal outstandingAmount;
    private BigDecimal originalLoanAmount;
    private BigDecimal recoveredAmount;
    private BigDecimal remainingAmount;
    private Integer daysOverdue;
    private Integer daysInRecovery;
    private Integer recoveryRate;
    private String currentStage;
    private String status;
    private String priority;
    private Integer stageDuration;
    private Long assignedAgentId;
    private String assignedAgent;
    private LocalDateTime lastActivityDate;
    private String lastActivityType;
    private Integer contactAttempts;
    private Integer agentsInvolved;
    private List<String> completedStages;
    private List<CaseNoteDto> notes;
    private List<StageDateDto> stageDates;
    private LocalDateTime createdDate;
    private String loanProductName;
    private BigDecimal interestRate;
    private String branchName;

}






