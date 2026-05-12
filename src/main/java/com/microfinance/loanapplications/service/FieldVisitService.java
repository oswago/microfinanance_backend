// service/FieldVisitService.java
package com.microfinance.loanapplications.service;

import com.microfinance.base.entity.User;
import com.microfinance.loanapplications.dto.collection.FieldVisitDto;
import com.microfinance.loanapplications.dto.collection.ScheduleVisitDto;
import com.microfinance.loanapplications.dto.collection.UpdateVisitOutcomeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface FieldVisitService {
    
    FieldVisitDto scheduleVisit(ScheduleVisitDto request, User currentUser);
    
    Page<FieldVisitDto> getFieldVisits(Long loanId, Long recoveryCaseId, Long assignedOfficerId,
                                       String status, LocalDate fromDate, LocalDate toDate,
                                       Pageable pageable, User currentUser);
    
    FieldVisitDto getFieldVisitById(Long visitId, User currentUser);
    
    List<FieldVisitDto> getVisitsByLoanId(Long loanId, User currentUser);
    
    List<FieldVisitDto> getVisitsByRecoveryCaseId(Long recoveryCaseId, User currentUser);
    
    FieldVisitDto updateVisitOutcome(Long visitId, UpdateVisitOutcomeDto request, User currentUser);
    
    FieldVisitDto cancelVisit(Long visitId, String reason, User currentUser);
    
    FieldVisitDto rescheduleVisit(Long visitId, LocalDate newDate, LocalTime newTime, String reason, User currentUser);
    
    List<FieldVisitDto> getUpcomingVisits(User currentUser);
    
    void sendReminder(Long visitId, User currentUser);
    
    byte[] exportVisits(Long loanId, Long recoveryCaseId, Long assignedOfficerId,
                       String status, LocalDate fromDate, LocalDate toDate,
                       String format, User currentUser);
}