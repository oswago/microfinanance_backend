// dto/report/LegalStatisticsDto.java
package com.microfinance.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalStatisticsDto {
    
    private Integer totalLegalNotices;
    
    private Integer pendingNotices;
    
    private Integer sentNotices;
    
    private Integer acknowledgedNotices;
    
    private Integer compliedNotices;
    
    private Integer defaultedNotices;
    
    private Double complianceRate;
    
    private Integer totalCourtCases;
    
    private Integer activeCourtCases;
    
    private Integer resolvedCourtCases;
    
    private BigDecimal amountUnderLitigation;
    
    private Map<String, Integer> noticesByType;
    
    private Map<String, Integer> casesByStatus;
    
    private Map<String, Integer> casesByCourt;
    
    private LocalDate lastNoticeDate;
    
    private LocalDate lastCourtDate;
}