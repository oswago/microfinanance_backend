package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAssignmentRequestDto {
    private List<Long> loanIds;
    private Long assignToUserId;
    private String assignmentType; // INDIVIDUAL, BATCH
    private String priority;
    private LocalDate dueDate;
    private String notes;
}
