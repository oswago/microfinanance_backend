package com.microfinance.borrower.dto;

import lombok.Data;

import java.util.List;
@Data
public class DocumentSyncResponse {
        private boolean success;
        private String message;
        private int stepsCompleted;
        private int stepsSkipped;
        private List<String> completedStepNames;
    }