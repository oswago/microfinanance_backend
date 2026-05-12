package com.microfinance.loanapplications.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanAuditDto {
    private Long id;
    private String action;
    private String entityType;
    private Long entityId;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String performedBy;
    private LocalDateTime performedAt;
    private String ipAddress;
    private String details;
}