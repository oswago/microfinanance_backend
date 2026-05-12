package com.microfinance.borrower.dto;


import lombok.Data;

import java.util.List;
@Data
public class AutoCompleteResponse {
        private boolean success;
        private String message;
        private int stepsCompleted;
        private List<String> completedSteps;
    }