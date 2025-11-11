package com.microfinance.borrower.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityTimelineDto {
    private LocalDate date;
    private List<BorrowerActivityDto> activities;
    private Integer activityCount;
    
    @Data
    public static class TimelineGroup {
        private String period; // "Today", "Yesterday", "Last Week", etc.
        private List<ActivityTimelineDto> dailyActivities;
        private Integer totalActivities;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime earliestActivity;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime latestActivity;
    }
}