// src/main/java/com/microfinance/reports/repository/ReportConfigurationRepository.java
package com.microfinance.reports.repository;

import com.microfinance.reports.entity.ReportConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportConfigurationRepository extends JpaRepository<ReportConfiguration, Long> {
    
    Page<ReportConfiguration> findByCreatedByOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<ReportConfiguration> findByCreatedByAndStatus(Long userId, String status);
    
    Page<ReportConfiguration> findByCreatedByAndReportTypeOrderByCreatedAtDesc(Long userId, String reportType, Pageable pageable);
}