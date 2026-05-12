package com.microfinance.loanapplications.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkApprovalResult {

    private Integer totalProcessed;
    private Integer successfulCount;
    private Integer failedCount;
    private List<Long> successfulApplicationIds;
    private List<BulkApprovalError> errors;
    private LocalDateTime processedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkApprovalError {
        private Long applicationId;
        private String errorMessage;
        private String errorCode;
        private String errorDetails;
        private LocalDateTime timestamp;
    }

    // Helper methods
    public Double getSuccessRate() {
        if (totalProcessed == null || totalProcessed == 0) {
            return 0.0;
        }
        return (double) successfulCount / totalProcessed * 100;
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}