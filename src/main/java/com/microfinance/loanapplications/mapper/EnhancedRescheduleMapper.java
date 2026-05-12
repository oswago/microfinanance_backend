package com.microfinance.loanapplications.mapper;

import com.microfinance.loanapplications.entity.LoanReschedule;
import com.microfinance.loanapplications.dto.RescheduleRequestDto;
import com.microfinance.loanapplications.dto.disbursement.RescheduleApprovalDto;
import com.microfinance.loanapplications.dto.disbursement.RescheduleDetailDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EnhancedRescheduleMapper {
    
    private final RescheduleMapper basicMapper;
    
    public EnhancedRescheduleMapper() {
        this.basicMapper = new RescheduleMapper();
    }
    
    public RescheduleApprovalDto toApprovalDto(LoanReschedule reschedule) {
        return basicMapper.toApprovalDto(reschedule);
    }
    
    public RescheduleDetailDto toDetailDto(LoanReschedule reschedule) {
        if (reschedule == null) {
            return null;
        }
        
        RescheduleDetailDto dto = new RescheduleDetailDto();
        
        // Map all basic fields
        RescheduleApprovalDto basicDto = basicMapper.toApprovalDto(reschedule);
        if (basicDto != null) {
            dto.setId(basicDto.getId());
            dto.setLoanId(basicDto.getLoanId());
            dto.setLoanAccountNumber(basicDto.getLoanAccountNumber());
            dto.setOriginalMaturityDate(basicDto.getOriginalMaturityDate());
            dto.setNewMaturityDate(basicDto.getNewMaturityDate());
            dto.setExtensionMonths(basicDto.getExtensionMonths());
            dto.setReason(basicDto.getReason());
            dto.setStatus(basicDto.getStatus());
            dto.setRequestedByName(basicDto.getRequestedByName());
            dto.setRequestDate(basicDto.getRequestDate());
            dto.setApprovedByName(basicDto.getApprovedByName());
            dto.setApprovalDate(basicDto.getApprovalDate());
            dto.setApprovalNotes(basicDto.getApprovalNotes());
            dto.setOriginalMonthlyPayment(basicDto.getOriginalMonthlyPayment());
            dto.setNewMonthlyPayment(basicDto.getNewMonthlyPayment());
        }
        
        // Additional detailed fields
        dto.setGracePeriodDays(reschedule.getGracePeriodDays());
        dto.setInterestRecalculation(reschedule.getInterestRecalculation());
        dto.setReschedulingFee(reschedule.getReschedulingFee());
        dto.setEffectiveDate(reschedule.getEffectiveDate());
        
        // Borrower information
        if (reschedule.getLoan() != null && reschedule.getLoan().getBorrower() != null) {
            dto.setBorrowerName(reschedule.getLoan().getBorrower().getFullName());
            dto.setBorrowerNumber(reschedule.getLoan().getBorrower().getBorrowerNumber());
        }
        
        // Loan product information
        if (reschedule.getLoan() != null && reschedule.getLoan().getLoanProduct() != null) {
            dto.setLoanProductName(reschedule.getLoan().getLoanProduct().getName());
            dto.setInterestRate(reschedule.getLoan().getInterestRate());
        }
        
        // Calculate derived fields
        dto.setMonthlyPaymentReduction(calculateMonthlyPaymentReduction(reschedule));
        dto.setTotalInterestImpact(calculateTotalInterestImpact(reschedule));
        
        return dto;
    }
    
    public List<RescheduleApprovalDto> toApprovalDtoList(List<LoanReschedule> reschedules) {
        return basicMapper.toApprovalDtoList(reschedules);
    }
    
    public List<RescheduleDetailDto> toDetailDtoList(List<LoanReschedule> reschedules) {
        if (reschedules == null) {
            return List.of();
        }
        
        return reschedules.stream()
                .map(this::toDetailDto)
                .collect(Collectors.toList());
    }
    
    // Helper methods for financial calculations
    private BigDecimal calculateMonthlyPaymentReduction(LoanReschedule reschedule) {
        if (reschedule.getOriginalMonthlyPayment() != null && reschedule.getNewMonthlyPayment() != null) {
            return reschedule.getOriginalMonthlyPayment().subtract(reschedule.getNewMonthlyPayment());
        }
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculateTotalInterestImpact(LoanReschedule reschedule) {
        if (reschedule.getOriginalMonthlyPayment() != null && reschedule.getNewMonthlyPayment() != null &&
            reschedule.getOriginalTenureMonths() != null && reschedule.getNewTenureMonths() != null) {
            
            BigDecimal originalTotal = reschedule.getOriginalMonthlyPayment()
                    .multiply(BigDecimal.valueOf(reschedule.getOriginalTenureMonths()));
            BigDecimal newTotal = reschedule.getNewMonthlyPayment()
                    .multiply(BigDecimal.valueOf(reschedule.getNewTenureMonths()));
            
            return newTotal.subtract(originalTotal);
        }
        return BigDecimal.ZERO;
    }
    
    // Method to update entity from DTO (for modifications)
    public void updateEntityFromDto(RescheduleRequestDto dto, LoanReschedule reschedule) {
        if (dto == null || reschedule == null) {
            return;
        }
        
        reschedule.setReason(dto.getReason());
        reschedule.setApprovalComments(dto.getAdditionalNotes());
        // Note: Maturity dates and tenure are calculated in service layer
    }
}