// service/impl/FieldVisitServiceImpl.java
package com.microfinance.loanapplications.service;

import com.microfinance.audit.service.AuditService;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.loanapplications.dto.collection.FieldVisitDto;
import com.microfinance.loanapplications.dto.collection.ScheduleVisitDto;
import com.microfinance.loanapplications.dto.collection.UpdateVisitOutcomeDto;
import com.microfinance.loanapplications.entity.*;
import com.microfinance.loanapplications.repository.*;
import com.microfinance.loanapplications.service.FieldVisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FieldVisitServiceImpl implements FieldVisitService {

    private final FieldVisitRepository fieldVisitRepository;
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
    public FieldVisitDto scheduleVisit(ScheduleVisitDto request, User currentUser) {
        log.info("Scheduling field visit for loan: {}, recovery case: {}", request.getLoanId(), request.getRecoveryCaseId());

        // Generate unique visit number
        String visitNumber = generateVisitNumber();

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

        FieldVisit visit = FieldVisit.builder()
                .visitNumber(visitNumber)
                .loan(loan)
                .recoveryCase(recoveryCase)
                .assignedOfficer(assignedOfficer)
                .visitDate(request.getVisitDate())
                .visitTime(request.getVisitTime())
                .visitAddress(request.getVisitAddress())
                .purpose(request.getPurpose())
                .status("SCHEDULED")
                .notes(request.getNotes())
                .notifyBorrower(request.getNotifyBorrower() != null ? request.getNotifyBorrower() : true)
                .sendReminder(request.getSendReminder() != null ? request.getSendReminder() : true)
                .reminderSent(false)
                .notificationSent(false)
                .createdDate(LocalDateTime.now())
                .build();

        visit.setCreatedBy(currentUser.getId());
        visit.setCreatedAt(LocalDateTime.now());
        visit.setUpdatedAt(LocalDateTime.now());

        FieldVisit savedVisit = fieldVisitRepository.save(visit);

        // UPDATE RECOVERY CASE STAGE
        if (recoveryCase != null) {
            updateRecoveryCaseStageForVisit(recoveryCase, savedVisit, currentUser);
        }

        // Create collection action for this visit
        createCollectionActionForVisit(loan, recoveryCase, savedVisit, currentUser);

        // Add case note if attached to recovery case
        if (recoveryCase != null) {
            addCaseNoteForVisit(recoveryCase, savedVisit, currentUser);
        }

        // Send notifications if requested
        if (savedVisit.getNotifyBorrower()) {
            sendBorrowerNotification(savedVisit);
        }

        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedVisit.getId())) {
            auditService.masterAuditLogs(
                    savedVisit.getLoan().getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.FIELD_VISIT_ACTIVITY,
                    "FIELD_VISIT",
                    "Field Visit of ID:"+savedVisit.getId()+" Loan No:"+savedVisit.getLoan().getLoanAccountNumber()+  " has been Created by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section

        log.info("Field visit scheduled with number: {}", visitNumber);

        return convertToDto(savedVisit);
    }

    private void updateRecoveryCaseStageForVisit(RecoveryCase recoveryCase, FieldVisit visit, User currentUser) {
        String currentStage = recoveryCase.getCurrentStage();
        String newStage = currentStage;

        // Only update if the current stage is before FIELD_VISIT in the workflow
        List<String> stageOrder = Arrays.asList("INITIAL_CONTACT", "PAYMENT_NEGOTIATION", "PAYMENT_PLAN",
                "FIELD_VISIT", "LEGAL_NOTICE", "COURT_CASE", "ASSET_SEIZURE");

        int currentIndex = stageOrder.indexOf(currentStage);
        int fieldVisitIndex = stageOrder.indexOf("FIELD_VISIT");

        if (currentIndex < fieldVisitIndex) {
            newStage = "FIELD_VISIT";
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


            log.info("Updated recovery case {} stage from {} to {}",
                    recoveryCase.getCaseNumber(), currentStage, newStage);
        }
    }


    @Override
    public Page<FieldVisitDto> getFieldVisits(Long loanId, Long recoveryCaseId, Long assignedOfficerId,
                                              String status, LocalDate fromDate, LocalDate toDate,
                                              Pageable pageable, User currentUser) {
        log.info("Fetching field visits with filters - loanId: {}, recoveryCaseId: {}, status: {}", 
                loanId, recoveryCaseId, status);

        Page<FieldVisit> visits = fieldVisitRepository.findAllWithFilters(
                loanId, recoveryCaseId, assignedOfficerId, status, fromDate, toDate, pageable);

        return visits.map(this::convertToDto);
    }

    @Override
    public FieldVisitDto getFieldVisitById(Long visitId, User currentUser) {
        log.info("Fetching field visit by ID: {}", visitId);

        FieldVisit visit = fieldVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Field visit not found with ID: " + visitId));

        return convertToDto(visit);
    }

    @Override
    public List<FieldVisitDto> getVisitsByLoanId(Long loanId, User currentUser) {
        log.info("Fetching field visits for loan: {}", loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with ID: " + loanId));

        return fieldVisitRepository.findByLoan(loan).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FieldVisitDto> getVisitsByRecoveryCaseId(Long recoveryCaseId, User currentUser) {
        log.info("Fetching field visits for recovery case: {}", recoveryCaseId);

        RecoveryCase recoveryCase = recoveryCaseRepository.findById(recoveryCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery case not found with ID: " + recoveryCaseId));

        return fieldVisitRepository.findByRecoveryCase(recoveryCase).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FieldVisitDto updateVisitOutcome(Long visitId, UpdateVisitOutcomeDto request, User currentUser) {
        log.info("Updating visit outcome for visit: {} to status: {}", visitId, request.getStatus());

        FieldVisit visit = fieldVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Field visit not found with ID: " + visitId));

        String oldStatus = visit.getStatus();
        visit.setStatus(request.getStatus());
        visit.setOutcome(request.getOutcome());
        visit.setCompletionNotes(request.getCompletionNotes());
        
        if (request.getCompletedDate() != null) {
            visit.setCompletedDate(request.getCompletedDate());
        } else if ("COMPLETED".equals(request.getStatus())) {
            visit.setCompletedDate(LocalDate.now());
        }

        FieldVisit savedVisit = fieldVisitRepository.save(visit);

        // Update collection action
        updateCollectionActionForVisit(savedVisit, currentUser);

        // Add case note for status change
        if (savedVisit.getRecoveryCase() != null) {
            addStatusChangeNote(savedVisit.getRecoveryCase(), savedVisit, oldStatus, currentUser);
        }

        log.info("Visit outcome updated for visit: {} from {} to {}", 
                visit.getVisitNumber(), oldStatus, request.getStatus());

        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedVisit.getId())) {
            auditService.masterAuditLogs(
                    savedVisit.getLoan().getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.FIELD_VISIT_ACTIVITY,
                    "FIELD_VISIT",
                    "Field Visit of ID:"+savedVisit.getId()+" Loan No:"+savedVisit.getLoan().getLoanAccountNumber()+" Updated from:"+ oldStatus +" To:"+ request.getStatus()+" has been Created by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section


        return convertToDto(savedVisit);
    }

    @Override
    @Transactional
    public FieldVisitDto cancelVisit(Long visitId, String reason, User currentUser) {
        log.info("Cancelling field visit: {} with reason: {}", visitId, reason);

        FieldVisit visit = fieldVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Field visit not found with ID: " + visitId));

        String oldStatus = visit.getStatus();
        visit.setStatus("CANCELLED");
        visit.setCompletionNotes("Cancelled: " + reason);
        visit.setCompletedDate(LocalDate.now());

        FieldVisit savedVisit = fieldVisitRepository.save(visit);

        // Update collection action
        updateCollectionActionForVisit(savedVisit, currentUser);

        // Add case note for cancellation
        if (savedVisit.getRecoveryCase() != null) {
            addCancellationNote(savedVisit.getRecoveryCase(), savedVisit, reason, currentUser);
        }

        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedVisit.getId())) {
            auditService.masterAuditLogs(
                    savedVisit.getLoan().getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.FIELD_VISIT_ACTIVITY,
                    "FIELD_VISIT",
                    "Field Visit of ID:"+savedVisit.getId()+" Loan No:"+savedVisit.getLoan().getLoanAccountNumber()+" has been CANCELED  by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section

        log.info("Field visit cancelled: {}", visit.getVisitNumber());

        return convertToDto(savedVisit);
    }

    @Override
    @Transactional
    public FieldVisitDto rescheduleVisit(Long visitId, LocalDate newDate, LocalTime newTime, String reason, User currentUser) {
        log.info("Rescheduling field visit: {} to date: {}", visitId, newDate);

        FieldVisit visit = fieldVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Field visit not found with ID: " + visitId));

        String oldDate = visit.getVisitDate().toString();
        String oldStatus = visit.getStatus();
        
        visit.setVisitDate(newDate);
        if (newTime != null) {
            visit.setVisitTime(newTime);
        }
        visit.setStatus("SCHEDULED");
        
        String rescheduleNote = String.format("Rescheduled from %s to %s. Reason: %s", 
                oldDate, newDate, reason != null ? reason : "Not specified");
        visit.setNotes(visit.getNotes() != null ? visit.getNotes() + "\n" + rescheduleNote : rescheduleNote);

        FieldVisit savedVisit = fieldVisitRepository.save(visit);

        // Update collection action
        updateCollectionActionForVisit(savedVisit, currentUser);

        // Add case note for reschedule
        if (savedVisit.getRecoveryCase() != null) {
            addRescheduleNote(savedVisit.getRecoveryCase(), savedVisit, oldDate, newDate, reason, currentUser);
        }

        // Send new notification if requested
        if (savedVisit.getNotifyBorrower()) {
            sendBorrowerNotification(savedVisit);
        }

        log.info("Field visit rescheduled: {}", visit.getVisitNumber());

        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedVisit.getId())) {
            auditService.masterAuditLogs(
                    savedVisit.getLoan().getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.FIELD_VISIT_ACTIVITY,
                    "FIELD_VISIT",
                    "Field Visit of ID:"+savedVisit.getId()+" Loan No:"+savedVisit.getLoan().getLoanAccountNumber()+" has been RESCHEDULED  by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section

        return convertToDto(savedVisit);
    }

    @Override
    public List<FieldVisitDto> getUpcomingVisits(User currentUser) {
        log.info("Fetching upcoming visits for user: {}", currentUser.getId());

        List<FieldVisit> visits = fieldVisitRepository.findUpcomingVisitsByOfficer(
                currentUser.getId(), LocalDate.now());

        return visits.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void sendReminder(Long visitId, User currentUser) {
        log.info("Sending reminder for visit: {}", visitId);

        FieldVisit visit = fieldVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Field visit not found with ID: " + visitId));

        // Create a collection action for the reminder
        createReminderCollectionAction(visit, currentUser);

        // Implementation for sending reminder (SMS/Email)
        // This would integrate with your notification service
        
        visit.setReminderSent(true);
        FieldVisit visit1 = fieldVisitRepository.save(visit);

        log.info("Reminder sent for visit: {}", visit.getVisitNumber());

        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(visit1.getId())) {
            auditService.masterAuditLogs(
                    visit1.getLoan().getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.FIELD_VISIT_ACTIVITY,
                    "FIELD_VISIT",
                    "Field Visit of ID:"+visit1.getId()+" Loan No:"+visit1.getLoan().getLoanAccountNumber()+" has REMINDER sent  by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section

    }

    @Override
    public byte[] exportVisits(Long loanId, Long recoveryCaseId, Long assignedOfficerId,
                              String status, LocalDate fromDate, LocalDate toDate,
                              String format, User currentUser) {
        log.info("Exporting field visits with filters");

        Page<FieldVisit> visits = fieldVisitRepository.findAllWithFilters(
                loanId, recoveryCaseId, assignedOfficerId, status, fromDate, toDate, Pageable.unpaged());

        // TODO: Implementation for PDF/Excel export
        // This would generate the report using a library like Apache POI for Excel
        // or iText for PDF
        
        log.info("Exporting {} field visits in {} format", visits.getTotalElements(), format);
        
        return new byte[0]; // Placeholder
    }

    // ==================== HELPER METHODS ====================

    private String generateVisitNumber() {
        String prefix = "VIS";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = fieldVisitRepository.count();
        String sequence = String.format("%04d", (int) (count % 10000));
        
        String visitNumber = prefix + "-" + datePart + "-" + sequence;
        
        // Ensure uniqueness
        while (fieldVisitRepository.existsByVisitNumber(visitNumber)) {
            int seqNum = Integer.parseInt(sequence) + 1;
            sequence = String.format("%04d", seqNum % 10000);
            visitNumber = prefix + "-" + datePart + "-" + sequence;
        }
        
        return visitNumber;
    }

    private void createCollectionActionForVisit(Loan loan, RecoveryCase recoveryCase, 
                                                 FieldVisit visit, User currentUser) {
        CollectionAction action = CollectionAction.builder()
                .loan(loan)
                .recoveryCase(recoveryCase)
                .actionType(GeneralConfig.ActionType.FIELD_VISIT)
                .actionStatus(GeneralConfig.ActionStatus.SCHEDULED)
                .actionDate(visit.getVisitDate())
                .actionTime(visit.getVisitTime())
                .contactPerson(loan != null && loan.getBorrower() != null ? 
                    loan.getBorrower().getFullName() : null)
                .contactNumber(loan != null && loan.getBorrower() != null ? 
                    loan.getBorrower().getPhoneNumber() : null)
                .contactMethod(GeneralConfig.ContactMethod.FIELD_VISIT)
                .outcome(GeneralConfig.Outcome.PENDING)
                .notes(String.format("Field visit scheduled for %s at %s. Purpose: %s. Address: %s. Visit #: %s", 
                        visit.getVisitDate(), visit.getVisitTime() != null ? visit.getVisitTime() : "Not specified",
                        visit.getPurpose(), visit.getVisitAddress() != null ? visit.getVisitAddress() : "Not specified",
                        visit.getVisitNumber()))
                .followUpDate(visit.getVisitDate())
                .visitAddress(visit.getVisitAddress())
                .assignedTo(visit.getAssignedOfficer())
                .performedBy(currentUser)
                .build();
        
        collectionActionRepository.save(action);
        log.info("Created collection action for field visit: {}", visit.getVisitNumber());
    }

    private void updateCollectionActionForVisit(FieldVisit visit, User currentUser) {
        // Find the related collection action by loan and action type
        if (visit.getLoan() != null) {
            Optional<CollectionAction> actionOpt = collectionActionRepository
                    .findTopByLoanIdAndActionTypeOrderByActionDateDesc(
                        visit.getLoan().getId(), 
                        GeneralConfig.ActionType.FIELD_VISIT
                    );
            
            actionOpt.ifPresent(action -> {
                // Update action status based on visit status
                GeneralConfig.ActionStatus newStatus = mapVisitStatusToActionStatus(visit.getStatus());
                action.setActionStatus(newStatus);
                
                // Update outcome based on visit outcome
                if (visit.getOutcome() != null) {
                    GeneralConfig.Outcome newOutcome = mapVisitOutcomeToOutcome(visit.getOutcome());
                    action.setOutcome(newOutcome);
                }
                
                // Update notes with completion info
                String updatedNotes = action.getNotes() + String.format(
                    "\n[%s] Visit status: %s", LocalDate.now(), visit.getStatus()
                );
                
                if (visit.getCompletionNotes() != null && !visit.getCompletionNotes().isEmpty()) {
                    updatedNotes += String.format(". Completion notes: %s", visit.getCompletionNotes());
                }
                
                action.setNotes(updatedNotes);
                action.setCompletedDate(visit.getCompletedDate());
                
                collectionActionRepository.save(action);
                log.info("Updated collection action for field visit: {}", visit.getVisitNumber());
            });
        }
    }

    private void createReminderCollectionAction(FieldVisit visit, User currentUser) {
        if (visit.getLoan() != null) {
            CollectionAction action = CollectionAction.builder()
                    .loan(visit.getLoan())
                    .recoveryCase(visit.getRecoveryCase())
                    .actionType(GeneralConfig.ActionType.EMAIL)
                    .actionStatus(GeneralConfig.ActionStatus.COMPLETED)
                    .actionDate(LocalDate.now())
                    .contactPerson(visit.getLoan().getBorrower() != null ? 
                        visit.getLoan().getBorrower().getFullName() : null)
                    .contactNumber(visit.getLoan().getBorrower() != null ? 
                        visit.getLoan().getBorrower().getPhoneNumber() : null)
                    .contactMethod(GeneralConfig.ContactMethod.EMAIL)
                    .outcome(GeneralConfig.Outcome.PENDING)
                    .notes(String.format("Reminder sent for field visit scheduled on %s. Visit #: %s", 
                            visit.getVisitDate(), visit.getVisitNumber()))
                    .followUpDate(visit.getVisitDate())
                    .assignedTo(visit.getAssignedOfficer())
                    .performedBy(currentUser)
                    .build();
            
            collectionActionRepository.save(action);
            log.info("Created reminder collection action for visit: {}", visit.getVisitNumber());
        }
    }

    private void addCaseNoteForVisit(RecoveryCase recoveryCase, FieldVisit visit, User currentUser) {
        CaseNote caseNote = CaseNote.builder()
                .recoveryCase(recoveryCase)
                .content(String.format("Field visit scheduled - Date: %s, Purpose: %s, Address: %s", 
                        visit.getVisitDate(), visit.getPurpose(), 
                        visit.getVisitAddress() != null ? visit.getVisitAddress() : "Not specified"))
                .type("FIELD_VISIT")
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();
        
        caseNoteRepository.save(caseNote);
        log.info("Added case note for field visit: {}", visit.getVisitNumber());
    }

    private void addStatusChangeNote(RecoveryCase recoveryCase, FieldVisit visit, 
                                     String oldStatus, User currentUser) {
        CaseNote caseNote = CaseNote.builder()
                .recoveryCase(recoveryCase)
                .content(String.format("Field visit %s status changed from %s to %s. Outcome: %s", 
                        visit.getVisitNumber(), oldStatus, visit.getStatus(), 
                        visit.getOutcome() != null ? visit.getOutcome() : "N/A"))
                .type("SYSTEM")
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();
        
        caseNoteRepository.save(caseNote);
        log.info("Added status change note for field visit: {}", visit.getVisitNumber());
    }

    private void addCancellationNote(RecoveryCase recoveryCase, FieldVisit visit, 
                                     String reason, User currentUser) {
        CaseNote caseNote = CaseNote.builder()
                .recoveryCase(recoveryCase)
                .content(String.format("Field visit %s has been cancelled. Reason: %s", 
                        visit.getVisitNumber(), reason))
                .type("SYSTEM")
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();
        
        caseNoteRepository.save(caseNote);
        log.info("Added cancellation note for field visit: {}", visit.getVisitNumber());
    }

    private void addRescheduleNote(RecoveryCase recoveryCase, FieldVisit visit, 
                                   String oldDate, LocalDate newDate, String reason, User currentUser) {
        CaseNote caseNote = CaseNote.builder()
                .recoveryCase(recoveryCase)
                .content(String.format("Field visit %s rescheduled from %s to %s. Reason: %s", 
                        visit.getVisitNumber(), oldDate, newDate, reason != null ? reason : "Not specified"))
                .type("SYSTEM")
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();
        
        caseNoteRepository.save(caseNote);
        log.info("Added reschedule note for field visit: {}", visit.getVisitNumber());
    }

    private void sendBorrowerNotification(FieldVisit visit) {
        // TODO: Implementation for sending SMS/Email notification to borrower
        // This would integrate with your notification service
        log.info("Sending notification to borrower for visit: {}", visit.getVisitNumber());
        
        // For now, just mark as sent
        visit.setNotificationSent(true);
    }

    // ==================== MAPPING METHODS ====================

    private GeneralConfig.ActionStatus mapVisitStatusToActionStatus(String visitStatus) {
        switch (visitStatus) {
            case "SCHEDULED":
                return GeneralConfig.ActionStatus.SCHEDULED;
            case "COMPLETED":
                return GeneralConfig.ActionStatus.COMPLETED;
            case "CANCELLED":
                return GeneralConfig.ActionStatus.CANCELLED;
            case "RESCHEDULED":
                return GeneralConfig.ActionStatus.RESCHEDULED;
            default:
                return GeneralConfig.ActionStatus.PENDING;
        }
    }

    private GeneralConfig.Outcome mapVisitOutcomeToOutcome(String visitOutcome) {
        switch (visitOutcome) {
            case "SUCCESSFUL":
                return GeneralConfig.Outcome.SUCCESSFUL;
            case "UNSUCCESSFUL":
                return GeneralConfig.Outcome.UNSUCCESSFUL;
            case "PARTIAL":
                return GeneralConfig.Outcome.PARTIAL;
            case "POSTPONED":
                return GeneralConfig.Outcome.POSTPONED;
            default:
                return GeneralConfig.Outcome.PENDING;
        }
    }

    // ==================== CONVERSION METHODS ====================

    private FieldVisitDto convertToDto(FieldVisit visit) {
        FieldVisitDto dto = new FieldVisitDto();
        
        dto.setId(visit.getId());
        dto.setVisitNumber(visit.getVisitNumber());
        
        if (visit.getLoan() != null) {
            dto.setLoanId(visit.getLoan().getId());
            dto.setLoanNumber(visit.getLoan().getLoanAccountNumber());
            if (visit.getLoan().getBorrower() != null) {
                dto.setBorrowerName(visit.getLoan().getBorrower().getFullName());
                dto.setBorrowerPhone(visit.getLoan().getBorrower().getPhoneNumber());
                dto.setBorrowerAddress(visit.getLoan().getBorrower().getPhysicalAddress());
            }
        }
        
        if (visit.getRecoveryCase() != null) {
            dto.setRecoveryCaseId(visit.getRecoveryCase().getId());
        }
        
        dto.setVisitDate(visit.getVisitDate());
        dto.setVisitTime(visit.getVisitTime());
        dto.setVisitAddress(visit.getVisitAddress());
        dto.setPurpose(visit.getPurpose());
        dto.setStatus(visit.getStatus());
        
        if (visit.getAssignedOfficer() != null) {
            dto.setAssignedOfficerId(visit.getAssignedOfficer().getId());
            dto.setAssignedOfficerName(visit.getAssignedOfficer().getFullName());
        }
        
        dto.setNotes(visit.getNotes());
        dto.setOutcome(visit.getOutcome());
        dto.setCompletionNotes(visit.getCompletionNotes());
        dto.setCompletedDate(visit.getCompletedDate());
        dto.setNotifyBorrower(visit.getNotifyBorrower());
        dto.setSendReminder(visit.getSendReminder());
        dto.setReminderSent(visit.getReminderSent());
        dto.setNotificationSent(visit.getNotificationSent());
        
        if (visit.getCreatedBy() != null) {
            dto.setCreatedBy(String.valueOf(visit.getCreatedBy()));
        }
        dto.setCreatedDate(visit.getCreatedDate());
        dto.setLastModifiedDate(visit.getLastModifiedDate());
        
        return dto;
    }
}