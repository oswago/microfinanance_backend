// WriteOffSearchCriteria.java
package com.microfinance.loanapplications.dto.disbursement;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WriteOffSearchCriteria {
    private String status;
    private Long branchId;
    private Long loanOfficerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String searchTerm;
    private String recoveryPlan;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}