// src/main/java/com/microfinance/loanapplications/dto/report/ActivityReport.java
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
public class ActivityReport {
    private Integer totalActivities;
    private List<ActivityByType> activitiesByType;
    private List<DailyActivityTrend> dailyActivityTrend;
    private List<RecentActivity> recentActivities;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityByType {
        private String type;
        private Integer count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyActivityTrend {
        private String date;
        private Integer count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private Long id;
        private String type;
        private String description;
        private String timestamp;
        private String performedBy;
    }
}