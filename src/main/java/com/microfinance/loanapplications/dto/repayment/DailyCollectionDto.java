package com.microfinance.loanapplications.dto.repayment;

import com.microfinance.loanapplications.dto.disbursement.LoanRepaymentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// In DailyCollectionDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyCollectionDto {
    private LocalDate reportDate;
    private Long branchId;
    private Long officerId;
    private BigDecimal totalCollection;
    private Long numberOfTransactions;
    private List<PaymentMethodBreakdownDto> paymentMethodBreakdown;
    private List<LoanRepaymentDto> recentRepayments;
}