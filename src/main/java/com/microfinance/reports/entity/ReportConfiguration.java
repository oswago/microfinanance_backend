// src/main/java/com/microfinance/reports/entity/ReportConfiguration.java
package com.microfinance.reports.entity;

import com.microfinance.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_configurations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportConfiguration extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String reportType; // demographics, kyc-status, portfolio, group-performance, activity, risk
    
    @Column(nullable = false)
    private String format; // pdf, csv, excel
    
    @Column(nullable = false)
    private Long createdBy; // User ID who created the report
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    // Store parameters as JSON
    @Column(columnDefinition = "TEXT")
    private String parameters; // JSON string of all report parameters
    
    private Long branchId;
    private String startDate;
    private String endDate;
    private String clientStatus;
    private String dataFields; // JSON array of selected fields
    
    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, DELETED
}