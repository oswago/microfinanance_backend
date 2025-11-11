package com.microfinance.borrower.dto;

import com.microfinance.borrower.enums.KycWorkflowState;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KycWorkflowHistoryDto {
    private Long id;
    private KycWorkflowState fromState;
    private KycWorkflowState toState;
    private String actionPerformed;
    private String performedByName;
    private String notes;
    private LocalDateTime transitionDate;
}