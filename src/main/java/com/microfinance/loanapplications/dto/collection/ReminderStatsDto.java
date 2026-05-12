package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderStatsDto {
    private Integer scheduledReminders;
    private Integer totalRecipients;
    private Integer smsCount;
    private Integer emailCount;
}