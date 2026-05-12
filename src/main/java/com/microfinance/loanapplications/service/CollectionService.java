package com.microfinance.loanapplications.service;

import com.microfinance.base.entity.User;
import com.microfinance.loanapplications.dto.collection.*;
import com.microfinance.loanapplications.dto.repayment.OverdueInstallmentDto;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface CollectionService {

    @Transactional(readOnly = true)
    List<OverdueInstallmentDto> getOverdueInstallments(Long branchId, Integer minDaysOverdue,
                                                       Integer maxDaysOverdue, User currentUser);

    @Transactional(readOnly = true)
    CollectionActionStatsDto getCollectionActionStats(LocalDate date, Long branchId, User currentUser);

    @Transactional
    CollectionActionDto logPhoneCall(LogPhoneCallDto phoneCallDto, User currentUser);

    @Transactional
    PenaltyResultDto applyPenalty(ApplyPenaltyDto penaltyDto, User currentUser);

    @Transactional
    EscalationResultDto escalateCase(EscalateCaseDto escalateDto, User currentUser);

    @Transactional(readOnly = true)
    List<ActivityDto> getRecentActivities(int limit, Long branchId, User currentUser);

    CollectionStatisticsDto getCollectionStatistics(LocalDate date, Long branchId, User currentUser);
    List<UpcomingActionDto> getUpcomingCollectionActions(int limit, Long branchId, Long loanOfficerId, User currentUser);
    CollectionActionDto recordCollectionAction(RecordCollectionActionDto actionDto, User currentUser);
    List<CollectionActionDto> getCollectionActions(Long loanId);
    BulkReminderResultDto sendBulkReminders(BulkReminderRequestDto request, User currentUser);
    TaskAssignmentResultDto assignCollectionTasks(TaskAssignmentRequestDto request, User currentUser);
    CollectionReportDto generateCollectionReport(LocalDate startDate, LocalDate endDate, Long branchId, Long loanOfficerId, User currentUser);

    @Transactional(readOnly = true)
    byte[] exportCollectionReport(CollectionReportDto report, String format);


    @Transactional
    void scheduleBulkReminders(ScheduleReminderRequestDto request, User currentUser);

    // Add to CollectionService.java

    CollectionPerformanceDto getCollectionPerformance(LocalDate startDate, LocalDate endDate,
                                                      Long branchId, Long officerId, User currentUser);

    byte[] exportPerformanceReport(CollectionPerformanceDto performance, String format);

    OfficerPerformanceDetailDto getOfficerPerformanceDetails(Long officerId, LocalDate startDate,
                                                             LocalDate endDate, User currentUser);

    PerformanceSummaryDto getPerformanceSummary(LocalDate startDate, LocalDate endDate,
                                                Long branchId, User currentUser);

    List<DailyCollectionTrendDto> getCollectionTrends(LocalDate startDate, LocalDate endDate,
                                                      Long branchId, User currentUser);


    }
