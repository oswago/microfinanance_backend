package com.microfinance.loanapplications.service;

import com.microfinance.base.entity.User;
import com.microfinance.loanapplications.dto.*;
import com.microfinance.loanapplications.dto.approval.*;
import com.microfinance.loanapplications.dto.ApplicationApprovalDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface LoanApprovalService {
    
    LoanApplicationDto approveApplication(Long applicationId, ApprovalDecisionDto dto, User approver);
    LoanApplicationDto rejectApplication(Long applicationId, ApprovalDecisionDto dto, User approver);
    LoanApplicationDto returnForRevision(Long applicationId, ApprovalDecisionDto dto, User approver);
    
    List<LoanApplicationDto> getApplicationsForApproval(User approver);

    @Transactional(readOnly = true)
    Page<PendingApprovalDto> getPendingApprovals(User approver, Pageable pageable,
                                                 ApprovalFilterDto filter);

    List<ApplicationApprovalDto> getApprovalHistory(Long applicationId);
    ApprovalWorkflowDto getApprovalWorkflow(Long applicationId);

    @Transactional(readOnly = true)
    ApprovalWorkflowDto getApprovalWorkflow(Long applicationId, User currentUser);

    ApprovalPerformanceDto getApprovalPerformance(Long approverId, LocalDate startDate, LocalDate endDate);
    
    boolean canUserApproveApplication(Long applicationId, User user);
    
    // New methods
    ApprovalSummaryDto getApprovalSummary(Long applicationId, User currentUser);

    @Transactional(readOnly = true)
    ApprovalStatsDto getApprovalStatistics(User user, Long branchId, LocalDate startDate,
                                           LocalDate endDate, String period);

    @Transactional
    BulkApprovalResult bulkApprovePendingApplications(BulkApprovalRequestDto request, User approver);

    @Transactional(readOnly = true)
    ApprovalAnalyticsDto getApprovalAnalytics(String period, Long branchId, Long approverId,
                                              String productType);

    @Transactional(readOnly = true)
    byte[] exportApprovals(String format, LocalDate startDate, LocalDate endDate,
                           String status, Long branchId, Long approverId);

    @Transactional(readOnly = true)
    SLAStatusDto getSLAStatus(Long applicationId);

    List<ApprovalConditionDto> getApprovalConditions(Long applicationId);
    LoanApplicationDto addApprovalCondition(Long applicationId, ApprovalConditionDto condition, User user);
    LoanApplicationDto completeApprovalCondition(Long applicationId, String conditionType, User user);

    ApprovalStatsDto getApprovalStatisticsForUser(User currentUser, Long approverId, Long branchId, LocalDate startDate, LocalDate endDate, String period);

    List<ApprovalCommentDto> getApprovalComments(Long applicationId);

    ApprovalCommentDto addApprovalComment(Long applicationId, AddCommentDto dto, User currentUser);

    ApprovalTimelineDto getApprovalTimeline(Long applicationId);

    QueuePositionDto getApprovalQueuePosition(Long applicationId);

    DelegateApprovalResult delegateApproval(Long applicationId, DelegateApprovalDto dto, User currentUser);

    List<ApprovalDelegationDto> getApprovalDelegations(Long delegatorId, Long delegateId, boolean activeOnly);

    List<ApprovalReminderDto> getApprovalReminders(Long approverId, boolean overdueOnly, int limit);

    void dismissApprovalReminder(Long reminderId, User currentUser, String reason);

    @Transactional
    EscalationResult escalateApproval(Long applicationId, EscalationDto dto, User currentUser);
}
