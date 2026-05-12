package com.microfinance.borrower.dto;

import lombok.Data;

@Data
   public  class AutoProgressResponse {
        private boolean progressed;
        private String fromState;
        private String toState;
        private String message;
    }