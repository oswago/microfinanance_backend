package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class ProductApprovalStats {
        private String productName;
        private Long totalApplications;
        private Long approved;
        private Long rejected;
        private Double approvalRate;
        private Double avgProcessingTime;
    }