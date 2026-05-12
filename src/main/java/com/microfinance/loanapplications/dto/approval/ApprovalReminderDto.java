package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalReminderDto {
    private Long id;
    private Long applicationId;
    private String applicationNumber;
    private String borrowerName;
    private BigDecimal amount;
    private Long approverId;
    private String approverName;
    private String reminderType; // SLA, OVERDUE, PENDING, ESCALATION
    private String priority; // HIGH, MEDIUM, LOW
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime dueDate;
    private Boolean isOverdue;
    private Boolean isDismissed;
    private LocalDateTime dismissedAt;
    private String dismissedBy;
    private Integer reminderCount;
    private LocalDateTime lastSentAt;
}