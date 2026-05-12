// service/LegalNoticeService.java
package com.microfinance.loanapplications.service;

import com.microfinance.base.entity.User;
import com.microfinance.loanapplications.dto.collection.LegalNoticeDto;
import com.microfinance.loanapplications.dto.collection.SendLegalNoticeDto;
import com.microfinance.loanapplications.dto.collection.UpdateNoticeStatusDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface LegalNoticeService {
    
    LegalNoticeDto sendLegalNotice(SendLegalNoticeDto request, User currentUser);
    
    Page<LegalNoticeDto> getLegalNotices(Long loanId, Long recoveryCaseId, Long assignedOfficerId,
                                         String noticeType, String status, LocalDate fromDate, 
                                         LocalDate toDate, Pageable pageable, User currentUser);
    
    LegalNoticeDto getLegalNoticeById(Long noticeId, User currentUser);
    
    List<LegalNoticeDto> getNoticesByLoanId(Long loanId, User currentUser);
    
    List<LegalNoticeDto> getNoticesByRecoveryCaseId(Long recoveryCaseId, User currentUser);
    
    LegalNoticeDto updateNoticeStatus(Long noticeId, UpdateNoticeStatusDto request, User currentUser);
    
    LegalNoticeDto cancelNotice(Long noticeId, String reason, User currentUser);
    
    byte[] generateDocument(Long noticeId, User currentUser);
    
    byte[] downloadDocument(Long noticeId, User currentUser);
    
    void sendComplianceReminder(Long noticeId, User currentUser);
    
    byte[] exportNotices(Long loanId, Long recoveryCaseId, Long assignedOfficerId,
                        String noticeType, String status, LocalDate fromDate, 
                        LocalDate toDate, String format, User currentUser);
    
    Map<String, Object> getNoticeStatistics(User currentUser);
}