// dto/report/ComplianceReportDto.java
package com.microfinance.reports.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ComplianceReportDto {
    private LocalDate reportDate;
    private String reportingPeriod;
    
    // Regulatory Compliance
    private BigDecimal capitalAdequacyRatio;
    private BigDecimal liquidityRatio;
    private BigDecimal nonPerformingLoanRatio;
    private BigDecimal provisioningRatio;
    
    // KYC Compliance
    private Integer totalBorrowers;
    private Integer kycVerifiedCount;
    private Integer kycPendingCount;
    private Integer kycExpiredCount;
    private Double kycComplianceRate;
    
    // Interest Rate Compliance
    private BigDecimal averageInterestRate;
    private BigDecimal maxInterestRate;
    private BigDecimal minInterestRate;
    private Integer loansExceedingRateCap;
    
    // Data Protection
    private Boolean gdprCompliant;
    private LocalDate lastAuditDate;
    private Integer dataBreachIncidents;
    
    // Legal Actions
    private Integer pendingLegalCases;
    private Integer resolvedLegalCases;
    private BigDecimal amountUnderLitigation;
}