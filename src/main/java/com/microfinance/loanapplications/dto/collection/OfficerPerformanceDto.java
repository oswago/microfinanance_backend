package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficerPerformanceDto {
    private Long officerId;
    private String officerName;
    private Integer assignedLoans;
    private Integer resolvedLoans;
    private BigDecimal resolvedAmount;
    private Integer callsMade;
    private Integer visitsMade;
    private BigDecimal collectionRate;
}
