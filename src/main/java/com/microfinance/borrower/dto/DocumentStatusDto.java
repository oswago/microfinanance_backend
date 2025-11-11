package com.microfinance.borrower.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
class DocumentStatusDto {
    private String documentType;
    private String status;
    private Boolean isRequired;
    private LocalDateTime verifiedAt;
}