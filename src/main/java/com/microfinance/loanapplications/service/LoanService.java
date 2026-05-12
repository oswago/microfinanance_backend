package com.microfinance.loanapplications.service;

import com.microfinance.loanapplications.dto.*;
import com.microfinance.loanapplications.dto.collection.BulkAssignResultDto;
import com.microfinance.loanapplications.dto.collection.LoanEligibleForRecoveryDto;
import com.microfinance.loanapplications.dto.disbursement.PortfolioSummaryDto;
import com.microfinance.loanapplications.dto.repayment.LoanEligibleForRepaymentDto;
import com.microfinance.loanapplications.dto.repayment.RepaymentReceiptDto;
import com.microfinance.loanapplications.dto.repayment.RepaymentRequestDto;
import com.microfinance.loanapplications.dto.repayment.RepaymentScheduleDto;
import com.microfinance.loanapplications.dto.LoanRescheduleRequestDto;
import com.microfinance.loanapplications.dto.LoanRescheduleResponseDto;
import com.microfinance.loanapplications.dto.LoanRestructureRequestDto;
import com.microfinance.loanapplications.dto.LoanRestructureResponseDto;
import com.microfinance.base.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface LoanService {

    @Transactional(readOnly = true)
    Page<LoanDto> getLoans(String status, Long branchId, Long borrowerId, Long loanProductId,
                           LocalDate fromDate, LocalDate toDate, Pageable pageable);

    @Transactional(readOnly = true)
    LoanDto getLoanById(Long id);

    LoanDto getLoanByAccountNumber(String accountNumber, User currentUser);

    List<LoanSummaryDto> getLoansByBorrower(Long borrowerId);

    Map<String, Object> getLoanSummary(Long branchId);

    List<RepaymentScheduleDto> getRepaymentSchedule(Long loanId);

    RepaymentReceiptDto makeRepayment(Long loanId, RepaymentRequestDto request, User currentUser);

    List<RepaymentReceiptDto> getRepaymentHistory(Long loanId);

    LoanRescheduleResponseDto requestReschedule(Long loanId, LoanRescheduleRequestDto request, User currentUser);

    LoanRestructureResponseDto requestRestructure(Long loanId, LoanRestructureRequestDto request, User currentUser);

    List<LoanDocumentDto> getLoanDocuments(Long loanId);

    List<LoanAuditDto> getLoanAuditTrail(Long loanId);

    PortfolioSummaryDto getPortfolioSummary(Long branchId, LocalDate asOfDate);

    Page<LoanDto> getDelinquentLoans(Integer daysOverdue, Long branchId, Pageable pageable);

    BigDecimal calculateTotalOutstanding(Long branchId);

    @Transactional
    LoanDto assignCollectionOfficer(Long loanId, Long officerId, User currentUser);

    @Transactional
    LoanDto unassignCollectionOfficer(Long loanId, User currentUser);

    @Transactional
    BulkAssignResultDto bulkAssignCollectionOfficers(List<Long> loanIds, Long officerId, String notes, User currentUser);

    @Transactional(readOnly = true)
    Page<LoanDto> getLoansByCollectionOfficer(Long officerId, String status, Pageable pageable);

    List<LoanEligibleForRepaymentDto> getLoansEligibleForRepayment(Long branchId, Long loanProductId, String status, User currentUser);

    Page<OverdueLoanDto> getOverdueLoans(LocalDate date, Long branchId, Long loanOfficerId, Integer minDaysOverdue, Integer maxDaysOverdue, User currentUser, Pageable pageable);

    @Transactional(readOnly = true)
    List<LoanEligibleForRecoveryDto> getLoansEligibleForRecovery(User currentUser);
}