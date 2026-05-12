package com.microfinance.loanapplications.controller;

import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.loanapplications.dto.collection.*;
import com.microfinance.loanapplications.dto.repayment.OverdueInstallmentDto;
import com.microfinance.loanapplications.service.CollectionService;
import com.microfinance.loanapplications.service.LoanService;
import com.microfinance.base.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final LoanService loanService;
    private final SecurityUtils securityUtils;
    private final UserService userService;
    private final CollectionService collectionService;






    /**
     * Get upcoming collection actions
     */
    @GetMapping("/upcoming-actions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<UpcomingActionDto>> getUpcomingCollectionActions(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long loanOfficerId) {

        log.info("Fetching upcoming collection actions, limit: {}, branch: {}, officer: {}",
                limit, branchId, loanOfficerId);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        List<UpcomingActionDto> actions = collectionService.getUpcomingCollectionActions(
                limit, branchId, loanOfficerId, currentUser);

        return ResponseEntity.ok(actions);
    }

    /**
     * Record a collection action (call, visit, etc.)
     */
    @PostMapping("/actions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'COLLECTION_OFFICER') and " +
            " @permissionCheckService.hasPermission('COLLECTION_ACTION_RECORD')")
    public ResponseEntity<CollectionActionDto> recordCollectionAction(
            @Valid @RequestBody RecordCollectionActionDto actionDto) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Recording collection action for loan {} by user: {}",
                actionDto.getLoanId(), currentUser.getUsername());

        CollectionActionDto result = collectionService.recordCollectionAction(actionDto, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * Get collection actions history for a loan
     */
    @GetMapping("/actions/{loanId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<CollectionActionDto>> getCollectionActions(@PathVariable Long loanId) {

        log.info("Fetching collection actions for loan: {}", loanId);

        List<CollectionActionDto> actions = collectionService.getCollectionActions(loanId);
        return ResponseEntity.ok(actions);
    }

    /**
     * Send bulk reminders to overdue clients
     */
    @PostMapping("/send-reminders")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER') and  @permissionCheckService.hasPermission('COLLECTION_BULK_ACTION')")
    public ResponseEntity<BulkReminderResultDto> sendBulkReminders(
            @Valid @RequestBody BulkReminderRequestDto request) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Sending bulk reminders by user: {}", currentUser.getUsername());

        BulkReminderResultDto result = collectionService.sendBulkReminders(request, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * Assign collection tasks to officers
     */
    @PostMapping("/assign-tasks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER') and  @permissionCheckService.hasPermission('COLLECTION_ASSIGN')")
    public ResponseEntity<TaskAssignmentResultDto> assignCollectionTasks(
            @Valid @RequestBody TaskAssignmentRequestDto request) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Assigning collection tasks by user: {}", currentUser.getUsername());

        TaskAssignmentResultDto result = collectionService.assignCollectionTasks(request, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * Generate collection report
     */
    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<CollectionReportDto> generateCollectionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long loanOfficerId) {

        log.info("Generating collection report from {} to {}, branch: {}, officer: {}",
                startDate, endDate, branchId, loanOfficerId);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        CollectionReportDto report = collectionService.generateCollectionReport(
                startDate, endDate, branchId, loanOfficerId, currentUser);

        return ResponseEntity.ok(report);
    }


    @GetMapping("/report/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> exportCollectionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long loanOfficerId,
            @RequestParam(defaultValue = "PDF") String format) {

        log.info("Exporting collection report from {} to {} in format: {}", startDate, endDate, format);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());

        // Get the report data
        CollectionReportDto report = collectionService.generateCollectionReport(
                startDate, endDate, branchId, loanOfficerId, currentUser);

        // Generate PDF using collection service
        byte[] pdfContent = collectionService.exportCollectionReport(report, format);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("collection-report-" + startDate + "-to-" + endDate + ".pdf")
                .build());

        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }

    @PostMapping("/schedule-reminders")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER') and hasPermission('COLLECTION_SEND_REMINDERS')")
    public ResponseEntity<Void> scheduleBulkReminders(@RequestBody ScheduleReminderRequestDto request) {
        log.info("Scheduling bulk reminders for date: {}, type: {}",
                request.getReminderDate(), request.getReminderType());
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        collectionService.scheduleBulkReminders(request, currentUser);

        return ResponseEntity.ok().build();
    }

        // ==================== New Performance Endpoints ====================

        /**
         * Get collection performance data (summary + officer performance + trends)
         */
        @GetMapping("/performance")
        @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT', 'COLLECTION_OFFICER')")
        public ResponseEntity<CollectionPerformanceDto> getCollectionPerformance(
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                @RequestParam(required = false) Long branchId,
                @RequestParam(required = false) Long officerId) {

            log.info("Fetching collection performance from {} to {}, branch: {}, officer: {}",
                    startDate, endDate, branchId, officerId);

            User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
            CollectionPerformanceDto performance = collectionService.getCollectionPerformance(
                    startDate, endDate, branchId, officerId, currentUser);

            return ResponseEntity.ok(performance);
        }

        /**
         * Export performance report
         */
        @GetMapping("/performance/export")
        @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
        public ResponseEntity<byte[]> exportPerformanceReport(
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                @RequestParam(required = false) Long branchId,
                @RequestParam(required = false) Long officerId,
                @RequestParam(defaultValue = "PDF") String format) {

            log.info("Exporting performance report from {} to {}, branch: {}, officer: {}, format: {}",
                    startDate, endDate, branchId, officerId, format);

            User currentUser = userService.getUserById(securityUtils.getCurrentUserId());

            // Get performance data
            CollectionPerformanceDto performance = collectionService.getCollectionPerformance(
                    startDate, endDate, branchId, officerId, currentUser);

            // Generate export
            byte[] reportContent = collectionService.exportPerformanceReport(performance, format);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("performance-report-" + startDate + "-to-" + endDate + ".pdf")
                    .build());

            return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
        }

        /**
         * Get officer performance details
         */
        @GetMapping("/performance/officer/{officerId}")
        @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
        public ResponseEntity<OfficerPerformanceDetailDto> getOfficerPerformanceDetails(
                @PathVariable Long officerId,
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

            log.info("Fetching performance details for officer: {} from {} to {}",
                    officerId, startDate, endDate);

            User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
            OfficerPerformanceDetailDto details = collectionService.getOfficerPerformanceDetails(
                    officerId, startDate, endDate, currentUser);

            return ResponseEntity.ok(details);
        }

        /**
         * Get performance summary (for dashboard widgets)
         */
        @GetMapping("/performance/summary")
        @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
        public ResponseEntity<PerformanceSummaryDto> getPerformanceSummary(
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                @RequestParam(required = false) Long branchId) {

            log.info("Fetching performance summary from {} to {}, branch: {}", startDate, endDate, branchId);

            User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
            PerformanceSummaryDto summary = collectionService.getPerformanceSummary(
                    startDate, endDate, branchId, currentUser);

            return ResponseEntity.ok(summary);
        }

        /**
         * Get daily collection trends
         */
        @GetMapping("/performance/trends")
        @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
        public ResponseEntity<List<DailyCollectionTrendDto>> getCollectionTrends(
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                @RequestParam(required = false) Long branchId) {

            log.info("Fetching collection trends from {} to {}, branch: {}", startDate, endDate, branchId);

            User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
            List<DailyCollectionTrendDto> trends = collectionService.getCollectionTrends(
                    startDate, endDate, branchId, currentUser);

            return ResponseEntity.ok(trends);
        }


        /**
         * Get overdue installments for collection actions
         */
        @GetMapping("/overdue-installments")
        @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'COLLECTION_OFFICER')")
        public ResponseEntity<List<OverdueInstallmentDto>> getOverdueInstallments(
                @RequestParam(required = false) Long branchId,
                @RequestParam(required = false) Integer minDaysOverdue,
                @RequestParam(required = false) Integer maxDaysOverdue) {

            log.info("Fetching overdue installments - branch: {}, minDays: {}, maxDays: {}",
                    branchId, minDaysOverdue, maxDaysOverdue);

            User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
            List<OverdueInstallmentDto> installments = collectionService.getOverdueInstallments(
                    branchId, minDaysOverdue, maxDaysOverdue, currentUser);

            return ResponseEntity.ok(installments);
        }

        /**
         * Get collection statistics for dashboard
         */
        @GetMapping("/stats")
        @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'COLLECTION_OFFICER', 'ACCOUNTANT')")
        public ResponseEntity<CollectionActionStatsDto> getCollectionActionStats(
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                @RequestParam(required = false) Long branchId) {

            log.info("Fetching collection action stats for date: {}, branch: {}", date, branchId);

            User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
            CollectionActionStatsDto stats = collectionService.getCollectionActionStats(
                    date, branchId, currentUser);

            return ResponseEntity.ok(stats);
        }



    /**
     * Get collection statistics (summary data for dashboard)
     */
    @GetMapping("/stats_bk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'COLLECTION_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<CollectionStatisticsDto> getCollectionStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long branchId) {

        log.info("Fetching collection statistics for date: {}, branch: {}", date, branchId);

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        CollectionStatisticsDto stats = collectionService.getCollectionStatistics(date, branchId, currentUser);

        return ResponseEntity.ok(stats);
    }


        /**
         * Log a phone call action
         */
        @PostMapping("/log-phone-call")
        @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'COLLECTION_OFFICER') and " +
                "@permissionCheckService.hasPermission('COLLECTION_ACTION_RECORD')")
        public ResponseEntity<CollectionActionDto> logPhoneCall(
                @Valid @RequestBody LogPhoneCallDto phoneCallDto) {

            User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
            log.info("Logging phone call for loan {} by user: {}",
                    phoneCallDto.getLoanId(), currentUser.getUsername());

            CollectionActionDto result = collectionService.logPhoneCall(phoneCallDto, currentUser);
            return ResponseEntity.ok(result);
        }

        /**
         * Apply penalty to an overdue loan
         */
        @PostMapping("/apply-penalty")
        @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'COLLECTION_OFFICER') and " +
                "@permissionCheckService.hasPermission('COLLECTION_ACTION_RECORD')")
        public ResponseEntity<PenaltyResultDto> applyPenalty(
                @Valid @RequestBody ApplyPenaltyDto penaltyDto) {

            User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
            log.info("Applying penalty of {} to loan {} by user: {}",
                    penaltyDto.getAmount(), penaltyDto.getLoanId(), currentUser.getUsername());

            PenaltyResultDto result = collectionService.applyPenalty(penaltyDto, currentUser);
            return ResponseEntity.ok(result);
        }

        /**
         * Escalate a collection case
         */
        @PostMapping("/escalate-case")
        @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER') and " +
                "@permissionCheckService.hasPermission('COLLECTION_ESCALATE')")
        public ResponseEntity<EscalationResultDto> escalateCase(
                @Valid @RequestBody EscalateCaseDto escalateDto) {

            User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
            log.info("Escalating case for loan {} by user: {}",
                    escalateDto.getLoanId(), currentUser.getUsername());

            EscalationResultDto result = collectionService.escalateCase(escalateDto, currentUser);
            return ResponseEntity.ok(result);
        }

        /**
         * Get recent collection activities
         */
        @GetMapping("/recent-activities")
        @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'COLLECTION_OFFICER')")
        public ResponseEntity<List<ActivityDto>> getRecentActivities(
                @RequestParam(defaultValue = "10") int limit,
                @RequestParam(required = false) Long branchId) {

            log.info("Fetching recent activities, limit: {}, branch: {}", limit, branchId);

            User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
            List<ActivityDto> activities = collectionService.getRecentActivities(
                    limit, branchId, currentUser);

            return ResponseEntity.ok(activities);
        }
    }

