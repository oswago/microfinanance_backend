package com.microfinance.borrower.dto;

import com.microfinance.borrower.enums.KycWorkflowStep;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KycWorkflowStepStatusDto {
    private Long id;
    private KycWorkflowStep step;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String completedByName;
    private String notes;
    private LocalDateTime dueDate;
    private Boolean isRequired;
    private Boolean isOverdue;
    private Integer retryCount;
}