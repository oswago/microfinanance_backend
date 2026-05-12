// RepaymentScheduleService.java
package com.microfinance.loanapplications.service;

import com.microfinance.base.entity.User;
import com.microfinance.loanapplications.dto.RescheduleRequestDto;
import com.microfinance.loanapplications.dto.repayment.*;
import com.microfinance.loanapplications.entity.LoanRepayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface RepaymentScheduleService {

    // Get all repayment schedules with filters
    Page<RepaymentScheduleDto> getRepaymentSchedules(String status, Long branchId, Long loanProductId,
                                                     LocalDate startDate, LocalDate endDate,
                                                     String search, Pageable pageable);

    // Get a single repayment schedule by ID
    RepaymentScheduleDto getRepaymentScheduleById(Long id);

    // Get installments for a specific schedule
    List<InstallmentDto> getScheduleInstallments(Long scheduleId);

    // Generate a new repayment schedule for a loan
    RepaymentScheduleDto generateRepaymentSchedule(CreateScheduleRequestDto requestDto, User currentUser);

    // Export schedules (PDF/Excel)
    byte[] exportSchedules(String status, Long branchId, Long loanProductId,
                           LocalDate startDate, LocalDate endDate, String format);

    // Export a single schedule
    byte[] exportSchedule(Long scheduleId, String format);

    // Print schedule
    byte[] printSchedule(Long scheduleId);

    // Send payment reminder
    void sendReminder(Long scheduleId, User currentUser);

    // Close a schedule (when loan is fully paid)
    RepaymentScheduleDto closeSchedule(Long scheduleId, User currentUser);

    // Get schedule statistics
    ScheduleStatisticsDto getScheduleStatistics(Long branchId, LocalDate asOfDate);

    // Get upcoming payments
    List<InstallmentDto> getUpcomingPayments(int days, Long branchId);

    // Get overdue payments
    List<InstallmentDto> getOverduePayments(Long branchId);

    // Record payment for an installment
    InstallmentDto recordInstallmentPayment(InstallmentPaymentDto paymentDto, User currentUser);

    // Get installment details
    InstallmentDto getInstallmentDetails(Long installmentId);

    List<RepaymentSummaryDto> getInstallmentPaymentHistory(Long installmentId);



    // Generate schedule statement
    byte[] generateScheduleStatement(Long scheduleId);

    // Reschedule a repayment schedule
    RepaymentScheduleDto rescheduleSchedule(Long scheduleId, RescheduleRequestDto requestDto, User currentUser);

    // Get schedule calendar data
    List<CalendarEventDto> getScheduleCalendar(Integer month, Integer year, Long branchId);

    // Generate due reports
    byte[] generateDueReports(LocalDate startDate, LocalDate endDate, Long branchId, String format);

    List<RepaymentScheduleDto> getRepaymentSchedulesByLoanId(Long loanId);
    Page<RepaymentScheduleDto> getRepaymentSchedulesByLoanId(Long loanId, Pageable pageable);
}