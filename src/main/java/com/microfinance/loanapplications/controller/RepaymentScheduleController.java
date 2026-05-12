// RepaymentScheduleController.java
package com.microfinance.loanapplications.controller;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.BusinessException;
import com.microfinance.loanapplications.dto.RescheduleRequestDto;
import com.microfinance.loanapplications.dto.repayment.*;
import com.microfinance.loanapplications.entity.LoanRepayment;
import com.microfinance.loanapplications.service.RepaymentScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/repayment-schedules")
@RequiredArgsConstructor
public class RepaymentScheduleController {

    private final RepaymentScheduleService repaymentScheduleService;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    // Get all repayment schedules with filters
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT') and " +
                 "(@permissionCheckService.hasPermission('REPAYMENT_VIEW_ALL') or @permissionCheckService.hasPermission('REPAYMENT_VIEW_BRANCH'))")
    public ResponseEntity<Page<RepaymentScheduleDto>> getRepaymentSchedules(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long loanProductId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        log.info("Fetching repayment schedules with filters - status: {}, branch: {}, product: {}, dates: {} - {}",
                status, branchId, loanProductId, startDate, endDate);

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RepaymentScheduleDto> schedules = repaymentScheduleService.getRepaymentSchedules(
                status, branchId, loanProductId, startDate, endDate, search, pageable);
        return ResponseEntity.ok(schedules);
    }

    // Get a single repayment schedule by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<RepaymentScheduleDto> getRepaymentScheduleById(@PathVariable Long id) {
        log.info("Fetching repayment schedule with id: {}", id);
        RepaymentScheduleDto schedule = repaymentScheduleService.getRepaymentScheduleById(id);
        return ResponseEntity.ok(schedule);
    }

    // Get installments for a specific schedule
    @GetMapping("/{scheduleId}/installments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<List<InstallmentDto>> getScheduleInstallments(@PathVariable Long scheduleId) {
        log.info("Fetching installments for schedule: {}", scheduleId);
        List<InstallmentDto> installments = repaymentScheduleService.getScheduleInstallments(scheduleId);
        return ResponseEntity.ok(installments);
    }

    // Generate a new repayment schedule for a loan
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER') and @permissionCheckService.hasPermission('REPAYMENT_SCHEDULE_CREATE')")
    public ResponseEntity<RepaymentScheduleDto> generateRepaymentSchedule(
            @Valid @RequestBody CreateScheduleRequestDto requestDto) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Generating repayment schedule for loan: {} by user: {}", requestDto.getLoanId(), currentUser.getUsername());

        RepaymentScheduleDto schedule = repaymentScheduleService.generateRepaymentSchedule(requestDto, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(schedule);
    }

    // Export schedules (PDF/Excel)
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> exportSchedules(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long loanProductId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "PDF") String format) {

        log.info("Exporting repayment schedules - format: {}, status: {}, branch: {}, dates: {} - {}",
                format, status, branchId, startDate, endDate);

        byte[] reportContent = repaymentScheduleService.exportSchedules(status, branchId, loanProductId, startDate, endDate, format);

        String contentType = format.equalsIgnoreCase("PDF") ? "application/pdf" : "application/vnd.ms-excel";
        String extension = format.toLowerCase();
        String filename = "repayment-schedules." + extension;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }

    // Export a single schedule
    @GetMapping("/{scheduleId}/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> exportSchedule(
            @PathVariable Long scheduleId,
            @RequestParam(defaultValue = "PDF") String format) {

        log.info("Exporting schedule {} in format: {}", scheduleId, format);

        byte[] reportContent = repaymentScheduleService.exportSchedule(scheduleId, format);

        String contentType = format.equalsIgnoreCase("PDF") ? "application/pdf" : "application/vnd.ms-excel";
        String extension = format.toLowerCase();
        String filename = "repayment-schedule-" + scheduleId + "." + extension;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }

    // Print schedule
    @GetMapping("/{scheduleId}/print")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> printSchedule(@PathVariable Long scheduleId) {

        log.info("Printing schedule: {}", scheduleId);

        byte[] reportContent = repaymentScheduleService.printSchedule(scheduleId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        // For inline display in browser
        headers.setContentDisposition(ContentDisposition.inline()
                .filename("schedule-" + scheduleId + ".pdf")
                .build());

        // Alternative simpler approach:
        // headers.add("Content-Disposition", "inline; filename=schedule-" + scheduleId + ".pdf");

        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }

    // Send payment reminder
    @PostMapping("/{scheduleId}/send-reminder")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'COLLECTION_OFFICER')")
    public ResponseEntity<Void> sendReminder(@PathVariable Long scheduleId) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Sending payment reminder for schedule: {} by user: {}", scheduleId, currentUser.getUsername());

        repaymentScheduleService.sendReminder(scheduleId, currentUser);
        return ResponseEntity.ok().build();
    }

    // Close a schedule (when loan is fully paid)
    @PostMapping("/{scheduleId}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER') and @permissionCheckService.hasPermission('REPAYMENT_SCHEDULE_CLOSE')")
    public ResponseEntity<RepaymentScheduleDto> closeSchedule(@PathVariable Long scheduleId) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Closing repayment schedule: {} by user: {}", scheduleId, currentUser.getUsername());

        RepaymentScheduleDto closedSchedule = repaymentScheduleService.closeSchedule(scheduleId, currentUser);
        return ResponseEntity.ok(closedSchedule);
    }

    // Get schedule statistics
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<ScheduleStatisticsDto> getScheduleStatistics(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        log.info("Fetching schedule statistics for branch: {}, as of: {}", branchId, asOfDate);

        ScheduleStatisticsDto statistics = repaymentScheduleService.getScheduleStatistics(branchId, asOfDate);
        return ResponseEntity.ok(statistics);
    }

    // Get upcoming payments
    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<InstallmentDto>> getUpcomingPayments(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) Long branchId) {

        log.info("Fetching upcoming payments for next {} days, branch: {}", days, branchId);

        List<InstallmentDto> upcomingPayments = repaymentScheduleService.getUpcomingPayments(days, branchId);
        return ResponseEntity.ok(upcomingPayments);
    }

    // Get overdue payments
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<InstallmentDto>> getOverduePayments(
            @RequestParam(required = false) Long branchId) {

        log.info("Fetching overdue payments for branch: {}", branchId);

        List<InstallmentDto> overduePayments = repaymentScheduleService.getOverduePayments(branchId);
        return ResponseEntity.ok(overduePayments);
    }

    // Record payment for an installment
    @PostMapping("/installments/{installmentId}/pay")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'ACCOUNTANT', 'COLLECTION_OFFICER') and @permissionCheckService.hasPermission('REPAYMENT_RECORD')")
    public ResponseEntity<InstallmentDto> recordInstallmentPayment(
            @PathVariable Long installmentId,
            @Valid @RequestBody InstallmentPaymentDto paymentDto) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Recording payment for installment: {} by user: {}", installmentId, currentUser.getUsername());

        if (!installmentId.equals(paymentDto.getInstallmentId())) {
            throw new BusinessException("Installment ID mismatch");
        }

        InstallmentDto updatedInstallment = repaymentScheduleService.recordInstallmentPayment(paymentDto, currentUser);
        return ResponseEntity.ok(updatedInstallment);
    }

    // Get installment details
    @GetMapping("/installments/{installmentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<InstallmentDto> getInstallmentDetails(@PathVariable Long installmentId) {

        log.info("Fetching installment details: {}", installmentId);

        InstallmentDto installment = repaymentScheduleService.getInstallmentDetails(installmentId);
        return ResponseEntity.ok(installment);


    }


    @GetMapping("/installments/{installmentId}/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT')")
    public ResponseEntity<List<RepaymentSummaryDto>> getInstallmentPaymentHistory(
            @PathVariable Long installmentId) {

        log.info("Fetching payment history for installment: {}", installmentId);

        List<RepaymentSummaryDto> payments = repaymentScheduleService.getInstallmentPaymentHistory(installmentId);
        return ResponseEntity.ok(payments);
    }




    // Generate schedule statement
    @GetMapping("/{scheduleId}/statement")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> generateScheduleStatement(@PathVariable Long scheduleId) {

        log.info("Generating statement for schedule: {}", scheduleId);

        byte[] statementContent = repaymentScheduleService.generateScheduleStatement(scheduleId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "repayment-statement-" + scheduleId + ".pdf");

        return new ResponseEntity<>(statementContent, headers, HttpStatus.OK);
    }

    // Reschedule a repayment schedule
    @PostMapping("/{scheduleId}/reschedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER') and @permissionCheckService.hasPermission('REPAYMENT_SCHEDULE_RESCHEDULE')")
    public ResponseEntity<RepaymentScheduleDto> rescheduleSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody RescheduleRequestDto requestDto) {

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
        log.info("Rescheduling schedule: {} by user: {}", scheduleId, currentUser.getUsername());

        RepaymentScheduleDto rescheduledSchedule = repaymentScheduleService.rescheduleSchedule(scheduleId, requestDto, currentUser);
        return ResponseEntity.ok(rescheduledSchedule);
    }

    // Get schedule calendar data
    @GetMapping("/calendar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<CalendarEventDto>> getScheduleCalendar(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long branchId) {

        log.info("Fetching schedule calendar for month: {}, year: {}, branch: {}", month, year, branchId);

        List<CalendarEventDto> calendarEvents = repaymentScheduleService.getScheduleCalendar(month, year, branchId);
        return ResponseEntity.ok(calendarEvents);
    }

    // Get due reports
    @GetMapping("/due-reports")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> getDueReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "PDF") String format) {

        log.info("Generating due reports for period: {} to {}, branch: {}, format: {}",
                startDate, endDate, branchId, format);

        byte[] reportContent = repaymentScheduleService.generateDueReports(startDate, endDate, branchId, format);

        String contentType = format.equalsIgnoreCase("PDF") ? "application/pdf" : "application/vnd.ms-excel";
        String extension = format.toLowerCase();
        String filename = "due-reports-" + startDate + "-to-" + endDate + "." + extension;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(reportContent, headers, HttpStatus.OK);
    }

    /**
     * Get repayment schedules by loan ID
     */
    @GetMapping("/loan/{loanId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT', 'COLLECTION_OFFICER')")
    public ResponseEntity<List<RepaymentScheduleDto>> getRepaymentSchedulesByLoanId(@PathVariable Long loanId) {
        log.info("Fetching repayment schedules for loan ID: {}", loanId);

        List<RepaymentScheduleDto> schedules = repaymentScheduleService.getRepaymentSchedulesByLoanId(loanId);
        return ResponseEntity.ok(schedules);
    }

    /**
     * Get repayment schedules by loan ID with pagination
     */
    @GetMapping("/loan/{loanId}/paginated")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'ACCOUNTANT', 'COLLECTION_OFFICER')")
    public ResponseEntity<Page<RepaymentScheduleDto>> getRepaymentSchedulesByLoanIdPaginated(
            @PathVariable Long loanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        log.info("Fetching paginated repayment schedules for loan ID: {}, page: {}, size: {}", loanId, page, size);

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RepaymentScheduleDto> schedules = repaymentScheduleService.getRepaymentSchedulesByLoanId(loanId, pageable);
        return ResponseEntity.ok(schedules);
    }

}