package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class ApproverPerformance {
        private String approverName;
        private String approverRole;
        private Long totalDecisions;
        private Long approved;
        private Long rejected;
        private Double approvalRate;
        private Double avgProcessingTime;
        private Double onTimeRate;
    }
