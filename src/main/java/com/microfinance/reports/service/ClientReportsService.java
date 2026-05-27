// src/main/java/com/microfinance/loanapplications/service/ClientReportsService.java
package com.microfinance.reports.service;

import com.microfinance.loanapplications.dto.report.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Map;

public interface ClientReportsService {
    
    ClientDemographicsReport getClientDemographics(Long branchId);
    
    KycStatusReport getKycStatusReport(Long branchId);
    
    PortfolioSummaryReport getPortfolioSummaryReport(Long branchId);
    
    GroupPerformanceReport getGroupPerformanceReport(Long branchId);
    
    ActivityReport getActivityReport(Long branchId, LocalDate startDate, LocalDate endDate);
    
    RiskAssessmentReport getRiskAssessmentReport(Long branchId);
    
    byte[] exportReport(String reportType, String format, Map<String, Object> params);
    
    Page<ReportHistoryDto> getRecentReports(Pageable pageable);

    ReportConfigurationDto saveReportConfiguration(SaveReportRequestDto request, Long userId);

    Page<ReportConfigurationDto> getSavedReports(Long userId, Pageable pageable);

    byte[] generateReportFromConfiguration(Long configId, Long userId, String format);

    void deleteReportConfiguration(Long configId, Long userId);

}