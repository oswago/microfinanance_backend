package com.microfinance.loanapplications.dto.disbursement;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;

@Data
@Builder
public class RescheduleStatisticsDto {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Long totalRequests;
    private Long approvedRequests;
    private Long rejectedRequests;
    private Double approvalRate;
    
    // Branch-wise breakdown can be added here
}