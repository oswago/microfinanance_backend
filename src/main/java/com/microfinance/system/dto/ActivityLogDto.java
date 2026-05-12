// src/main/java/com/microfinance/system/dto/ActivityLogDto.java
package com.microfinance.system.dto;

import com.microfinance.common.config.GeneralConfig;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityLogDto {
    private Long id;
    private Long borrowerId;
    private String borrowerName;
    private String borrowerNumber;
    private GeneralConfig.BorrowerActivityType activityType;
    private String description;
    private Long performedBy;
    private String performedByName;
    private String ipAddress;
    private LocalDateTime activityDate;
    private Long groupId;
    private String groupName;
    private Long loanId;
    private String loanNumber;
    private Long documentId;
    private String oldValue;
    private String newValue;
    
    // Helper methods for frontend display
    public String getActivityTypeDisplay() {
        return activityType.name().replace("_", " ");
    }
    
    public String getFormattedActivityDate() {
        return activityDate.toString(); // You can format this as needed
    }
}