// BulkDisbursementResponseDto.java
package com.microfinance.loanapplications.dto.disbursement;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDisbursementResponseDto {
    private String batchReference;
    private int totalProcessed;
    private int successful;
    private int failed;
    private List<BulkDisbursementResult> results;
    private LocalDateTime processedAt;
}

