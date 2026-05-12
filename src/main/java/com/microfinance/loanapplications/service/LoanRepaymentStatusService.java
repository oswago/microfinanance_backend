package com.microfinance.loanapplications.service;

import com.microfinance.base.utils.GeneralConfig;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.entity.LoanRepayment;
import com.microfinance.loanapplications.entity.LoanRepaymentStatus;
import com.microfinance.loanapplications.entity.RepaymentSchedule;
import com.microfinance.loanapplications.repository.LoanRepaymentStatusRepository;
import com.microfinance.loanapplications.repository.LoanRepository;
import com.microfinance.loanapplications.repository.LoanRepaymentRepository;
import com.microfinance.loanapplications.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanRepaymentStatusService {

    private final LoanRepaymentStatusRepository statusRepository;
    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final LoanRepaymentRepository repaymentRepository;

    @Transactional
    public LoanRepaymentStatus updateRepaymentStatus(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        List<RepaymentSchedule> schedules = scheduleRepository
                .findByLoanIdOrderByInstallmentNumberAsc(loanId);

        List<LoanRepayment> repayments = repaymentRepository.findByLoanIdOrderByPaymentDateDesc(loanId);
        // Calculate totals
        BigDecimal totalDue = schedules.stream()
                .map(RepaymentSchedule::getTotalDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = repayments.stream()
                .map(LoanRepayment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstandingBalance = totalDue.subtract(totalPaid);

        // Calculate arrears
        LocalDate today = LocalDate.now();
        BigDecimal totalArrears = schedules.stream()
                .filter(s -> s.getDueDate().isBefore(today) && s.getStatus() != com.microfinance.common.config.GeneralConfig.InstallmentStatus.PAID)
                .map(RepaymentSchedule::getOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Count installments by status
        int totalInstallments = schedules.size();
        int paidInstallments = (int) schedules.stream()
                .filter(s -> s.getStatus() == com.microfinance.common.config.GeneralConfig.InstallmentStatus.PAID)
                .count();
        int pendingInstallments = (int) schedules.stream()
                .filter(s -> s.getStatus() == com.microfinance.common.config.GeneralConfig.InstallmentStatus.PENDING)
                .count();
        int overdueInstallments = (int) schedules.stream()
                .filter(s -> s.isOverdue())
                .count();

        // Find next due date
        Optional<RepaymentSchedule> nextDue = schedules.stream()
                .filter(s -> s.getStatus() == com.microfinance.common.config.GeneralConfig.InstallmentStatus.PENDING)
                .min(Comparator.comparing(RepaymentSchedule::getDueDate));

        // Find last payment date
        Optional<LoanRepayment> lastRepayment = repayments.stream()
                .max(Comparator.comparing(LoanRepayment::getPaymentDate));

        // Calculate delinquency days
        int daysDelinquent = 0;
        if (overdueInstallments > 0) {
            Optional<RepaymentSchedule> oldestOverdue = schedules.stream()
                    .filter(s -> s.isOverdue())
                    .min(Comparator.comparing(RepaymentSchedule::getDueDate));

            if (oldestOverdue.isPresent()) {
                daysDelinquent = (int) java.time.temporal.ChronoUnit.DAYS
                        .between(oldestOverdue.get().getDueDate(), today);
            }
        }

        // Create or update status
        LoanRepaymentStatus status = statusRepository.findByLoanId(loanId)
                .orElse(LoanRepaymentStatus.builder()
                        .loan(loan)
                        .createdAt(LocalDateTime.now())
                        .build());

        status.setTotalDue(totalDue);
        status.setTotalPaid(totalPaid);
        status.setOutstandingBalance(outstandingBalance);
        status.setTotalArrears(totalArrears);
        status.setPenaltyAccrued(loan.getPenaltyAccrued());
        status.setTotalInstallments(totalInstallments);
        status.setPaidInstallments(paidInstallments);
        status.setPendingInstallments(pendingInstallments);
        status.setOverdueInstallments(overdueInstallments);
        status.setLastPaymentDate(lastRepayment.map(LoanRepayment::getPaymentDate).orElse(null));
        status.setNextDueDate(nextDue.map(RepaymentSchedule::getDueDate).orElse(null));
        status.setDaysDelinquent(daysDelinquent);
        status.setMaxDaysDelinquent(Math.max(daysDelinquent, 
                status.getMaxDaysDelinquent() != null ? status.getMaxDaysDelinquent() : 0));
        status.setLastCalculatedAt(LocalDateTime.now());
        status.setUpdatedAt(LocalDateTime.now());

        // Update derived fields
        status.updateDelinquencyBucket();
        status.calculateCollectionRate();

        // Set overall status
        if (outstandingBalance.compareTo(BigDecimal.ZERO) == 0) {
            status.setOverallStatus(GeneralConfig.RepaymentStatus.PAID);
        } else if (daysDelinquent > 90) {
            status.setOverallStatus(GeneralConfig.RepaymentStatus.DEFAULT);
        } else if (daysDelinquent > 0) {
            status.setOverallStatus(GeneralConfig.RepaymentStatus.OVERDUE);
        } else if (nextDue.isPresent() && nextDue.get().getDueDate().isBefore(today.plusDays(7))) {
            status.setOverallStatus(GeneralConfig.RepaymentStatus.DUE);
        } else {
            status.setOverallStatus(GeneralConfig.RepaymentStatus.CURRENT);
        }

        return statusRepository.save(status);
    }

    @Transactional
    public void updateAllLoanStatuses() {
        List<Loan> allLoans = loanRepository.findAll();
        for (Loan loan : allLoans) {
            try {
                updateRepaymentStatus(loan.getId());
            } catch (Exception e) {
                log.error("Failed to update status for loan {}: {}", loan.getId(), e.getMessage());
            }
        }
    }
}