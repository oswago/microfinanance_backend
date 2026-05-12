package com.microfinance.loanapplications.mapper;

import com.microfinance.borrower.entity.Borrower;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.entity.LoanReschedule;
import com.microfinance.loanapplications.dto.RescheduleRequestDto;
import com.microfinance.loanapplications.dto.disbursement.RescheduleApprovalDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RescheduleMapper {

    public RescheduleApprovalDto toApprovalDto(LoanReschedule reschedule) {
        if (reschedule == null) {
            return null;
        }

        RescheduleApprovalDto dto = new RescheduleApprovalDto();

        // Basic information (existing)
        dto.setId(reschedule.getId());

        // Loan information (existing)
        if (reschedule.getLoan() != null) {
            Loan loan = reschedule.getLoan();
            dto.setLoanId(loan.getId());
            dto.setLoanAccountNumber(loan.getLoanAccountNumber());

            // ADD THESE NEW LOAN FINANCIAL FIELDS (preserving existing ones)
            dto.setLoanAmount(loan.getPrincipalAmount());
            dto.setOutstandingBalance(loan.getOutstandingBalance() != null ?
                    loan.getOutstandingBalance() : loan.getPrincipalAmount());
            dto.setDaysOverdue(loan.getDaysDelinquent() != null ? loan.getDaysDelinquent() : 0);

            // Borrower information (existing - preserved exactly)
            if (loan.getBorrower() != null) {
                Borrower borrower = loan.getBorrower();
                dto.setBorrowerName(borrower.getFirstName() + " " +
                        (borrower.getLastName() != null ? borrower.getLastName() : ""));
                dto.setBorrowerIdNumber(borrower.getBorrowerNumber());
                dto.setBorrowerId(borrower.getId());
            }

            // Branch information (existing)
            if (loan.getBranch() != null) {
                dto.setBranchId(loan.getBranch().getId());
                dto.setBranchName(loan.getBranch().getName());
            }
        }

        // Reschedule details (existing)
        dto.setOriginalMaturityDate(reschedule.getOriginalMaturityDate());
        dto.setNewMaturityDate(reschedule.getNewMaturityDate());
        dto.setExtensionMonths(reschedule.getExtensionMonths());
        dto.setReason(reschedule.getReason());
        dto.setStatus(reschedule.getStatus() != null ? reschedule.getStatus().name() : null);

        // Request information (existing)
        if (reschedule.getRequestedBy() != null) {
            dto.setRequestedByName(reschedule.getRequestedBy().getFirstName() + " " +
                    (reschedule.getRequestedBy().getLastName() != null ?
                            reschedule.getRequestedBy().getLastName() : ""));
        } else {
            dto.setRequestedByName("System");
        }
        dto.setRequestDate(reschedule.getRequestDate());

        // Approval information (existing)
        if (reschedule.getApprovedBy() != null) {
            dto.setApprovedByName(reschedule.getApprovedBy().getFirstName() + " " +
                    (reschedule.getApprovedBy().getLastName() != null ?
                            reschedule.getApprovedBy().getLastName() : ""));
        }
        dto.setApprovalDate(reschedule.getApprovalDate());
        dto.setApprovalNotes(reschedule.getApprovalComments());

        // Financial impact (existing)
        dto.setOriginalMonthlyPayment(reschedule.getOriginalMonthlyPayment());
        dto.setNewMonthlyPayment(reschedule.getNewMonthlyPayment());
        dto.setOriginalTermMonths(reschedule.getOriginalTermMonths());
        dto.setNewTermMonths(reschedule.getNewTermMonths());

        // Additional fields (existing)
        dto.setRejectionReason(reschedule.getRejectionReason());

        // CALCULATED FIELDS (new but won't break existing display)
        // Calculate payment change and percentage
        if (reschedule.getOriginalMonthlyPayment() != null && reschedule.getNewMonthlyPayment() != null) {
            BigDecimal paymentChange = reschedule.getNewMonthlyPayment()
                    .subtract(reschedule.getOriginalMonthlyPayment());
            dto.setMonthlyPaymentChange(paymentChange);

            if (reschedule.getOriginalMonthlyPayment().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal changePercent = paymentChange
                        .divide(reschedule.getOriginalMonthlyPayment(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                dto.setMonthlyPaymentChangePercent(changePercent);
            }
        }

        // Calculate processing days
        if (reschedule.getRequestDate() != null && reschedule.getApprovalDate() != null) {
            long days = ChronoUnit.DAYS.between(reschedule.getRequestDate(), reschedule.getApprovalDate());
            dto.setProcessingDays((int) days);
        }

        // Set status description
        if (reschedule.getStatus() != null) {
            switch (reschedule.getStatus()) {
                case PENDING_APPROVAL:
                    dto.setStatusDescription("Awaiting approval");
                    break;
                case UNDER_REVIEW:
                    dto.setStatusDescription("Under review");
                    break;
                case APPROVED:
                    dto.setStatusDescription("Approved");
                    break;
                case REJECTED:
                    dto.setStatusDescription("Rejected");
                    break;
                case CANCELLED:
                    dto.setStatusDescription("Cancelled");
                    break;
                default:
                    dto.setStatusDescription(reschedule.getStatus().name());
            }
        }

        return dto;
    }
    
    public List<RescheduleApprovalDto> toApprovalDtoList(List<LoanReschedule> reschedules) {
        if (reschedules == null) {
            return List.of();
        }
        
        return reschedules.stream()
                .map(this::toApprovalDto)
                .collect(Collectors.toList());
    }
    
    public RescheduleRequestDto toRequestDto(LoanReschedule reschedule) {
        if (reschedule == null) {
            return null;
        }
        
        RescheduleRequestDto dto = new RescheduleRequestDto();
        dto.setNewMaturityDate(calculateNewMaturityDate(reschedule));
        dto.setReason(reschedule.getReason());
        dto.setAdditionalNotes(reschedule.getApprovalComments());
        dto.setExtensionMonths(calculateExtensionMonths(reschedule));
        
        return dto;
    }
    
    // Helper methods for date calculations
    private LocalDate calculateOriginalMaturityDate(LoanReschedule reschedule) {
        if (reschedule.getLoan() != null && reschedule.getLoan().getDisbursementDate() != null) {
            return reschedule.getLoan().getDisbursementDate()
                    .plusMonths(reschedule.getOriginalTenureMonths());
        }
        return null;
    }
    
    private LocalDate calculateNewMaturityDate(LoanReschedule reschedule) {
        if (reschedule.getLoan() != null && reschedule.getLoan().getDisbursementDate() != null) {
            return reschedule.getLoan().getDisbursementDate()
                    .plusMonths(reschedule.getNewTenureMonths());
        }
        return null;
    }
    
    private Integer calculateExtensionMonths(LoanReschedule reschedule) {
        if (reschedule.getOriginalTenureMonths() != null && reschedule.getNewTenureMonths() != null) {
            return reschedule.getNewTenureMonths() - reschedule.getOriginalTenureMonths();
        }
        return 0;
    }
    
    // Additional mapping methods for different use cases
    public RescheduleApprovalDto toBasicApprovalDto(LoanReschedule reschedule) {
        if (reschedule == null) {
            return null;
        }
        
        RescheduleApprovalDto dto = new RescheduleApprovalDto();
        dto.setId(reschedule.getId());
        dto.setLoanId(reschedule.getLoan() != null ? reschedule.getLoan().getId() : null);
        dto.setLoanAccountNumber(reschedule.getLoan() != null ? reschedule.getLoan().getLoanAccountNumber() : null);
        dto.setExtensionMonths(calculateExtensionMonths(reschedule));
        dto.setReason(reschedule.getReason());
        dto.setStatus(reschedule.getStatus() != null ? reschedule.getStatus().name() : null);
        dto.setRequestDate(reschedule.getRequestDate());
        
        return dto;
    }
    
    public List<RescheduleApprovalDto> toBasicApprovalDtoList(List<LoanReschedule> reschedules) {
        if (reschedules == null) {
            return List.of();
        }
        
        return reschedules.stream()
                .map(this::toBasicApprovalDto)
                .collect(Collectors.toList());
    }
    
    // Method to map from DTO to entity (for creating new reschedule requests)
    public LoanReschedule toEntity(RescheduleRequestDto dto) {
        if (dto == null) {
            return null;
        }
        
        LoanReschedule reschedule = new LoanReschedule();
        reschedule.setRequestDate(java.time.LocalDate.now());
        reschedule.setStatus(com.microfinance.common.config.GeneralConfig.RescheduleStatus.PENDING);
        reschedule.setReason(dto.getReason());
        reschedule.setApprovalComments(dto.getAdditionalNotes());
        
        // Note: Loan, tenure months, and payment amounts will be set in service layer
        // based on business logic and calculations
        
        return reschedule;
    }
}