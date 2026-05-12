package com.microfinance.loanapplications.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApplicationValidationResult {
    private Boolean requirementsMet;
    private String message;
    private LocalDateTime checkedAt;
}