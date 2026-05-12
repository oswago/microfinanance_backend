// dto/audit/AuditStatisticsDto.java
package com.microfinance.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditStatisticsDto {
    
    private Long totalLogs;
    private Long totalLogsInPeriod;
    private Map<String, Long> logsByAction;
    private Map<String, Long> logsByEntityType;
    private Map<String, Long> logsBySeverity;
    private Map<String, Long> logsByUser;
    private Map<String, Long> logsByHour;
    private Long averageResponseTime;
    private Long peakHourActivity;
    private LocalDateTime lastLogTimestamp;
    private String mostActiveUser;
    private String mostCommonAction;
}