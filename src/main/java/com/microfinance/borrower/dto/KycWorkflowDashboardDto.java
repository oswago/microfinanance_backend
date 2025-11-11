package com.microfinance.borrower.dto;

import lombok.Data;

import java.util.Map;

@Data
public class KycWorkflowDashboardDto {
    private Map<String, Long> stateCounts;
    private Long activeWorkflows;
    private Integer overdueWorkflows;
    private Double averageCompletionTime;
    private Integer workflowsNeedingAttention;
    private Integer completedThisWeek;
    private Integer inProgressWorkflows;
}