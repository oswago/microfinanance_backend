package com.microfinance.loanapplications.dto.approval;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationDto {
    
    @NotBlank(message = "Escalation reason is required")
    private String reason;
    
    @Builder.Default
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, URGENT
    
    private String escalateToRole; // Optional - specific role to escalate to
    
    @Builder.Default
    private boolean notifyAll = true;
    
    private String additionalNotes;
}