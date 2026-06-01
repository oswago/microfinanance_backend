package com.microfinance.loanapplications.mapper;

import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.dto.LoanDto;
import com.microfinance.loanapplications.entity.LoanRepayment;
import com.microfinance.loanapplications.entity.RepaymentSchedule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EfficientLoanMapper {

    private final RepaymentScheduleMapper repaymentScheduleMapper;
    private final LoanRepaymentMapper loanRepaymentMapper;

    public EfficientLoanMapper(RepaymentScheduleMapper repaymentScheduleMapper, LoanRepaymentMapper loanRepaymentMapper) {
        this.repaymentScheduleMapper = repaymentScheduleMapper;
        this.loanRepaymentMapper = loanRepaymentMapper;
    }


    public LoanDto toDto(Loan loan) {
        return toDto(loan, false, false);
    }
    
    public LoanDto toDtoWithSchedules(Loan loan) {
        return toDto(loan, true, false);
    }
    
    public LoanDto toDtoWithRepayments(Loan loan) {
        return toDto(loan, false, true);
    }
    
    public LoanDto toFullDto(Loan loan) {
        return toDto(loan, true, true);
    }

    /*
    private LoanDto toDto(Loan loan, boolean includeSchedules, boolean includeRepayments) {
        if (loan == null) {
            return null;
        }
        
        LoanDto dto = new LoanDto();
        
        // Map basic fields (always included)
        mapBasicFields(loan, dto);
        
        // Conditionally map related entities
        if (includeSchedules && loan.getRepaymentSchedules() != null) {
            // Use a separate mapper for repayment schedules
            // dto.setRepaymentSchedules(repaymentScheduleMapper.toDtoList(loan.getRepaymentSchedules()));
        }
        
        if (includeRepayments && loan.getRepayments() != null) {
            // Use a separate mapper for repayments
            // dto.setRecentRepayments(loanRepaymentMapper.toDtoList(loan.getRepayments()));
        }
        
        return dto;
    }
    */


    // Core mapping method
    private LoanDto toDto(Loan loan, boolean includeSchedules, boolean includeRepayments) {
        if (loan == null) {
            return null;
        }

        LoanDto dto = new LoanDto();

        // Map basic fields (always included)
        mapBasicFields(loan, dto);

        // Conditionally map related entities
        if (includeSchedules && loan.getRepaymentSchedules() != null) {
            dto.setRepaymentSchedules(
                    repaymentScheduleMapper.toDtoList(loan.getRepaymentSchedules())
            );
            // Also set derived fields from schedules
            setScheduleDerivedFields(loan.getRepaymentSchedules(), dto);
        }

        if (includeRepayments && loan.getRepayments() != null) {
            dto.setRecentRepayments(
                    loanRepaymentMapper.toDtoList(loan.getRepayments())
            );
        }

        return dto;
    }

    private void setScheduleDerivedFields(List<RepaymentSchedule> schedules, LoanDto dto) {
        if (schedules == null || schedules.isEmpty()) return;

        // Find next due
        schedules.stream()
                .filter(s -> s.getStatus() ==  GeneralConfig.InstallmentStatus.PENDING)
                .findFirst()
                .ifPresent(next -> {
                    dto.setNextDueDate(next.getDueDate());
                    dto.setNextDueAmount(next.getTotalDue());
                });

        // Calculate total arrears
        BigDecimal totalArrears = schedules.stream()
                .filter(s -> s.isOverdue() && s.getOutstandingAmount() != null)
                .map(RepaymentSchedule::getOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalArrears(totalArrears);

        // Calculate progress
        int totalInstallments = schedules.size();
        long paidInstallments = schedules.stream()
                .filter(s -> s.getStatus() ==  GeneralConfig.InstallmentStatus.PAID)
                .count();

        dto.setTotalInstallments(totalInstallments);
        dto.setInstallmentsPaid((int) paidInstallments);

        if (totalInstallments > 0) {
            double progress = (paidInstallments * 100.0) / totalInstallments;
            dto.setProgressPercentage(progress);
        }
    }

    /*
    private void mapBasicFields(Loan loan, LoanDto dto) {
        dto.setId(loan.getId());
        dto.setLoanAccountNumber(loan.getLoanAccountNumber());
        
        // Borrower information
        if (loan.getBorrower() != null) {
            dto.setBorrowerId(loan.getBorrower().getId());
            dto.setBorrowerName(loan.getBorrower().getFullName());
            dto.setBorrowerNumber(loan.getBorrower().getBorrowerNumber());
        }
        
        // Loan details
        dto.setPrincipalAmount(loan.getPrincipalAmount());
        dto.setInterestRate(loan.getInterestRate());
        dto.setTenureMonths(loan.getTenureMonths());
        
        // Status and dates
        if (loan.getStatus() != null) {
            dto.setStatus(loan.getStatus().name());
        }
        dto.setDisbursementDate(loan.getDisbursementDate());
        dto.setMaturityDate(loan.getMaturityDate());
        dto.setClosedDate(loan.getClosedDate());
        
        // Financial information
        dto.setTotalDue(loan.getTotalDue());
        dto.setTotalPaid(loan.getTotalPaid());
        dto.setOutstandingBalance(loan.getOutstandingBalance());
       // dto.setTotalInterestDue(loan.getTotalInterestDue());
        dto.setDaysDelinquent(loan.getDaysDelinquent());
        dto.setPenaltyAccrued(loan.getPenaltyAccrued());
    }
    */

    private void mapBasicFields(Loan loan, LoanDto dto) {
        // Basic loan info
        dto.setId(loan.getId());
        dto.setLoanAccountNumber(loan.getLoanAccountNumber());

        // Borrower information
        if (loan.getBorrower() != null) {
            dto.setBorrowerId(loan.getBorrower().getId());
            dto.setBorrowerName(loan.getBorrower().getFullName());
            dto.setBorrowerNumber(loan.getBorrower().getBorrowerNumber());
            dto.setBorrowerPhoneNumber(loan.getBorrower().getPhoneNumber());
        }

        // Loan product information
        if (loan.getLoanProduct() != null) {
            dto.setLoanProductId(loan.getLoanProduct().getId());
            dto.setLoanProductName(loan.getLoanProduct().getName());
        }

        // Branch information
        if (loan.getBranch() != null) {
            dto.setBranchId(loan.getBranch().getId());
            dto.setBranchName(loan.getBranch().getName());
        }

        // Loan application reference
        if (loan.getLoanApplication() != null) {
            dto.setLoanApplicationId(loan.getLoanApplication().getId());
        }

        // Loan details
        dto.setPrincipalAmount(loan.getPrincipalAmount());
        dto.setInterestRate(loan.getInterestRate());
        dto.setTenureMonths(loan.getTenureMonths());

        // Status and dates
        if (loan.getStatus() != null) {
            dto.setStatus(loan.getStatus().name());
        }
        dto.setDisbursementDate(loan.getDisbursementDate());
        dto.setMaturityDate(loan.getMaturityDate());
        dto.setClosedDate(loan.getClosedDate());
        dto.setApprovalDate(LocalDate.from(loan.getLoanApplication().getApprovedDate())); // If exists in entity

        // Financial information
        dto.setTotalDue(loan.getTotalDue());
        dto.setTotalPaid(loan.getTotalPaid());
        dto.setOutstandingBalance(loan.getOutstandingBalance());
        dto.setTotalInterestDue(loan.getTotalInterestDue());
        dto.setDaysDelinquent(loan.getDaysDelinquent() != null ? loan.getDaysDelinquent() : 0);
        dto.setPenaltyAccrued(loan.getPenaltyAccrued() != null ? loan.getPenaltyAccrued() : BigDecimal.ZERO);

        // Disbursement information
        dto.setDisbursementMethod(loan.getDisbursementMethod());
        dto.setTransactionReference(loan.getTransactionReference());
        dto.setDisbursementNotes(loan.getDisbursementNotes());
        dto.setNetDisbursementAmount(loan.getNetDisbursementAmount());

        // Disbursed by user
        if (loan.getDisbursedBy() != null) {
            dto.setDisbursedById(loan.getDisbursedBy().getId());
            dto.setDisbursedByName(loan.getDisbursedBy().getFullName());
        }

        // Write-off information
        dto.setWriteOffReason(loan.getWriteOffReason());
        dto.setWriteOffDate(loan.getWriteOffDate());
        dto.setWriteOffAmount(loan.getWriteOffAmount());
        if (loan.getWriteOffStatus() != null) {
            dto.setWriteOffStatus(loan.getWriteOffStatus().name());
        }
        if (loan.getWriteOffBy() != null) {
            dto.setWrittenOffBy(loan.getWriteOffBy().getFullName());
        }

        // Recovery plan
        dto.setRecoveryPlan(loan.getRecoveryPlan());

        // Timestamps
        dto.setCreatedAt(loan.getCreatedAt());
        dto.setUpdatedAt(loan.getUpdatedAt());

        // Calculate derived fields if needed
        if (loan.getRepaymentSchedules() != null && !loan.getRepaymentSchedules().isEmpty()) {
            // Find next due installment
            Optional<RepaymentSchedule> nextDue = loan.getRepaymentSchedules().stream()
                    .filter(s -> s.getStatus() ==  GeneralConfig.InstallmentStatus.PENDING)
                    .findFirst();

            nextDue.ifPresent(schedule -> {
                dto.setNextDueDate(schedule.getDueDate());
                dto.setNextDueAmount(schedule.getTotalDue());
            });

            // Calculate total arrears
            BigDecimal totalArrears = loan.getRepaymentSchedules().stream()
                    .filter(s -> s.isOverdue() && s.getOutstandingAmount() != null)
                    .map(RepaymentSchedule::getOutstandingAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setTotalArrears(totalArrears);

            // Calculate progress
            int totalInstallments = loan.getRepaymentSchedules().size();
            long paidInstallments = loan.getRepaymentSchedules().stream()
                    .filter(s -> s.getStatus() ==  GeneralConfig.InstallmentStatus.PAID)
                    .count();

            dto.setTotalInstallments(totalInstallments);
            dto.setInstallmentsPaid((int) paidInstallments);

            if (totalInstallments > 0) {
                double progress = (paidInstallments * 100.0) / totalInstallments;
                dto.setProgressPercentage(progress);
            }

            // Get upcoming installments (next 5)
            List<RepaymentSchedule> upcoming = loan.getRepaymentSchedules().stream()
                    .filter(s -> s.getStatus() ==  GeneralConfig.InstallmentStatus.PENDING)
                    .limit(5)
                    .collect(Collectors.toList());

            // Map to summary DTO if you have one
            // dto.setUpcomingInstallments(mapToSummaryList(upcoming));
        }

        // Get last payment
        if (loan.getRepayments() != null && !loan.getRepayments().isEmpty()) {
            LoanRepayment lastPayment = loan.getRepayments().stream()
                    .max(Comparator.comparing(LoanRepayment::getPaymentDate))
                    .orElse(null);

            if (lastPayment != null) {
                // dto.setLastPayment(mapToRepaymentSummary(lastPayment));
                dto.setLastPaymentDate(lastPayment.getPaymentDate());
                dto.setLastPaymentAmount(lastPayment.getAmountPaid());
            }
        }
    }

    
    public List<LoanDto> toDtoList(List<Loan> loans) {
        return toDtoList(loans, false, false);
    }
    
    public List<LoanDto> toDtoList(List<Loan> loans, boolean includeSchedules, boolean includeRepayments) {
        if (loans == null) {
            return List.of();
        }
        
        return loans.stream()
                .map(loan -> toDto(loan, includeSchedules, includeRepayments))
                .collect(Collectors.toList());
    }
}