package com.microfinance.loanapplications.mapper;

import com.microfinance.loanapplications.dto.repayment.RepaymentScheduleDto;
import com.microfinance.loanapplications.dto.repayment.RepaymentScheduleSummaryDto;
import com.microfinance.loanapplications.entity.RepaymentSchedule;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// RepaymentScheduleMapper.java
@Component
public class RepaymentScheduleMapper {
    
    // For detailed view with all fields
    public RepaymentScheduleDto toDto(RepaymentSchedule schedule) {
        if (schedule == null) return null;
        
        return RepaymentScheduleDto.builder()
            .id(schedule.getId())
            .installmentNumber(schedule.getInstallmentNumber())
            .dueDate(schedule.getDueDate())
            .principalAmount(schedule.getPrincipalAmount())
            .interestAmount(schedule.getInterestAmount())
            .totalDue(schedule.getTotalDue())
            .paidAmount(schedule.getPaidAmount())
            .outstandingAmount(schedule.getOutstandingAmount())
            .status(schedule.getStatus() != null ? schedule.getStatus().name() : null)
            .isOverdue(schedule.isOverdue())
            .paidDate(schedule.getPaidDate())
            .build();
    }
    
    public List<RepaymentScheduleDto> toDtoList(List<RepaymentSchedule> schedules) {
        if (schedules == null) return Collections.emptyList();
        return schedules.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    // For summary view (lighter version)
    public RepaymentScheduleSummaryDto toSummaryDto(RepaymentSchedule schedule) {
        if (schedule == null) return null;
        
        return RepaymentScheduleSummaryDto.builder()
            .id(schedule.getId())
            .installmentNumber(schedule.getInstallmentNumber())
            .dueDate(schedule.getDueDate())
            .totalDue(schedule.getTotalDue())
            .paidAmount(schedule.getPaidAmount())
            .outstandingAmount(schedule.getOutstandingAmount())
            .status(schedule.getStatus() != null ? schedule.getStatus().name() : null)
            .isOverdue(schedule.isOverdue())
            .paidDate(schedule.getPaidDate())
            .build();
    }
    
    public List<RepaymentScheduleSummaryDto> toSummaryDtoList(List<RepaymentSchedule> schedules) {
        if (schedules == null) return Collections.emptyList();
        return schedules.stream()
            .map(this::toSummaryDto)
            .collect(Collectors.toList());
    }
}