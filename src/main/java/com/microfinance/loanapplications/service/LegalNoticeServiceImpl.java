// service/impl/LegalNoticeServiceImpl.java
package com.microfinance.loanapplications.service;

import com.microfinance.audit.service.AuditService;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.loanapplications.dto.collection.LegalNoticeDto;
import com.microfinance.loanapplications.dto.collection.RecordCollectionActionDto;
import com.microfinance.loanapplications.dto.collection.SendLegalNoticeDto;
import com.microfinance.loanapplications.dto.collection.UpdateNoticeStatusDto;
import com.microfinance.loanapplications.entity.*;
import com.microfinance.loanapplications.repository.*;
import com.microfinance.loanapplications.service.LegalNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.security.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegalNoticeServiceImpl implements LegalNoticeService {

    private final LegalNoticeRepository legalNoticeRepository;
    private final LoanRepository loanRepository;
    private final RecoveryCaseRepository recoveryCaseRepository;
    private final UserService userService;
    private final CollectionActionRepository collectionActionRepository;
    private final CaseNoteRepository caseNoteRepository;

    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public LegalNoticeDto sendLegalNotice(SendLegalNoticeDto request, User currentUser) {
        log.info("Sending legal notice for loan: {}, recovery case: {}", request.getLoanId(), request.getRecoveryCaseId());

        // Generate unique notice number
        String noticeNumber = generateNoticeNumber();

        // Get loan if provided
        Loan loan = null;
        if (request.getLoanId() != null) {
            loan = loanRepository.findById(request.getLoanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Loan not found with ID: " + request.getLoanId()));
        }

        // Get recovery case if provided
        RecoveryCase recoveryCase = null;
        if (request.getRecoveryCaseId() != null) {
            recoveryCase = recoveryCaseRepository.findById(request.getRecoveryCaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recovery case not found with ID: " + request.getRecoveryCaseId()));
        }

        // Get assigned officer
        User assignedOfficer = null;
        if (request.getAssignedOfficerId() != null) {
            assignedOfficer = userService.getUserById(request.getAssignedOfficerId());
        }

        LegalNotice notice = LegalNotice.builder()
                .noticeNumber(noticeNumber)
                .loan(loan)
                .recoveryCase(recoveryCase)
                .assignedOfficer(assignedOfficer)
                .noticeType(request.getNoticeType())
                .noticeDate(request.getNoticeDate())
                .complianceDate(request.getComplianceDate())
                .status("PENDING")
                .reason(request.getReason())
                .legalGrounds(request.getLegalGrounds())
                .additionalNotes(request.getAdditionalNotes())
                .deliveryMethod(request.getDeliveryMethod())
                .generateDocument(request.getGenerateDocument() != null ? request.getGenerateDocument() : true)
                .notifyLegalTeam(request.getNotifyLegalTeam() != null ? request.getNotifyLegalTeam() : true)
                .attachToCase(request.getAttachToCase() != null ? request.getAttachToCase() : true)
                .createdDate(LocalDateTime.now())
                .build();

        notice.setCreatedBy(currentUser.getId());
        notice.setCreatedAt(LocalDateTime.now());
        notice.setUpdatedAt(LocalDateTime.now());

        LegalNotice savedNotice = legalNoticeRepository.save(notice);

        // UPDATE RECOVERY CASE STAGE
        if (recoveryCase != null) {
            updateRecoveryCaseStageForLegal(recoveryCase, savedNotice, currentUser);
        }

        // Update recovery case stage if attached
        if (savedNotice.getAttachToCase() && recoveryCase != null) {
            updateRecoveryCaseStage(recoveryCase, savedNotice, currentUser);
        }

        // Create collection action
        createCollectionActionForNotice(loan, recoveryCase, savedNotice, currentUser);

        // Add case note if attached to recovery case
        if (savedNotice.getAttachToCase() && recoveryCase != null) {
            addCaseNoteForNotice(recoveryCase, savedNotice, currentUser);
        }

        // Send notifications if requested
        if (savedNotice.getNotifyLegalTeam()) {
            notifyLegalTeam(savedNotice);
        }

        log.info("Legal notice sent with number: {}", noticeNumber);

        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedNotice.getId())) {
            auditService.masterAuditLogs(
                    savedNotice.getLoan().getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.LEGAL_NOTICE_ACTIVITY,
                    "LEGAL_NOTICE",
                    "Legal Notice of ID:"+savedNotice.getId()+" Loan No:"+savedNotice.getLoan().getLoanAccountNumber()+ " Legal No:"+savedNotice.getNoticeNumber()+  " has been SENT by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section

        return convertToDto(savedNotice);
    }


    private void updateRecoveryCaseStageForLegal(RecoveryCase recoveryCase, LegalNotice notice, User currentUser) {
        String currentStage = recoveryCase.getCurrentStage();
        String newStage = currentStage;

        // Determine the appropriate stage based on notice type
        if ("SUMMONS".equals(notice.getNoticeType()) || "COURT_ORDER".equals(notice.getNoticeType())) {
            newStage = "COURT_CASE";
        } else if ("ASSET_SEIZURE".equals(notice.getNoticeType())) {
            newStage = "ASSET_SEIZURE";
        } else if ("DEMAND_LETTER".equals(notice.getNoticeType()) || "FINAL_WARNING".equals(notice.getNoticeType())) {
            newStage = "LEGAL_NOTICE";
        }

        // Only update if the new stage is further along in the workflow
        List<String> stageOrder = Arrays.asList("INITIAL_CONTACT", "PAYMENT_NEGOTIATION", "PAYMENT_PLAN",
                "FIELD_VISIT", "LEGAL_NOTICE", "COURT_CASE", "ASSET_SEIZURE");

        int currentIndex = stageOrder.indexOf(currentStage);
        int newIndex = stageOrder.indexOf(newStage);

        if (newIndex > currentIndex) {
            recoveryCase.setCurrentStage(newStage);

            // Add stage date
            if (recoveryCase.getStageDates() == null) {
                recoveryCase.setStageDates(new ArrayList<>());
            }
            recoveryCase.getStageDates().add(StageDate.builder()
                    .stage(newStage)
                    .date(LocalDate.now())
                    .build());

         RecoveryCase recoveryCase1 = recoveryCaseRepository.save(recoveryCase);
            log.info("Updated recovery case {} stage from {} to {}",
                    recoveryCase.getCaseNumber(), currentStage, newStage);

            //Audit Section
            Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
            String createdByName ="";
            Long createdById=null;
            if(currentUser1.isPresent()){
                createdByName=currentUser1.get().getFullName();
                createdById=currentUser1.get().getId();
            }
            if (Objects.nonNull(recoveryCase1.getId())) {
                auditService.masterAuditLogs(
                        recoveryCase1.getLoan().getBorrower().getId(),
                        GeneralConfig.BorrowerActivityType.RECOVERY_CASE_ACTIVITY,
                        "RECOVERY_CASE",
                        "Recovery Case of ID:"+recoveryCase1.getId()+" Loan No:"+recoveryCase1.getLoan().getLoanAccountNumber()+" Updated from:"+ currentStage +" To:"+ newStage +" has been Created by:"+createdByName+"-"+createdById
                );
            }
            //End Audit Section


        }
    }


    @Override
    public Page<LegalNoticeDto> getLegalNotices(Long loanId, Long recoveryCaseId, Long assignedOfficerId,
                                                String noticeType, String status, LocalDate fromDate,
                                                LocalDate toDate, Pageable pageable, User currentUser) {
        log.info("Fetching legal notices with filters - loanId: {}, status: {}", loanId, status);

        Page<LegalNotice> notices = legalNoticeRepository.findAllWithFilters(
                loanId, recoveryCaseId, assignedOfficerId, noticeType, status, fromDate, toDate, pageable);

        return notices.map(this::convertToDto);
    }

    @Override
    public LegalNoticeDto getLegalNoticeById(Long noticeId, User currentUser) {
        log.info("Fetching legal notice by ID: {}", noticeId);

        LegalNotice notice = legalNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("Legal notice not found with ID: " + noticeId));

        return convertToDto(notice);
    }

    @Override
    public List<LegalNoticeDto> getNoticesByLoanId(Long loanId, User currentUser) {
        log.info("Fetching legal notices for loan: {}", loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with ID: " + loanId));

        return legalNoticeRepository.findByLoan(loan).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LegalNoticeDto> getNoticesByRecoveryCaseId(Long recoveryCaseId, User currentUser) {
        log.info("Fetching legal notices for recovery case: {}", recoveryCaseId);

        RecoveryCase recoveryCase = recoveryCaseRepository.findById(recoveryCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery case not found with ID: " + recoveryCaseId));

        return legalNoticeRepository.findByRecoveryCase(recoveryCase).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LegalNoticeDto updateNoticeStatus(Long noticeId, UpdateNoticeStatusDto request, User currentUser) {
        log.info("Updating notice status for notice: {} to: {}", noticeId, request.getStatus());

        LegalNotice notice = legalNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("Legal notice not found with ID: " + noticeId));

        String oldStatus = notice.getStatus();
        notice.setStatus(request.getStatus());
        
        if ("ACKNOWLEDGED".equals(request.getStatus())) {
            notice.setAcknowledgedDate(request.getAcknowledgedDate() != null ? 
                request.getAcknowledgedDate() : LocalDate.now());
            notice.setAcknowledgedBy(request.getAcknowledgedBy());
            notice.setAcknowledgementNotes(request.getAcknowledgementNotes());
        } else if ("COMPLIED".equals(request.getStatus())) {
            notice.setAcknowledgedDate(request.getAcknowledgedDate() != null ? 
                request.getAcknowledgedDate() : LocalDate.now());
            notice.setAcknowledgedBy(request.getAcknowledgedBy());
            notice.setAcknowledgementNotes(request.getAcknowledgementNotes());
        } else if ("SENT".equals(request.getStatus())) {
            notice.setSentDate(LocalDate.now());
        }

        LegalNotice savedNotice = legalNoticeRepository.save(notice);

        // Update collection action
        updateCollectionActionForNotice(savedNotice, currentUser);

        // Add case note for status change
        if (savedNotice.getAttachToCase() && savedNotice.getRecoveryCase() != null) {
            addStatusChangeNote(savedNotice.getRecoveryCase(), savedNotice, oldStatus, currentUser);
        }

        log.info("Notice status updated for: {} from {} to {}", notice.getNoticeNumber(), oldStatus, request.getStatus());

        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedNotice.getId())) {
            auditService.masterAuditLogs(
                    savedNotice.getLoan().getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.LEGAL_NOTICE_ACTIVITY,
                    "LEGAL_NOTICE",
                    "Legal Notice of ID:"+savedNotice.getId()+" Loan No:"+savedNotice.getLoan().getLoanAccountNumber()+" Notice No:"+notice.getNoticeNumber()+" Updated from:"+ oldStatus +" To:"+ request.getStatus() +" has been Created by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section


        return convertToDto(savedNotice);
    }

    @Override
    @Transactional
    public LegalNoticeDto cancelNotice(Long noticeId, String reason, User currentUser) {
        log.info("Cancelling legal notice: {} with reason: {}", noticeId, reason);

        LegalNotice notice = legalNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("Legal notice not found with ID: " + noticeId));

        String oldStatus = notice.getStatus();
        notice.setStatus("CANCELLED");
        notice.setAdditionalNotes(notice.getAdditionalNotes() != null ? 
            notice.getAdditionalNotes() + "\nCancelled: " + reason : "Cancelled: " + reason);

        LegalNotice savedNotice = legalNoticeRepository.save(notice);

        // Update collection action
        updateCollectionActionForNotice(savedNotice, currentUser);

        // Add case note for cancellation
        if (savedNotice.getAttachToCase() && savedNotice.getRecoveryCase() != null) {
            addCancellationNote(savedNotice.getRecoveryCase(), savedNotice, reason, currentUser);
        }

        log.info("Legal notice cancelled: {}", notice.getNoticeNumber());


        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedNotice.getId())) {
            auditService.masterAuditLogs(
                    savedNotice.getLoan().getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.LEGAL_NOTICE_ACTIVITY,
                    "LEGAL_NOTICE",
                    "Legal Notice of ID:"+savedNotice.getId()+" Loan No:"+savedNotice.getLoan().getLoanAccountNumber()+" Notice No:"+notice.getNoticeNumber()+" has been CANCELLED  by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section


        return convertToDto(savedNotice);
    }

    @Override
    public byte[] generateDocument(Long noticeId, User currentUser) {
        log.info("Generating document for legal notice: {}", noticeId);

        LegalNotice notice = legalNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("Legal notice not found with ID: " + noticeId));

        // TODO: Implementation for PDF generation
        // This would generate a formal legal document based on notice type
        // You would typically use a library like iText, Apache PDFBox, or JasperReports
        
        log.info("Document generation requested for notice: {}", notice.getNoticeNumber());
        
        return new byte[0]; // Placeholder - return actual PDF bytes in production
    }

    @Override
    public byte[] downloadDocument(Long noticeId, User currentUser) {
        log.info("Downloading document for legal notice: {}", noticeId);

        LegalNotice notice = legalNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("Legal notice not found with ID: " + noticeId));

        // TODO: Implementation for document download
        // This would retrieve the generated document from file system or database
        
        log.info("Document download requested for notice: {}", notice.getNoticeNumber());
        
        return new byte[0]; // Placeholder - return actual PDF bytes in production
    }

    @Override
    @Transactional
    public void sendComplianceReminder(Long noticeId, User currentUser) {
        log.info("Sending compliance reminder for notice: {}", noticeId);

        LegalNotice notice = legalNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("Legal notice not found with ID: " + noticeId));

        // Create a collection action for the reminder
        createReminderCollectionAction(notice, currentUser);

        // TODO: Implementation for sending reminder (SMS/Email)
        // This would integrate with your notification service

        log.info("Compliance reminder sent for notice: {}", notice.getNoticeNumber());


        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(notice.getId())) {
            auditService.masterAuditLogs(
                    notice.getLoan().getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.LEGAL_NOTICE_ACTIVITY,
                    "LEGAL_NOTICE",
                    "Legal Notice of ID:"+notice.getId()+" Loan No:"+notice.getLoan().getLoanAccountNumber()+" Notice No:"+notice.getNoticeNumber()+" has COMPLIANCE REMINDER sent  by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section
    }

    @Override
    public byte[] exportNotices(Long loanId, Long recoveryCaseId, Long assignedOfficerId,
                               String noticeType, String status, LocalDate fromDate,
                               LocalDate toDate, String format, User currentUser) {
        log.info("Exporting legal notices with filters");

        Page<LegalNotice> notices = legalNoticeRepository.findAllWithFilters(
                loanId, recoveryCaseId, assignedOfficerId, noticeType, status, fromDate, toDate, Pageable.unpaged());

        // TODO: Implementation for PDF/Excel export
        // This would generate the report using a library like Apache POI for Excel
        // or iText for PDF
        
        log.info("Exporting {} legal notices in {} format", notices.getTotalElements(), format);
        
        return new byte[0]; // Placeholder - return actual report bytes in production
    }

    @Override
    public Map<String, Object> getNoticeStatistics(User currentUser) {
        log.info("Fetching legal notice statistics");

        Map<String, Object> statistics = new HashMap<>();
        
        statistics.put("totalNotices", legalNoticeRepository.count());
        statistics.put("pendingNotices", legalNoticeRepository.countByStatus("PENDING"));
        statistics.put("sentNotices", legalNoticeRepository.countByStatus("SENT"));
        statistics.put("acknowledgedNotices", legalNoticeRepository.countByStatus("ACKNOWLEDGED"));
        statistics.put("compliedNotices", legalNoticeRepository.countByStatus("COMPLIED"));
        statistics.put("defaultedNotices", legalNoticeRepository.countByStatus("DEFAULTED"));
        statistics.put("cancelledNotices", legalNoticeRepository.countByStatus("CANCELLED"));
        
        // Calculate compliance rate
        long totalCompleted = (Long) statistics.get("compliedNotices");
        long totalSent = (Long) statistics.get("sentNotices");
        long totalAcknowledged = (Long) statistics.get("acknowledgedNotices");
        long totalProcessed = totalCompleted + totalAcknowledged;
        
        double complianceRate = totalSent > 0 ? (double) totalCompleted / totalSent * 100 : 0;
        statistics.put("complianceRate", Math.round(complianceRate * 100.0) / 100.0);
        
        // Get counts by notice type
        List<Object[]> noticeTypeCounts = legalNoticeRepository.countByNoticeType();
        Map<String, Long> byNoticeType = new HashMap<>();
        for (Object[] row : noticeTypeCounts) {
            byNoticeType.put((String) row[0], (Long) row[1]);
        }
        statistics.put("byNoticeType", byNoticeType);
        
        // Get counts by status
        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("PENDING", (Long) statistics.get("pendingNotices"));
        byStatus.put("SENT", (Long) statistics.get("sentNotices"));
        byStatus.put("ACKNOWLEDGED", (Long) statistics.get("acknowledgedNotices"));
        byStatus.put("COMPLIED", (Long) statistics.get("compliedNotices"));
        byStatus.put("DEFAULTED", (Long) statistics.get("defaultedNotices"));
        byStatus.put("CANCELLED", (Long) statistics.get("cancelledNotices"));
        statistics.put("byStatus", byStatus);
        
        return statistics;
    }

    // ==================== HELPER METHODS ====================

    private String generateNoticeNumber() {
        String prefix = "LN";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = legalNoticeRepository.count();
        String sequence = String.format("%04d", (int) (count % 10000));
        
        String noticeNumber = prefix + "-" + datePart + "-" + sequence;
        
        // Ensure uniqueness
        while (legalNoticeRepository.existsByNoticeNumber(noticeNumber)) {
            int seqNum = Integer.parseInt(sequence) + 1;
            sequence = String.format("%04d", seqNum % 10000);
            noticeNumber = prefix + "-" + datePart + "-" + sequence;
        }
        
        return noticeNumber;
    }

    private void updateRecoveryCaseStage(RecoveryCase recoveryCase, LegalNotice notice, User currentUser) {
        // Update recovery case stage based on notice type
        String newStage = recoveryCase.getCurrentStage();
        
        if ("SUMMONS".equals(notice.getNoticeType()) || "COURT_ORDER".equals(notice.getNoticeType())) {
            newStage = "COURT_CASE";
        } else if ("ASSET_SEIZURE".equals(notice.getNoticeType())) {
            newStage = "ASSET_SEIZURE";
        } else if ("DEMAND_LETTER".equals(notice.getNoticeType()) || "FINAL_WARNING".equals(notice.getNoticeType())) {
            newStage = "LEGAL_NOTICE";
        }
        
        if (!newStage.equals(recoveryCase.getCurrentStage())) {
            recoveryCase.setCurrentStage(newStage);
            recoveryCaseRepository.save(recoveryCase);
            
            // Add stage date
            // This would add a stage date entry in your StageDate entity
            log.info("Updated recovery case {} stage to: {}", recoveryCase.getCaseNumber(), newStage);
        }
    }

    private void createCollectionActionForNotice(Loan loan, RecoveryCase recoveryCase, 
                                                  LegalNotice notice, User currentUser) {
        CollectionAction action = CollectionAction.builder()
                .loan(loan)
                .actionType(GeneralConfig.ActionType.LEGAL_NOTICE)
                .actionStatus(GeneralConfig.ActionStatus.COMPLETED)
                .actionDate(notice.getNoticeDate())
                .contactPerson(loan != null && loan.getBorrower() != null ? 
                    loan.getBorrower().getFullName() : null)
                .contactNumber(loan != null && loan.getBorrower() != null ? 
                    loan.getBorrower().getPhoneNumber() : null)
                .outcome(GeneralConfig.Outcome.PENDING)
                .notes(String.format("Legal notice (%s) sent on %s. Notice #: %s. Compliance deadline: %s. Reason: %s", 
                        notice.getNoticeType(), notice.getNoticeDate(), notice.getNoticeNumber(), 
                        notice.getComplianceDate(), notice.getReason()))
                .followUpDate(notice.getComplianceDate())
                .assignedTo(notice.getAssignedOfficer())
                .performedBy(currentUser)
                .build();
        
        collectionActionRepository.save(action);
        log.info("Created collection action for legal notice: {}", notice.getNoticeNumber());
    }

    private void updateCollectionActionForNotice(LegalNotice notice, User currentUser) {
        // Find the related collection action by loan and action type
        if (notice.getLoan() != null) {
            Optional<CollectionAction> actionOpt = collectionActionRepository
                    .findTopByLoanIdAndActionTypeOrderByActionDateDesc(
                        notice.getLoan().getId(), 
                        GeneralConfig.ActionType.LEGAL_NOTICE
                    );
            
            actionOpt.ifPresent(action -> {
                // Update action status based on notice status
                GeneralConfig.ActionStatus newStatus = mapNoticeStatusToActionStatus(notice.getStatus());
                action.setActionStatus(newStatus);
                
                // Update outcome based on notice status
                GeneralConfig.Outcome newOutcome = mapNoticeStatusToOutcome(notice.getStatus());
                action.setOutcome(newOutcome);
                
                // Add additional notes
                String updatedNotes = action.getNotes() + String.format(
                    "\n[%s] Notice status updated to: %s", 
                    LocalDate.now(), notice.getStatus()
                );
                
                if (notice.getAcknowledgedDate() != null) {
                    updatedNotes += String.format(". Acknowledged on: %s by: %s", 
                        notice.getAcknowledgedDate(), notice.getAcknowledgedBy());
                }
                
                if (notice.getAcknowledgementNotes() != null && !notice.getAcknowledgementNotes().isEmpty()) {
                    updatedNotes += String.format(". Notes: %s", notice.getAcknowledgementNotes());
                }
                
                action.setNotes(updatedNotes);
                action.setFollowUpDate(notice.getComplianceDate());
                
                collectionActionRepository.save(action);
                log.info("Updated collection action for legal notice: {}", notice.getNoticeNumber());
            });
        }
    }

    private void createReminderCollectionAction(LegalNotice notice, User currentUser) {
        if (notice.getLoan() != null) {
            CollectionAction action = CollectionAction.builder()
                    .loan(notice.getLoan())
                    .actionType(GeneralConfig.ActionType.EMAIL)
                    .actionStatus(GeneralConfig.ActionStatus.COMPLETED)
                    .actionDate(LocalDate.now())
                    .contactPerson(notice.getLoan().getBorrower() != null ? 
                        notice.getLoan().getBorrower().getFullName() : null)
                    .contactNumber(notice.getLoan().getBorrower() != null ? 
                        notice.getLoan().getBorrower().getPhoneNumber() : null)
                    .outcome(GeneralConfig.Outcome.PENDING)
                    .notes(String.format("Compliance reminder sent for legal notice %s. Compliance deadline: %s", 
                            notice.getNoticeNumber(), notice.getComplianceDate()))
                    .followUpDate(notice.getComplianceDate())
                    .assignedTo(notice.getAssignedOfficer())
                    .performedBy(currentUser)
                    .build();
            
            collectionActionRepository.save(action);
            log.info("Created reminder collection action for notice: {}", notice.getNoticeNumber());
        }
    }

    private void addCaseNoteForNotice(RecoveryCase recoveryCase, LegalNotice notice, User currentUser) {
        CaseNote caseNote = CaseNote.builder()
                .recoveryCase(recoveryCase)
                .content(String.format("Legal notice issued - Type: %s, Notice #: %s, Compliance Deadline: %s\nReason: %s", 
                        notice.getNoticeType(), notice.getNoticeNumber(), notice.getComplianceDate(), notice.getReason()))
                .type("LEGAL_NOTICE")
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();
        
        caseNoteRepository.save(caseNote);
        log.info("Added case note for legal notice: {}", notice.getNoticeNumber());
    }

    private void addStatusChangeNote(RecoveryCase recoveryCase, LegalNotice notice, 
                                     String oldStatus, User currentUser) {
        CaseNote caseNote = CaseNote.builder()
                .recoveryCase(recoveryCase)
                .content(String.format("Legal notice %s status changed from %s to %s", 
                        notice.getNoticeNumber(), oldStatus, notice.getStatus()))
                .type("SYSTEM")
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();
        
        caseNoteRepository.save(caseNote);
        log.info("Added status change note for legal notice: {}", notice.getNoticeNumber());
    }

    private void addCancellationNote(RecoveryCase recoveryCase, LegalNotice notice, 
                                     String reason, User currentUser) {
        CaseNote caseNote = CaseNote.builder()
                .recoveryCase(recoveryCase)
                .content(String.format("Legal notice %s has been cancelled. Reason: %s", 
                        notice.getNoticeNumber(), reason))
                .type("SYSTEM")
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();
        
        caseNoteRepository.save(caseNote);
        log.info("Added cancellation note for legal notice: {}", notice.getNoticeNumber());
    }

    private void notifyLegalTeam(LegalNotice notice) {
        // TODO: Implementation for sending notification to legal team
        // This would integrate with your notification service (email, SMS, or in-app notification)
        log.info("Notifying legal team for notice: {}", notice.getNoticeNumber());
    }

    // ==================== MAPPING METHODS ====================

    private GeneralConfig.ActionStatus mapNoticeStatusToActionStatus(String noticeStatus) {
        switch (noticeStatus) {
            case "SENT":
                return GeneralConfig.ActionStatus.COMPLETED;
            case "ACKNOWLEDGED":
                return GeneralConfig.ActionStatus.COMPLETED;
            case "COMPLIED":
                return GeneralConfig.ActionStatus.COMPLETED;
            case "DEFAULTED":
                return GeneralConfig.ActionStatus.FAILED;
            case "CANCELLED":
                return GeneralConfig.ActionStatus.CANCELLED;
            default:
                return GeneralConfig.ActionStatus.PENDING;
        }
    }

    private GeneralConfig.Outcome mapNoticeStatusToOutcome(String noticeStatus) {
        switch (noticeStatus) {
            case "COMPLIED":
                return GeneralConfig.Outcome.SUCCESSFUL;
            case "DEFAULTED":
                return GeneralConfig.Outcome.UNSUCCESSFUL;
            case "ACKNOWLEDGED":
                return GeneralConfig.Outcome.PARTIAL;
            default:
                return GeneralConfig.Outcome.PENDING;
        }
    }

    // ==================== CONVERSION METHODS ====================

    private LegalNoticeDto convertToDto(LegalNotice notice) {
        LegalNoticeDto dto = new LegalNoticeDto();
        
        dto.setId(notice.getId());
        dto.setNoticeNumber(notice.getNoticeNumber());
        
        if (notice.getLoan() != null) {
            dto.setLoanId(notice.getLoan().getId());
            dto.setLoanNumber(notice.getLoan().getLoanAccountNumber());
            if (notice.getLoan().getBorrower() != null) {
                dto.setBorrowerName(notice.getLoan().getBorrower().getFullName());
                dto.setBorrowerPhone(notice.getLoan().getBorrower().getPhoneNumber());
                dto.setBorrowerEmail(notice.getLoan().getBorrower().getEmail());
            }
        }
        
        if (notice.getRecoveryCase() != null) {
            dto.setRecoveryCaseId(notice.getRecoveryCase().getId());
        }
        
        dto.setNoticeType(notice.getNoticeType());
        dto.setNoticeDate(notice.getNoticeDate());
        dto.setComplianceDate(notice.getComplianceDate());
        dto.setStatus(notice.getStatus());
        dto.setReason(notice.getReason());
        dto.setLegalGrounds(notice.getLegalGrounds());
        dto.setAdditionalNotes(notice.getAdditionalNotes());
        dto.setDeliveryMethod(notice.getDeliveryMethod());
        dto.setDocumentPath(notice.getDocumentPath());
        dto.setSentDate(notice.getSentDate());
        dto.setAcknowledgedDate(notice.getAcknowledgedDate());
        dto.setAcknowledgedBy(notice.getAcknowledgedBy());
        dto.setAcknowledgementNotes(notice.getAcknowledgementNotes());
        dto.setGenerateDocument(notice.getGenerateDocument());
        dto.setNotifyLegalTeam(notice.getNotifyLegalTeam());
        dto.setAttachToCase(notice.getAttachToCase());
        
        if (notice.getAssignedOfficer() != null) {
            dto.setAssignedOfficerId(notice.getAssignedOfficer().getId());
            dto.setAssignedOfficerName(notice.getAssignedOfficer().getFullName());
        }
        
        if (notice.getCreatedBy() != null) {
            User user= userService.getUserById(notice.getCreatedBy());

            dto.setCreatedBy(user.getFullName());
        }
        dto.setCreatedDate(notice.getCreatedDate());
        dto.setLastModifiedDate(notice.getLastModifiedDate());
        
        return dto;
    }
}