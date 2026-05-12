package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
    @AllArgsConstructor
    public  class AmountMetrics {
        private double totalApprovedAmount;
        private double averageApprovedAmount;
        private double largestApprovedAmount;
        private double smallestApprovedAmount;
    }