package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStatsDto {
    private Long pending;
    private Long approvedToday;
    private Long rejectedToday;
    private Long returnedToday;
    private Long avgProcessingTime;
    private Long totalProcessed;
    private Double onTimeCompletionRate;
    private LocalDate reportDate;
    private Long approverId;
}