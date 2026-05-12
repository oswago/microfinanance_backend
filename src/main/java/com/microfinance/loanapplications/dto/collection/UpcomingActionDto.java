package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingActionDto {
    private Long id;
    private Long loanId;
    private String loanAccountNumber;
    private String borrowerName;
    private String actionType; // CALL, VISIT, FOLLOW_UP, MEETING
    private String title;
    private String description;
    private LocalDateTime scheduledTime;
    private String priority; // HIGH, MEDIUM, LOW
    private String status; // PENDING, COMPLETED, CANCELLED
    private Long assignedToId;
    private String assignedToName;
}