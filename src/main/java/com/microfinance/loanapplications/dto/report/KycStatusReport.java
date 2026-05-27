// src/main/java/com/microfinance/loanapplications/dto/report/KycStatusReport.java
package com.microfinance.loanapplications.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycStatusReport {
    private Integer totalClients;
    private KycStatusCount verified;
    private KycStatusCount pending;
    private KycStatusCount rejected;
    private KycStatusCount expired;
    private KycStatusCount notStarted;
    private List<KycTrend> kycCompletionTrend;
    private List<PendingDocumentByType> pendingDocumentsByType;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KycStatusCount {
        private Integer count;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KycTrend {
        private String date;
        private Integer verified;
        private Integer pending;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingDocumentByType {
        private String documentType;
        private Integer count;
    }
}