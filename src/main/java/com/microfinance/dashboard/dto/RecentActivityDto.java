// dto/dashboard/RecentActivityDto.java
package com.microfinance.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RecentActivityDto {
    private Long id;
    private String icon;
    private String message;
    private LocalDateTime timestamp;
    private String severity;
    private String alert;
    private String type;
    private String description;
    private String referenceNumber;
    private String userName;
    private BigDecimal amount;
}