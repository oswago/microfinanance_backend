package com.microfinance.loanapplications.dto.approval;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelegateApprovalDto {
    
    @NotNull(message = "Delegate user ID is required")
    private Long delegateTo;
    
    @NotBlank(message = "Delegation reason is required")
    private String reason;
    
    private String duration; // "4h", "1d", "3d", "1w"
    
    private LocalDateTime expiryDate;
    
    @Builder.Default
    private boolean keepDelegatePermissions = false;
    
    @Builder.Default
    private boolean notifyDelegate = true;
    
    private String additionalNotes;
}