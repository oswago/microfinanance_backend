package com.microfinance.borrower.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KycWorkflowUpdateRequest {
        private String newState;
        private String assignedOfficerName;
        private LocalDateTime estimatedCompletion;
        private String notes;
    }