package com.microfinance.loanapplications.dto.collection;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderScheduleDto {
    private Long id;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduledDate;
    
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime scheduledTime;
    
    private String reminderType;
    private String status;
    private Integer recipientCount;
    private String messageTemplate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    private String createdBy;
    private Long loanId;
    private String loanAccountNumber;
    private String borrowerName;
}