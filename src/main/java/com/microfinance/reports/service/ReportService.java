// service/ReportService.java
package com.microfinance.reports.service;

import com.microfinance.base.entity.User;
import com.microfinance.reports.dto.*;

import java.util.Map;


public interface ReportService {
    
    FinancialReportDto generateFinancialReport(ReportFilterDto filter, User currentUser);
    
    PortfolioReportDto generatePortfolioReport(ReportFilterDto filter, User currentUser);
    
    ComplianceReportDto generateComplianceReport(ReportFilterDto filter, User currentUser);
    
    AuditReportDto generateAuditReport(ReportFilterDto filter, User currentUser);
    
    byte[] exportReport(String reportType, ReportFilterDto filter, String format, User currentUser);
    
    java.util.Map<String, Object> getReportStatistics(User currentUser);

    Map<String, Object> getReportStatistics(ReportFilterDto filter, User currentUser);
}