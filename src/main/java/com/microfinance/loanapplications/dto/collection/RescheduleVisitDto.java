// dto/collection/RescheduleVisitDto.java
package com.microfinance.loanapplications.dto.collection;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class RescheduleVisitDto {
    
    @NotNull(message = "New date is required")
    @Future(message = "New date must be in the future")
    private LocalDate newDate;
    
    private LocalTime newTime;
    
    private String reason;
}