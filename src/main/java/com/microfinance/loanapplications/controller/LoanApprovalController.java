package com.microfinance.loanapplications.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.loanapplications.dto.*;
import com.microfinance.loanapplications.dto.ApplicationApprovalDto;
import com.microfinance.loanapplications.dto.approval.*;
import com.microfinance.loanapplications.service.LoanApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/loan-approvals")
@RequiredArgsConstructor
public class LoanApprovalController {

    private final LoanApprovalService loanApprovalService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER') ") //and hasPermission('APPLICATION_APPROVE')
    public ResponseEntity<Page<PendingApprovalDto>> getPendingApprovals(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long approverId,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "submittedDate", direction = Sort.Direction.ASC) Pageable pageable) {

        User approver = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Fetching pending approvals for user: {}", approver.getUsername());

        // Convert Double to BigDecimal
        BigDecimal minAmountBD = minAmount != null ? BigDecimal.valueOf(minAmount) : null;
        BigDecimal maxAmountBD = maxAmount != null ? BigDecimal.valueOf(maxAmount) : null;

        // Build filter DTO
        ApprovalFilterDto filter = ApprovalFilterDto.builder()
                .branchId(branchId)
                .approverId(approverId)
                .minAmount(minAmountBD)
                .maxAmount(maxAmountBD)
                .productType(productType)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        Page<PendingApprovalDto> pendingApprovals = loanApprovalService.getPendingApprovals(
                approver, pageable, filter);

        return ResponseEntity.ok(pendingApprovals);
    }


    @GetMapping("/pending/page")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER') ") //and hasPermission('APPLICATION_APPROVE')
    public ResponseEntity<Page<PendingApprovalDto>> getPendingApprovalsPage(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @PageableDefault(size = 20, sort = "submittedDate", direction = Sort.Direction.ASC) Pageable pageable) {

        User approver = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Fetching pending approvals for user: {}", approver.getUsername());

        // Build the filter DTO
        ApprovalFilterDto filter = ApprovalFilterDto.builder()
                .branchId(branchId)
                .minAmount(minAmount != null ? BigDecimal.valueOf(minAmount) : null)
                .maxAmount(maxAmount != null ? BigDecimal.valueOf(maxAmount) : null)
                .productType(productType)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        Page<PendingApprovalDto> pendingApprovals = loanApprovalService.getPendingApprovals(approver, pageable, filter);

        return ResponseEntity.ok(pendingApprovals);
    }

    /**
     * Get approval statistics for the current user
     */
    @GetMapping("/stats/my")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER')") //and hasPermission('APPLICATION_STATS_VIEW')
    public ResponseEntity<ApprovalStatsDto> getMyApprovalStatistics(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String period) {

        User user = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Fetching approval statistics for current user: {}", user.getUsername());

        ApprovalStatsDto stats = loanApprovalService.getApprovalStatistics(
                user, branchId, startDate, endDate, period);

        return ResponseEntity.ok(stats);
    }

    /**
     * Get approval statistics for a specific user (admin only)
     */
    @GetMapping("/stats/user/{approverId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN' ,'REGIONAL_MANAGER')") // and hasPermission('VIEW_ALL_STATS')
    public ResponseEntity<ApprovalStatsDto> getUserApprovalStatistics(
            @PathVariable Long approverId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String period) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        User targetUser = userService.getUserById(approverId);

        log.info("User {} fetching approval statistics for user: {}",
                currentUser.getUsername(), targetUser.getUsername());

        // Create a new service method for viewing other users' stats
        ApprovalStatsDto stats = loanApprovalService.getApprovalStatisticsForUser(
                currentUser, approverId, branchId, startDate, endDate, period);

        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER') ") // and hasPermission('APPLICATION_APPROVE')
    public ResponseEntity<LoanApplicationDto> approveApplication(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalDecisionDto dto) {

        User approver = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Approving loan application {} by user: {}", id, approver.getUsername());

        if (!loanApprovalService.canUserApproveApplication(id, approver)) {
            log.warn("User {} does not have permission to approve application {}", approver.getUsername(), id);
            return ResponseEntity.status(403).build();
        }

        LoanApplicationDto application = loanApprovalService.approveApplication(id, dto, approver);
        return ResponseEntity.ok(application);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER') ") //and hasPermission('APPLICATION_REJECT')
    public ResponseEntity<LoanApplicationDto> rejectApplication(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalDecisionDto dto) {

        User approver = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Rejecting loan application {} by user: {}", id, approver.getUsername());

        if (!loanApprovalService.canUserApproveApplication(id, approver)) {
            log.warn("User {} does not have permission to reject application {}", approver.getUsername(), id);
            return ResponseEntity.status(403).build();
        }

        LoanApplicationDto application = loanApprovalService.rejectApplication(id, dto, approver);
        return ResponseEntity.ok(application);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER') ") //and hasPermission('APPLICATION_APPROVE')
    public ResponseEntity<LoanApplicationDto> returnForRevision(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalDecisionDto dto) {

        User approver = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Returning loan application {} for revision by user: {}", id, approver.getUsername());

        if (!loanApprovalService.canUserApproveApplication(id, approver)) {
            log.warn("User {} does not have permission to return application {}", approver.getUsername(), id);
            return ResponseEntity.status(403).build();
        }

        LoanApplicationDto application = loanApprovalService.returnForRevision(id, dto, approver);
        return ResponseEntity.ok(application);
    }

    @PostMapping("/bulk-approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER') ")//and hasPermission('APPLICATION_APPROVE')
    public ResponseEntity<BulkApprovalResult> bulkApprovePendingApplications(
            @Valid @RequestBody BulkApprovalRequestDto request) {

        User approver = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Bulk approving {} applications by user: {}",
                request.getApplicationIds().size(), approver.getUsername());

        BulkApprovalResult result = loanApprovalService.bulkApprovePendingApplications(request, approver);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{applicationId}/can-approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER') ") //and hasPermission('APPLICATION_APPROVE')
    public ResponseEntity<Boolean> canUserApproveApplication(@PathVariable Long applicationId) {

        User approver = userService.getUserById(securityUtils.getCurrentUserId());
        boolean canApprove = loanApprovalService.canUserApproveApplication(applicationId, approver);

        return ResponseEntity.ok(canApprove);
    }

    @GetMapping("/{applicationId}/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER', 'LOAN_OFFICER') " )
           /* "and " +
            "(hasPermission('APPLICATION_READ') or hasPermission('APPLICATION_VIEW_ALL') or hasPermission('APPLICATION_VIEW_BRANCH') or hasPermission('APPLICATION_VIEW_OWN'))")*/
    public ResponseEntity<List<ApplicationApprovalDto>> getApprovalHistory(@PathVariable Long applicationId) {

        log.info("Fetching approval history for application: {}", applicationId);
        List<ApplicationApprovalDto> approvals = loanApprovalService.getApprovalHistory(applicationId);
        return ResponseEntity.ok(approvals);
    }

    @GetMapping("/workflow/{applicationId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER', 'LOAN_OFFICER') " )
           // "(hasPermission('APPLICATION_READ') or hasPermission('APPLICATION_VIEW_ALL') or hasPermission('APPLICATION_VIEW_BRANCH') or hasPermission('APPLICATION_VIEW_OWN'))")
    public ResponseEntity<ApprovalWorkflowDto> getApprovalWorkflow(@PathVariable Long applicationId) {

        log.info("Fetching approval workflow for application: {}", applicationId);
        ApprovalWorkflowDto workflow = loanApprovalService.getApprovalWorkflow(applicationId);
        return ResponseEntity.ok(workflow);
    }

    @GetMapping("/stats/approval-performance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER') ")//and hasPermission('APPLICATION_STATS_VIEW')
    public ResponseEntity<ApprovalPerformanceDto> getApprovalPerformance(
            @RequestParam(required = false) Long approverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Fetching approval performance for approver: {}, period: {} to {}", approverId, startDate, endDate);
        ApprovalPerformanceDto performance = loanApprovalService.getApprovalPerformance(approverId, startDate, endDate);
        return ResponseEntity.ok(performance);
    }


    // ========== COMMENTS ENDPOINTS ==========

    @GetMapping("/{applicationId}/comments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER', 'LOAN_OFFICER')")
    public ResponseEntity<List<ApprovalCommentDto>> getApprovalComments(@PathVariable Long applicationId) {
        log.info("Fetching comments for application: {}", applicationId);
        List<ApprovalCommentDto> comments = loanApprovalService.getApprovalComments(applicationId);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{applicationId}/comments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER', 'LOAN_OFFICER')")
    public ResponseEntity<ApprovalCommentDto> addApprovalComment(
            @PathVariable Long applicationId,
            @Valid @RequestBody AddCommentDto dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Adding comment to application {} by user: {}", applicationId, currentUser.getUsername());
        ApprovalCommentDto comment = loanApprovalService.addApprovalComment(applicationId, dto, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

// ========== TIMELINE ENDPOINTS ==========

    @GetMapping("/{applicationId}/timeline")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER', 'LOAN_OFFICER')")
    public ResponseEntity<ApprovalTimelineDto> getApprovalTimeline(@PathVariable Long applicationId) {
        log.info("Fetching timeline for application: {}", applicationId);
        ApprovalTimelineDto timeline = loanApprovalService.getApprovalTimeline(applicationId);
        return ResponseEntity.ok(timeline);
    }

// ========== QUEUE & POSITION ENDPOINTS ==========

    @GetMapping("/{applicationId}/queue-position")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<QueuePositionDto> getApprovalQueuePosition(@PathVariable Long applicationId) {
        log.info("Getting queue position for application: {}", applicationId);
        QueuePositionDto position = loanApprovalService.getApprovalQueuePosition(applicationId);
        return ResponseEntity.ok(position);
    }

    @GetMapping("/{applicationId}/sla-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER', 'LOAN_OFFICER')")
    public ResponseEntity<SLAStatusDto> getApprovalSLAStatus(@PathVariable Long applicationId) {
        log.info("Getting SLA status for application: {}", applicationId);
        SLAStatusDto slaStatus = loanApprovalService.getSLAStatus(applicationId);
        return ResponseEntity.ok(slaStatus);
    }

// ========== DELEGATION ENDPOINTS ==========

    @PostMapping("/{applicationId}/delegate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<DelegateApprovalResult> delegateApproval(
            @PathVariable Long applicationId,
            @Valid @RequestBody DelegateApprovalDto dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("User {} delegating approval of application {} to user {}",
                currentUser.getUsername(), applicationId, dto.getDelegateTo());
        DelegateApprovalResult result = loanApprovalService.delegateApproval(applicationId, dto, currentUser);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/delegations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<ApprovalDelegationDto>> getApprovalDelegations(
            @RequestParam(required = false) Long delegatorId,
            @RequestParam(required = false) Long delegateId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        log.info("Fetching approval delegations");
        List<ApprovalDelegationDto> delegations = loanApprovalService.getApprovalDelegations(
                delegatorId, delegateId, activeOnly);
        return ResponseEntity.ok(delegations);
    }

// ========== REMINDER ENDPOINTS ==========

    @GetMapping("/reminders")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<ApprovalReminderDto>> getApprovalReminders(
            @RequestParam(required = false) Long approverId,
            @RequestParam(defaultValue = "false") boolean overdueOnly,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Fetching approval reminders");
        List<ApprovalReminderDto> reminders = loanApprovalService.getApprovalReminders(approverId, overdueOnly, limit);
        return ResponseEntity.ok(reminders);
    }

    @PostMapping("/reminders/{reminderId}/dismiss")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<Void> dismissApprovalReminder(
            @PathVariable Long reminderId,
            @RequestBody(required = false) DismissReminderDto dismissDto) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        String reason = dismissDto != null ? dismissDto.getReason() : "Dismissed by user";

        log.info("Dismissing reminder {} by user: {}, reason: {}", reminderId, currentUser.getUsername(), reason);
        loanApprovalService.dismissApprovalReminder(reminderId, currentUser, reason);
        return ResponseEntity.noContent().build();
    }

// ========== ESCALATION ENDPOINTS ==========

    @PostMapping("/{applicationId}/escalate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<EscalationResult> escalateApproval(
            @PathVariable Long applicationId,
            @Valid @RequestBody EscalationDto dto) {
        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("User {} escalating application {}", currentUser.getUsername(), applicationId);
        EscalationResult result = loanApprovalService.escalateApproval(applicationId, dto, currentUser);
        return ResponseEntity.ok(result);
    }

// ========== ANALYTICS ENDPOINTS ==========

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApprovalAnalyticsDto> getApprovalAnalytics(
            @RequestParam(required = false, defaultValue = "month") String period,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long approverId,
            @RequestParam(required = false) String productType) {
        log.info("Fetching approval analytics for period: {}, branch: {}, approver: {}, product: {}",
                period, branchId, approverId, productType);
        ApprovalAnalyticsDto analytics = loanApprovalService.getApprovalAnalytics(period, branchId, approverId, productType);
        return ResponseEntity.ok(analytics);
    }

}