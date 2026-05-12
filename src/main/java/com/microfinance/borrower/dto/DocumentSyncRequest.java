package com.microfinance.borrower.dto;

import lombok.Data;

import java.util.List;

// Request/Response DTOs
    @Data
   public class DocumentSyncRequest {
        private List<Long> documentIds;
    }