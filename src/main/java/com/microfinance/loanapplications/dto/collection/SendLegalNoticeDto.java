// dto/collection/SendLegalNoticeDto.java
package com.microfinance.loanapplications.dto.collection;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class SendLegalNoticeDto {
    
    private Long loanId;
    
    private Long recoveryCaseId;
    
    @NotBlank(message = "Notice type is required")
    private String noticeType;
    
    @NotNull(message = "Notice date is required")
    private LocalDate noticeDate;
    
    @NotNull(message = "Compliance date is required")
    @Future(message = "Compliance date must be in the future")
    private LocalDate complianceDate;
    
    @NotBlank(message = "Reason is required")
    private String reason;
    
    private String legalGrounds;
    
    private Long assignedOfficerId;
    
    private String additionalNotes;
    
    @NotBlank(message = "Delivery method is required")
    private String deliveryMethod;
    
    private Boolean generateDocument = true;
    
    private Boolean notifyLegalTeam = true;
    
    private Boolean attachToCase = true;
}