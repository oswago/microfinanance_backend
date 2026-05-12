package com.microfinance.loanapplications.dto.collection;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleReminderRequestDto {

    /**
     * Date when the reminder should be sent
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reminderDate;

    /**
     * Time when the reminder should be sent (optional)
     */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime reminderTime;

    /**
     * Type of reminder: SMS, EMAIL, BOTH
     */
    private String reminderType;

    /**
     * Frequency: ONCE, DAILY, WEEKLY, MONTHLY
     */
    private String frequency;

    /**
     * Scope of recipients: ALL_OVERDUE, SELECTED_LOANS, BY_BRANCH, BY_OFFICER
     */
    private String recipientScope;

    /**
     * Branch ID for BY_BRANCH scope
     */
    private Long branchId;

    /**
     * Loan officer ID for BY_OFFICER scope
     */
    private Long loanOfficerId;

    /**
     * List of loan IDs for SELECTED_LOANS scope
     */
    private List<Long> loanIds;

    /**
     * Message template for the reminder
     */
    private String messageTemplate;

    /**
     * Whether to send immediately instead of scheduling
     */
    private Boolean sendNow;

    /**
     * Whether this is a recurring reminder
     */
    private Boolean recurring;

    /**
     * Recurrence interval (e.g., 1, 2, 3)
     */
    private Integer recurrenceInterval;

    /**
     * Recurrence unit: DAYS, WEEKS, MONTHS
     */
    private String recurrenceUnit;

    /**
     * End date for recurring reminders (optional)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}