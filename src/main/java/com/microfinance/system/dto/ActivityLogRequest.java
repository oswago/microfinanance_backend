// src/main/java/com/microfinance/system/dto/ActivityLogRequest.java
package com.microfinance.system.dto;

import com.microfinance.common.config.GeneralConfig;
import lombok.Data;

@Data
public class ActivityLogRequest {
    private Long borrowerId;
    private GeneralConfig.BorrowerActivityType activityType;
    private String description;
    private Long performedBy;
    private String ipAddress;
    private String userAgent;
    private Long groupId;
    private Long loanId;
    private Long documentId;
    private String oldValue;
    private String newValue;
}