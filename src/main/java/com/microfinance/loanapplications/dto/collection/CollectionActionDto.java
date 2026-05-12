package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionActionDto {
    private Long id;
    private Long loanId;
    private String loanAccountNumber;
    private String borrowerName;
    private String actionType;
    private String outcome;
    private LocalTime actionTime;
    private String notes;
    private LocalDate followUpDate;
    private String performedBy;
    private LocalDateTime createdAt;
    private String actionStatus;
    private LocalDate actionDate;
    private String contactPerson;
    private String contactNumber;
    private String contactMethod;
    private String followUpAction;
    private BigDecimal promiseAmount;
    private LocalDate promiseDate;
    private Boolean paymentConfirmed;
    private Long assignedToId;
    private String assignedToName;
    private Long performedById;
    private String performedByName;
    private BigDecimal visitLatitude;
    private BigDecimal visitLongitude;
    private String visitAddress;
    private String attachmentUrl;
    private String recordingUrl;
}