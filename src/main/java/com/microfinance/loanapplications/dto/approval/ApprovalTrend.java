package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class ApprovalTrend {
        private String period;
        private Long approved;
        private Long rejected;
        private Long pending;
        private Double approvalRate;
    }