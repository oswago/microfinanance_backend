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
public class BulkReminderResultDto {
    private Integer totalSent;
    private Integer successful;
    private Integer failed;
    private List<ReminderResultDto> results;
}
