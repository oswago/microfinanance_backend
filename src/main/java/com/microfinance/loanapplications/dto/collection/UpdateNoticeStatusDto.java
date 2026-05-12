// dto/collection/UpdateNoticeStatusDto.java
package com.microfinance.loanapplications.dto.collection;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateNoticeStatusDto {
    
    @NotBlank(message = "Status is required")
    private String status; // SENT, ACKNOWLEDGED, COMPLIED, DEFAULTED, CANCELLED
    
    private LocalDate acknowledgedDate;
    
    private String acknowledgedBy;
    
    private String acknowledgementNotes;
}