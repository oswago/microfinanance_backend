// dto/report/AuditReportDto.java
package com.microfinance.reports.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AuditReportDto {
    private LocalDateTime generatedAt;
    private String generatedBy;
    private String reportPeriod;
    
    // User Activity
    private Integer totalActiveUsers;
    private Integer totalLoginCount;
    private Integer failedLoginAttempts;
    private List<UserActivityDto> topActiveUsers;
    
    // System Activity
    private Integer totalTransactions;
    private Integer totalLoansCreated;
    private Integer totalRepaymentsProcessed;
    private Integer totalDisbursements;
    
    // Security Events
    private Integer totalSecurityEvents;
    private Integer criticalSecurityEvents;
    private List<SecurityEventDto> recentSecurityEvents;
    
    // Data Changes
    private Integer totalDataChanges;
    private List<DataChangeDto> recentDataChanges;
}