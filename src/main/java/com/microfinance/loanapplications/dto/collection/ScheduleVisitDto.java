// dto/collection/ScheduleVisitDto.java
package com.microfinance.loanapplications.dto.collection;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ScheduleVisitDto {
    
    private Long loanId;
    
    private Long recoveryCaseId;
    
    @NotNull(message = "Visit date is required")
    @FutureOrPresent(message = "Visit date must be today or in the future")
    private LocalDate visitDate;
    
    private LocalTime visitTime;
    
    private String visitAddress;
    
    @NotBlank(message = "Purpose is required")
    private String purpose;
    
    private Long assignedOfficerId;
    
    private String notes;
    
    private Boolean notifyBorrower = true;
    
    private Boolean sendReminder = true;
}