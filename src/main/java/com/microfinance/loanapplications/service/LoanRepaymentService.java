package com.microfinance.loanapplications.service;

import com.microfinance.base.entity.User;
import com.microfinance.loanapplications.dto.disbursement.LoanRepaymentDto;
import com.microfinance.loanapplications.dto.repayment.*;
import com.microfinance.loanapplications.dto.repayment.DailyCollectionDto;
import com.microfinance.loanapplications.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LoanRepaymentService {
    
    // Core repayment operations
    RepaymentReceiptDto recordRepayment(RepaymentDto dto, User currentUser);
    BulkRepaymentResultDto recordBulkRepayments(List<RepaymentDto> repayments, User currentUser);
    RepaymentReceiptDto reverseRepayment(Long repaymentId, String reason, User currentUser);
    RepaymentReceiptDto waiveRepayment(Long repaymentId, WaiveRepaymentDto dto, User currentUser);
    
    // Query operations
    List<RepaymentScheduleDto> getRepaymentSchedule(Long loanId);
    Page<LoanRepaymentDto> getRepaymentHistory(Long loanId, Pageable pageable);
    Page<LoanRepaymentDto> getRepaymentHistoryByBorrower(Long borrowerId, Pageable pageable);
    
    // Early repayment
    EarlyRepaymentQuoteDto calculateEarlyRepaymentAmount(Long loanId);
    
    // Reports and analytics
    Page<OverdueInstallmentDto> getOverdueInstallments(LocalDate date, Long branchId, Pageable pageable);
    DailyCollectionDto getDailyCollectionReport(LocalDate date, Long branchId, Long officerId);
    CollectionPerformanceDto getCollectionPerformance(Long officerId, LocalDate startDate, LocalDate endDate);
    
    // Utility methods
    RepaymentAllocationDto calculateRepaymentAllocation(Loan loan, BigDecimal paymentAmount);
    void validateRepayment(RepaymentDto dto);

    List<LoanRepaymentDto> getRecentRepayments(int limit);

    RepaymentStatisticsDto getRepaymentStatistics();

    @Transactional(readOnly = true)
    byte[] exportRepaymentHistory(Page<LoanRepaymentDto> repayments, Long loanId, String format);
}