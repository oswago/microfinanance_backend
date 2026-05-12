// dto/report/SecurityEventDto.java
package com.microfinance.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
public class SecurityEventDto {
    
    private Long id;
    
    private String eventType;
    
    private String severity; // INFO, WARNING, ERROR, CRITICAL
    
    private String description;
    
    private LocalDateTime timestamp;
    
    private Long userId;
    
    private String username;
    
    private String ipAddress;
    
    private String userAgent;
    
    private String resourceAccessed;
    
    private String additionalDetails;

    public SecurityEventDto(Long id, String action, String severity, String details,
                            LocalDateTime timestamp, Long userId, String username,
                            String ipAddress, String userAgent, String resource, String additionalDetails) {
        this.id = id;
        this.eventType = action;
        this.severity = severity;
        this.description = details;
        this.timestamp = timestamp;
        this.userId = userId;
        this.username = username;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.resourceAccessed = resource;
        this.additionalDetails = additionalDetails;
    }

}