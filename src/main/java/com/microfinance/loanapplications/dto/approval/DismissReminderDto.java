package com.microfinance.loanapplications.dto.approval;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for dismissing an approval reminder
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DismissReminderDto {
    
    /**
     * Reason for dismissing the reminder
     * Optional field - if not provided, a default reason will be used
     */
    @Size(max = 500, message = "Dismissal reason cannot exceed 500 characters")
    private String reason;
    
    /**
     * Whether to add this dismissal to the audit log
     * Default is true
     */
    @Builder.Default
    private boolean addToAuditLog = true;
    
    /**
     * Whether to send a confirmation notification
     * Default is false
     */
    @Builder.Default
    private boolean sendConfirmation = false;
    
    /**
     * Additional notes about the dismissal
     */
    @Size(max = 1000, message = "Additional notes cannot exceed 1000 characters")
    private String additionalNotes;
    
    /**
     * Whether to dismiss all reminders for this application
     * Default is false (only dismiss this specific reminder)
     */
    @Builder.Default
    private boolean dismissAllForApplication = false;
}