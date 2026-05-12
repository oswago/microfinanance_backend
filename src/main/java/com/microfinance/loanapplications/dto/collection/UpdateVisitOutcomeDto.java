// dto/collection/UpdateVisitOutcomeDto.java
package com.microfinance.loanapplications.dto.collection;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateVisitOutcomeDto {
    
    @NotBlank(message = "Status is required")
    private String status; // COMPLETED, CANCELLED, RESCHEDULED
    
    @NotBlank(message = "Outcome is required")
    private String outcome; // SUCCESSFUL, UNSUCCESSFUL, PARTIAL, POSTPONED
    
    private String completionNotes;
    
    private LocalDate completedDate;
}