package com.microfinance.loanapplications.mapper;

import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.dto.LoanDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EfficientLoanMapper {
    
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