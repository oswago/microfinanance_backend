package com.microfinance.loanapplications.service;

import com.microfinance.loanapplications.dto.*;
import com.microfinance.base.entity.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface LoanApplicationService {
    LoanApplicationDto createApplication(CreateLoanApplicationDto dto, User currentUser);
    LoanApplicationDto submitForApproval(Long applicationId, SubmitApplicationDto dto, User currentUser);
    LoanApplicationDto getApplicationById(Long id);
    LoanApplicationDto getApplicationByNumber(String applicationNumber);
    List<LoanApplicationDto> getApplicationsByBorrower(Long borrowerId);
    List<LoanApplicationDto> getDraftApplications(User currentUser);
    List<LoanApplicationDto> getPendingApprovals();
    Page<LoanApplicationDto> getApplicationsByStatus(String status, Pageable pageable);
    void deleteDraftApplication(Long applicationId, User currentUser);
    LoanApplicationDto updateApplication(Long applicationId, CreateLoanApplicationDto dto, User currentUser);
    LoanApplicationDto cancelApplication(Long applicationId, String reason, User currentUser);

    ApplicationStatsDto getApplicationStatistics(Long branchId, LocalDate startDate, LocalDate endDate);

    DocumentComplianceSummary checkDocumentCompliance(Long borrowerId, Long loanProductId);

    Boolean validateApplicationRequirements(@NotNull Long borrowerId, @NotNull Long loanProductId);
}