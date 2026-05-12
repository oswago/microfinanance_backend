// controller/DashboardController.java
package com.microfinance.dashboard.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.dashboard.dto.*;
import com.microfinance.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        log.info("Fetching dashboard statistics");
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        DashboardStatsDto stats = dashboardService.getDashboardStats(currentUser);
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/portfolio-distribution")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PortfolioDistributionDto> getPortfolioDistribution() {
        log.info("Fetching portfolio distribution");
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        PortfolioDistributionDto distribution = dashboardService.getPortfolioDistribution(currentUser);
        
        return ResponseEntity.ok(distribution);
    }

    @GetMapping("/repayment-performance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RepaymentPerformanceDto> getRepaymentPerformance() {
        log.info("Fetching repayment performance");
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        RepaymentPerformanceDto performance = dashboardService.getRepaymentPerformance(currentUser);
        
        return ResponseEntity.ok(performance);
    }

    @GetMapping("/recent-activities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RecentActivityDto>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Fetching recent activities, limit: {}", limit);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        List<RecentActivityDto> activities = dashboardService.getRecentActivities(limit, currentUser);
        
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/pending-tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PendingTaskDto>> getPendingTasks() {
        log.info("Fetching pending tasks");
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        List<PendingTaskDto> tasks = dashboardService.getPendingTasks(currentUser);
        
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/system-alerts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SystemAlertDto>> getSystemAlerts() {
        log.info("Fetching system alerts");
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        List<SystemAlertDto> alerts = dashboardService.getSystemAlerts(currentUser);
        
        return ResponseEntity.ok(alerts);
    }

    @PostMapping("/alerts/{alertId}/dismiss")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> dismissAlert(@PathVariable Long alertId) {
        log.info("Dismissing alert: {}", alertId);
        
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        dashboardService.dismissAlert(alertId, currentUser);
        
        return ResponseEntity.ok().build();
    }

    // NEW ENDPOINTS
    @GetMapping("/loan-stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<LoanStatsDTO> getLoanStats() {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(dashboardService.getLoanStats(currentUser));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary() {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(dashboardService.getDashboardSummary(currentUser));
    }

    @GetMapping("/upcoming-payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<List<Map<String, Object>>> getUpcomingPayments(
            @RequestParam(defaultValue = "7") int days) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(dashboardService.getUpcomingPayments(days, currentUser));
    }

}