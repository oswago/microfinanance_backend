package com.microfinance.borrower.dto;

import lombok.Data;

@Data
public class StepUpdateRequest {
        private String newStatus;
        private String notes;
    }