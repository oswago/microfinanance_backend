package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SLAStatusDto {
    private Long applicationId;
    private String applicationNumber;
    private String slaLevel;
    private LocalDateTime slaStartDate;
    private LocalDateTime slaDueDate;
    private Long hoursRemaining;
    private Long hoursElapsed;
    private String status; // ON_TRACK, AT_RISK, BREACHED
    private Double completionPercentage;
    private String nextAction;
    private LocalDateTime nextActionDue;
     private boolean Breached;


}