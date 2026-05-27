// src/main/java/com/microfinance/loanapplications/dto/report/ReportHistoryDto.java
package com.microfinance.loanapplications.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportHistoryDto {
    private Long id;
    private String name;
    private String type;
    private String generatedAt;
    private String fileSize;
    private String status;
}