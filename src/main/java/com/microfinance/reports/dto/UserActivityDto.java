// dto/report/UserActivityDto.java
package com.microfinance.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityDto {
    
    private Long userId;
    
    private String username;
    
    private String fullName;
    
    private String email;
    
    private String role;
    
    private Integer actionCount;
    
    private LocalDateTime lastActive;
    
    private LocalDateTime firstActive;
    
    private String lastAction;
    
    private String lastIpAddress;
    
    private Double averageActionsPerDay;
    
    private Integer uniqueSessions;
}