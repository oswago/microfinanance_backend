package com.microfinance.loanapplications.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkReminderRequestDto {
    private List<Long> loanIds;
    private String reminderType; // SMS, EMAIL, BOTH
    private String messageTemplate;
    private Boolean sendToAllOverdue;
    private Integer minDaysOverdue;
    private Integer maxDaysOverdue;
    private Long branchId;
}