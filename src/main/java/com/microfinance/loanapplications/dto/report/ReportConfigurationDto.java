// src/main/java/com/microfinance/loanapplications/dto/report/ReportConfigurationDto.java
package com.microfinance.loanapplications.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportConfigurationDto {
    private Long id;
    private String name;
    private String reportType;
    private String format;
    private LocalDateTime createdAt;
    private String parameters;
    private Long branchId;
    private String startDate;
    private String endDate;
    private String clientStatus;
    private String dataFields;
}