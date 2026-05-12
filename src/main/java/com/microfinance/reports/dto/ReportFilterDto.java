// dto/report/ReportFilterDto.java
package com.microfinance.reports.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
@Builder
@Data
public class ReportFilterDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private String period; // DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
    private Long branchId;
    private String productType;
    private String format; // PDF, EXCEL, CSV
}