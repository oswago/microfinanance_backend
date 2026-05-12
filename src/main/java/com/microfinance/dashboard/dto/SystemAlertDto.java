// dto/dashboard/SystemAlertDto.java
package com.microfinance.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemAlertDto {
    private Long id;
    private String message;
    private String severity;
    private String icon;
}