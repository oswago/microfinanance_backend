// src/main/java/com/microfinance/loanapplications/mapper/LoanRepaymentMapper.java
package com.microfinance.loanapplications.mapper;

import com.microfinance.loanapplications.dto.repayment.RepaymentDto;
import com.microfinance.loanapplications.dto.repayment.RepaymentSummaryDto;
import com.microfinance.loanapplications.entity.LoanRepayment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LoanRepaymentMapper {
    
    public RepaymentDto toDto(LoanRepayment repayment) {
        if (repayment == null) {
            return null;
        }
        
        RepaymentDto dto = new RepaymentDto();
        dto.setId(repayment.getId());
        dto.setAmountPaid(repayment.getAmountPaid());
        dto.setPaymentDate(repayment.getPaymentDate());
        dto.setPaymentMethod(String.valueOf(repayment.getPaymentMethod()));
        dto.setTransactionReference(repayment.getTransactionReference());
        dto.setReceiptNumber(repayment.getReceiptNumber());
        dto.setNotes(repayment.getNotes());
        
        // If you have status field
        if (repayment.getStatus() != null) {
            dto.setStatus(repayment.getStatus().name());
        }
        
        // If you have reference to loan
        if (repayment.getLoan() != null) {
            dto.setLoanId(repayment.getLoan().getId());
            dto.setLoanAccountNumber(repayment.getLoan().getLoanAccountNumber());
        }
        
        // If you have reference to user who recorded the payment
        if (repayment.getCreatedBy() != null) {
            dto.setRecordedBy(repayment.getCreatedBy().toString());
            dto.setRecordedByName(repayment.getCreatedBy().toString());
        }
        
        dto.setCreatedAt(repayment.getCreatedAt());
        
        return dto;
    }
    
    public List<RepaymentDto> toDtoList(List<LoanRepayment> repayments) {
        if (repayments == null || repayments.isEmpty()) {
            return Collections.emptyList();
        }
        
        return repayments.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    // For summary view (lighter version)
    public RepaymentSummaryDto toSummaryDto(LoanRepayment repayment) {
        if (repayment == null) {
            return null;
        }
        
        RepaymentSummaryDto dto = new RepaymentSummaryDto();
        dto.setId(repayment.getId());
        dto.setAmountPaid(repayment.getAmountPaid());
        dto.setPaymentDate(repayment.getPaymentDate());
        dto.setPaymentMethod(String.valueOf(repayment.getPaymentMethod()));
        dto.setTransactionReference(repayment.getTransactionReference());
        dto.setReceiptNumber(repayment.getReceiptNumber());
        
        return dto;
    }
    
    public List<RepaymentSummaryDto> toSummaryDtoList(List<LoanRepayment> repayments) {
        if (repayments == null || repayments.isEmpty()) {
            return Collections.emptyList();
        }
        
        return repayments.stream()
            .map(this::toSummaryDto)
            .collect(Collectors.toList());
    }
}