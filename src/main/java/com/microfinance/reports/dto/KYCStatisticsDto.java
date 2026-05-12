// dto/report/KYCStatisticsDto.java
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
public class KYCStatisticsDto {
    
    private Integer totalBorrowers;
    
    private Integer kycVerifiedCount;
    
    private Integer kycPendingCount;
    
    private Integer kycExpiredCount;
    
    private Integer kycRejectedCount;
    
    private Double kycComplianceRate;
    
    private Map<String, Integer> kycByDocumentType;
    
    private Map<String, Integer> kycByBranch;
    
    private LocalDate lastKycUpdate;
    
    private Integer pendingVerifications;
    
    private Integer expiringIn30Days;
}