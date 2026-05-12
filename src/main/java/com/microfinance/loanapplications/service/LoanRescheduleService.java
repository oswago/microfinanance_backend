package com.microfinance.loanapplications.service;

import com.microfinance.loanapplications.dto.LoanDto;
import com.microfinance.loanapplications.dto.RescheduleRequestDto;
import com.microfinance.loanapplications.dto.disbursement.RescheduleApprovalDto;
import com.microfinance.loanapplications.dto.approval.ApprovalDecisionDto;
import com.microfinance.base.entity.User;
import com.microfinance.loanapplications.dto.disbursement.RescheduleEligibilityDto;
import com.microfinance.loanapplications.dto.rescheduling.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface LoanRescheduleService {
    
    // Core rescheduling operations
    LoanDto rescheduleLoan(Long loanId, RescheduleRequestDto dto, User currentUser);
    RescheduleApprovalDto submitRescheduleRequest(Long loanId, RescheduleRequestDto dto, User currentUser);
    RescheduleApprovalDto approveReschedule(Long requestId, ApprovalDecisionDto dto, User approver);
    RescheduleApprovalDto rejectReschedule(Long requestId, ApprovalDecisionDto dto, User approver);
    
    // Query methods
    List<RescheduleApprovalDto> getRescheduleHistory(Long loanId);
    List<RescheduleApprovalDto> getPendingRescheduleRequests();
    Page<RescheduleApprovalDto> getRescheduleRequestsByStatus(String status, Pageable pageable);
    RescheduleApprovalDto getRescheduleRequestById(Long requestId);
    
    // Validation and utility methods
    boolean canLoanBeRescheduled(Long loanId);
    RescheduleEligibilityDto checkRescheduleEligibility(Long loanId);
    List<String> getValidRescheduleReasons();

    Page<RescheduleApprovalDto> getReschedulingRequests(String status, Long branchId,
                                                        LocalDate startDate, LocalDate endDate,
                                                        Pageable pageable);



    RescheduleStatisticsDto getReschedulingStatistics();

    List<EligibleLoanDto> searchEligibleLoans(String searchTerm);


    @Transactional
    RescheduleApprovalDto createReschedulingRequest(CreateReschedulingRequestDto requestDto,
                                                    List<MultipartFile> documents,
                                                    User currentUser);

    RescheduleApprovalDto approveReschedulingRequest(Long requestId,
                                                     ApproveRejectRequestDto requestDto,
                                                     User approver);

    RescheduleApprovalDto rejectReschedulingRequest(Long requestId,
                                                    ApproveRejectRequestDto requestDto,
                                                    User approver);


    RescheduleApprovalDto requestMoreInfo(Long requestId, String message, User currentUser);

    RescheduleApprovalDto cancelReschedulingRequest(Long requestId, User currentUser);

    byte[] generateReschedulingReport(LocalDate startDate, LocalDate endDate, Long branchId);

    byte[] generateSingleReschedulingReport(Long requestId);

    ReschedulingDocumentDto getDocument(Long documentId);

    byte[] downloadDocument(Long documentId);

    List<RescheduleApprovalDto> getReschedulingHistory(Long loanId);

    RescheduleApprovalDto getReschedulingRequestById(Long id);

    byte[] generateAnalyticsReport(LocalDate startDate, LocalDate endDate, Long branchId);
}