// WriteOffSummaryDto.java
package com.microfinance.loanapplications.dto.disbursement;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WriteOffSummaryDto {
    private long totalWriteOffs;
    private BigDecimal totalAmount;
    private long pendingApprovals;
    private long approvedWriteOffs;
    private long rejectedWriteOffs;
    private BigDecimal averageWriteOffAmount;
    private WriteOffStats byReason;
    private WriteOffStats byPeriod;
}
