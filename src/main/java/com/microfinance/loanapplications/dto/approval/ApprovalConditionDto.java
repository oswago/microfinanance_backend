package com.microfinance.loanapplications.dto.approval;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for approval conditions
 */
@Data
@Builder
public class ApprovalConditionDto {
    private String conditionType; // DOCUMENT_REQUIRED, GUARANTOR_REQUIRED, etc.
    private String description;
    private Boolean isMandatory;
    private String dueDate;
    private String status;
    private long id;
    private long applicationId;
    private LocalDate completedDate;
    private String  completedBy;
}