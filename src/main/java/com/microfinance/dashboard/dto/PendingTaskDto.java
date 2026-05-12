// dto/dashboard/PendingTaskDto.java
package com.microfinance.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PendingTaskDto {
    private Long id;
    private String description;
    private Boolean completed;
    private String priority;
}