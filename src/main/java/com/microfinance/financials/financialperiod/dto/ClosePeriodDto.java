// dto/ClosePeriodDto.java
package com.microfinance.financials.financialperiod.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClosePeriodDto {
    private Long periodId;
    private String notes;
    private Boolean runProvisions;
    private Boolean runAccruals;
}