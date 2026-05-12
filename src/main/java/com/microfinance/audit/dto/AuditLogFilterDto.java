// dto/audit/AuditLogFilterDto.java
package com.microfinance.audit.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogFilterDto {
    
    private String entityType;
    private Long entityId;
    private String action;
    private Long userId;
    private String severity;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String searchTerm;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}