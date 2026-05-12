package com.microfinance.loanapplications.dto.collection;

import com.microfinance.common.config.GeneralConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordCollectionActionDto {
    private Long loanId;
    private GeneralConfig.ActionType actionType;
    private LocalDate actionDate;
    private LocalTime actionTime;
    
    // Contact Information
    private String contactPerson;
    private String contactNumber;
    private GeneralConfig.ContactMethod contactMethod;
    
    // Outcome
    private GeneralConfig.Outcome outcome;
    private String notes;
    private LocalDate followUpDate;
    private LocalTime followUpTime;
    private GeneralConfig.FollowUpAction followUpAction;
    
    // Promise to Pay
    private BigDecimal promiseAmount;
    private LocalDate promiseDate;
    
    // Assignment
    private Long assignedToId;
    
    // Location (for field visits)
    private BigDecimal visitLatitude;
    private BigDecimal visitLongitude;
    private String visitAddress;
    
    // Attachments
    private MultipartFile attachment;
    private MultipartFile recording;
}