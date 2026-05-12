// RecentReportDto.java
package com.microfinance.loanapplications.dto.earlyrepayment;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentReportDto {
    private String reportName;
    private LocalDateTime generatedDate;
    private String format;
    private String generatedBy;
    private String reportType;
    private String downloadUrl;
}