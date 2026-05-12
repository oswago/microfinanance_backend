// service/DashboardService.java
package com.microfinance.dashboard.service;

import com.microfinance.base.entity.User;
import com.microfinance.dashboard.dto.*;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Map;

public interface DashboardService {
    
    DashboardStatsDto getDashboardStats(User currentUser);
    
    PortfolioDistributionDto getPortfolioDistribution(User currentUser);
    
    RepaymentPerformanceDto getRepaymentPerformance(User currentUser);
    
    List<RecentActivityDto> getRecentActivities(int limit, User currentUser);
    
    List<PendingTaskDto> getPendingTasks(User currentUser);
    
    List<SystemAlertDto> getSystemAlerts(User currentUser);
    
    void dismissAlert(Long alertId, User currentUser);

    @Transactional(readOnly = true)
    LoanStatsDTO getLoanStats(User currentUser);

    @Transactional(readOnly = true)
    DashboardSummaryDTO getDashboardSummary(User currentUser);

    @Transactional(readOnly = true)
    List<Map<String, Object>> getUpcomingPayments(int days, User currentUser);
}