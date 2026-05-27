// src/main/java/com/microfinance/loanapplications/dto/report/SaveReportRequestDto.java
package com.microfinance.loanapplications.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveReportRequestDto {
    private String name;
    private String reportType;
    private String format;
    private Long branchId;
    private String startDate;
    private String endDate;
    private String clientStatus;
    private java.util.List<String> dataFields;
    private java.util.Map<String, Object> additionalParams;
}