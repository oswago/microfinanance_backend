// dto/collection/FieldVisitDto.java
package com.microfinance.loanapplications.dto.collection;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class FieldVisitDto {
    private Long id;
    private String visitNumber;
    private Long loanId;
    private String loanNumber;
    private Long recoveryCaseId;
    private String borrowerName;
    private String borrowerPhone;
    private String borrowerAddress;
    private LocalDate visitDate;
    private LocalTime visitTime;
    private String visitAddress;
    private String purpose;
    private String status;
    private Long assignedOfficerId;
    private String assignedOfficerName;
    private String notes;
    private String outcome;
    private String completionNotes;
    private LocalDate completedDate;
    private Boolean notifyBorrower;
    private Boolean sendReminder;
    private Boolean reminderSent;
    private Boolean notificationSent;
    private String createdBy;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
}