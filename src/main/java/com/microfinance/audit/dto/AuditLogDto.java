// dto/audit/AuditLogDto.java
package com.microfinance.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    
    private Long id;
    private String entityType;
    private Long entityId;
    private String action;
    private Long userId;
    private String username;
    private String ipAddress;
    private String userAgent;
    private String sessionId;
    private String details;
    private String oldValue;
    private String newValue;
    private String severity;
    private Long durationMs;
    private String resource;
    private String status;
    private LocalDateTime timestamp;
}