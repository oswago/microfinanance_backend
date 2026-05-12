// dto/dashboard/PortfolioDistributionDto.java
package com.microfinance.dashboard.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PortfolioDistributionDto {
    private List<String> labels;
    private List<Double> values;
}