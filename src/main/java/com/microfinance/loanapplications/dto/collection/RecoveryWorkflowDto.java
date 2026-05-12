package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryWorkflowDto {
    private List<RecoveryCaseDto> cases;
    private List<RecoveryAgentDto> agents;
    private Map<String, StageStatisticsDto> stageStats;
}


