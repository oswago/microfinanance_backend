// dto/report/ReportStatisticsDto.java
package com.microfinance.reports.dto;

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
public class ReportStatisticsDto {
    
    private Integer totalReportsGenerated;
    
    private String mostViewedReport;
    
    private Double averageReportGenerationTime; // in seconds
    
    private Map<String, Integer> reportsByType;
    
    private Map<String, Integer> reportsByUser;
    
    private Map<String, Integer> reportsByDate;
    
    private LocalDateTime lastReportGeneratedAt;
    
    private String lastReportGeneratedBy;
    
    private Integer totalExports;
    
    private Map<String, Integer> exportsByFormat; // PDF, EXCEL, CSV
}