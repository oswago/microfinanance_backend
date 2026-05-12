package com.microfinance.loanapplications.service;

import com.microfinance.loanapplications.dto.LoanDto;
import com.microfinance.loanapplications.dto.RescheduleRequestDto;
import com.microfinance.loanapplications.dto.disbursement.*;
import com.microfinance.base.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LoanDisbursementService {
    
    // Core disbursement methods
    LoanDto disburseLoan(Long loanId, DisburseLoanDto dto, User currentUser);
    DisbursementReceiptDto generateDisbursementReceipt(Long loanId);
    List<LoanDto> getLoansPendingDisbursement();
    
    // Loan management
    LoanDto getLoanByAccountNumber(String accountNumber);
    LoanDto closeLoan(Long loanId, User currentUser);
    LoanDto writeOffLoan(Long loanId, WriteOffRequestDto dto, User currentUser);
    LoanDto rescheduleLoan(Long loanId, RescheduleRequestDto dto, User currentUser);
    
    // Query methods
    Page<LoanDto> getLoans(String status, Long branchId, Long borrowerId, Pageable pageable);
    PortfolioSummaryDto getPortfolioSummary(Long branchId, LocalDate asOfDate);
    
    // Additional methods
    boolean canDisburseLoan(Long loanId);
    BigDecimal calculateNetDisbursementAmount(BigDecimal principal, DisburseLoanDto dto);

    void generateRepaymentSchedule(Long loanId, User user);

    List<LoanDto> getRecentDisbursements(int limit);

    DisbursementStatsDto getDisbursementStatistics();

    byte[] generateDisbursementReceiptPdf(Long loanId);

    BulkDisbursementResponseDto processBulkDisbursement(BulkDisbursementRequestDto request, User currentUser);

    List<LoanDto> getLoansForBulkDisbursement(List<Long> loanIds);
    byte[] generateDisbursementReport(LocalDate startDate, LocalDate endDate, Long branchId, String format);

    byte[] exportPortfolioSummary(Long branchId, LocalDate asOfDate);


    // Write-off methods
    WriteOffResponseDto processWriteOff(Long loanId, WriteOffRequestDto dto, User currentUser);

    List<LoanDto> getEligibleLoansForWriteOff();

    Page<LoanDto> getWrittenOffLoans(WriteOffSearchCriteria criteria, Pageable pageable);

    WriteOffSummaryDto getWriteOffSummary(LocalDate startDate, LocalDate endDate, Long branchId);

    WriteOffResponseDto approveWriteOff(Long loanId, User currentUser, String comments);

    WriteOffResponseDto rejectWriteOff(Long loanId, User currentUser, String reason);

    byte[] generateWriteOffReport(WriteOffSearchCriteria criteria);

    Page<LoanDto> getLoansByStatus(String status, Pageable pageable);
}