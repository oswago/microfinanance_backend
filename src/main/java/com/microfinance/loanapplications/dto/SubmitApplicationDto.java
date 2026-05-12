package com.microfinance.loanapplications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitApplicationDto {
    @NotBlank(message = "Officer comments are required")
    @Size(max = 1000, message = "Comments cannot exceed 1000 characters")
    private String officerComments;
    
    private Integer creditScore;
    private String riskLevel;
    private Boolean recommendedForApproval;
    private String purpose;
    private String additionalNotes;
}