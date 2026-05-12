package com.microfinance.loanapplications.service;

import com.microfinance.base.entity.User;
import com.microfinance.loanapplications.dto.collection.*;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface RecoveryWorkflowService {

    RecoveryCaseDto getRecoveryCaseById(Long caseId, User currentUser);

    RecoveryCaseDto createRecoveryCase(CreateRecoveryCaseDto request, User currentUser);

    @Transactional
    RecoveryCaseDto updateRecoveryCaseAfterPayment(Long loanId, Double amountPaid,
                                                   LocalDate paymentDate, User currentUser);

    RecoveryCaseDto escalateCase(Long caseId, EscalateCaseDto request, User currentUser);

    RecoveryCaseDto completeStage(Long caseId, CompleteStageDto request, User currentUser);

    RecoveryCaseDto closeCase(Long caseId, String notes, User currentUser);

    CaseNoteDto addCaseNote(Long caseId, AddCaseNoteDto request, User currentUser);

    List<CaseNoteDto> getCaseNotes(Long caseId);

    @Transactional(readOnly = true)
    Page<RecoveryCaseDto> getRecoveryCases(String search, String status, String stage, String priority,
                                           Long assignedTo, int page, int size, String sortBy,
                                           String sortDirection, User currentUser);

    List<StageStatisticsDto> getStageStatistics();

    List<RecoveryAgentDto> getRecoveryAgents();

    RecoveryCaseDto assignCaseToAgent(Long caseId, Long agentId, User currentUser);

    byte[] exportWorkflowData(String search, String status, String stage, String priority,
                              Long assignedTo, String format, User currentUser);


}