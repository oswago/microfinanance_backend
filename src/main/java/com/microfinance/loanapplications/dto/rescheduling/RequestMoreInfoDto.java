package com.microfinance.loanapplications.dto.rescheduling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestMoreInfoDto {
    
    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message cannot exceed 1000 characters")
    private String message;
    
    @Size(max = 255, message = "Subject cannot exceed 255 characters")
    private String subject;
    
    private Boolean requireDocuments;
    
    @Size(max = 100, message = "Document type cannot exceed 100 characters")
    private String requiredDocumentType;
    
    @Size(max = 50, message = "Due date cannot exceed 50 characters")
    private String dueDate;
    
    private Boolean sendNotification;
}