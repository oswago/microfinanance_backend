package com.microfinance.loanapplications.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.microfinance.audit.service.AuditService;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.RolePermissionRepository;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;

import com.microfinance.borrower.service.BorrowerService;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.BusinessException;
import com.microfinance.exception.ResourceNotFoundException;

import com.microfinance.exception.ValidationException;
import com.microfinance.loanapplications.dto.collection.*;

import com.microfinance.loanapplications.dto.collection.DailyCollectionDto;
import com.microfinance.loanapplications.dto.repayment.OverdueInstallmentDto;
import com.microfinance.loanapplications.entity.*;
import com.microfinance.loanapplications.repository.*;
;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CollectionServiceImpl implements CollectionService {

    private final LoanRepository loanRepository;
    private final CollectionActionRepository collectionActionRepository;
    private final UserService userService;
    private final SecurityUtils securityUtils;
    private final RolePermissionRepository rolePermissionRepository;
    private final PdfGenerationService pdfService;
    private final ReminderScheduleRepository reminderScheduleRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    @Autowired
    private final AuditService auditService;
    private final UserRepository userRepository;
    private  final BorrowerService borrowerService;

    // Add to CollectionServiceImpl.java

    @Transactional(readOnly = true)
    @Override
    public List<OverdueInstallmentDto> getOverdueInstallments(Long branchId, Integer minDaysOverdue,
                                                              Integer maxDaysOverdue, User currentUser) {
        log.debug("Getting overdue installments - branch: {}, minDays: {}, maxDays: {}",
                branchId, minDaysOverdue, maxDaysOverdue);

        Long effectiveBranchId = getEffectiveBranchId(branchId, currentUser);

        // Find overdue loans
        Pageable pageable = PageRequest.of(0, 100);
        Page<Loan> overdueLoans = loanRepository.findOverdueLoansTest(
                effectiveBranchId, null,
                minDaysOverdue != null ? minDaysOverdue : 1,
                maxDaysOverdue, pageable);

        List<OverdueInstallmentDto> result = new ArrayList<>();

        for (Loan loan : overdueLoans.getContent()) {
            // Find overdue repayment schedules
            List<RepaymentSchedule> schedules = repaymentScheduleRepository
                    .findByLoanIdAndDueDateBeforeAndStatusNot(
                            loan.getId(), LocalDate.now(),GeneralConfig.InstallmentStatus.PAID);

            for (RepaymentSchedule schedule : schedules) {
                result.add(OverdueInstallmentDto.builder()
                        .id(schedule.getId())
                        .loanId(loan.getId())
                        .loanAccountNumber(loan.getLoanAccountNumber())
                        .borrowerName(loan.getBorrower() != null ?
                                loan.getBorrower().getFullName() : "Unknown")
                        .phoneNumber(loan.getBorrower() != null ?
                                loan.getBorrower().getPhoneNumber() : null)
                        .amountOverdue(schedule.getOutstandingAmount())
                        .daysOverdue(schedule.getDaysOverdue())
                        .installmentNumber(schedule.getInstallmentNumber())
                        .principalOverdue(schedule.getPrincipalDue())
                        .interestOverdue(schedule.getInterestDue())
                        .penaltyOverdue(schedule.getPenaltyAmount())
                        .build());
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public CollectionActionStatsDto getCollectionActionStats(LocalDate date, Long branchId, User currentUser) {
        log.debug("Getting collection action stats for date: {}, branch: {}", date, branchId);

        Long effectiveBranchId = getEffectiveBranchId(branchId, currentUser);

        // Get overdue installments count
        Pageable pageable = PageRequest.of(0, 1000);
        Page<Loan> overdueLoans = loanRepository.findOverdueLoans(effectiveBranchId, null, 1, null, pageable);

        // Get today's calls count
        Integer todaysCalls = collectionActionRepository.countByPerformedByIdAndActionTypeAndActionDate(
                currentUser.getId(), GeneralConfig.ActionType.PHONE_CALL, date);

        // Get amount collected today
        BigDecimal amountCollectedToday = loanRepository.sumCollectionsByDateRange(
                effectiveBranchId, date, date);

        // Get active agents count
        Integer activeAgents = loanRepository.countActiveLoanOfficers(effectiveBranchId);

        return CollectionActionStatsDto.builder()
                .overdueInstallments(overdueLoans.getNumberOfElements())
                .todaysCalls(todaysCalls != null ? todaysCalls : 0)
                .amountCollectedToday(amountCollectedToday != null ? amountCollectedToday : BigDecimal.ZERO)
                .activeAgents(activeAgents != null ? activeAgents : 0)
                .build();
    }

    @Transactional
    @Override
    public CollectionActionDto logPhoneCall(LogPhoneCallDto phoneCallDto, User currentUser) {
        log.debug("Logging phone call for loan: {}", phoneCallDto.getLoanId());

        // Create a collection action record
        RecordCollectionActionDto actionDto = RecordCollectionActionDto.builder()
                .loanId(phoneCallDto.getLoanId())
                .actionType(GeneralConfig.ActionType.PHONE_CALL)
                .actionDate(LocalDate.now())
                .actionTime(LocalTime.now())
                .outcome(GeneralConfig.Outcome.valueOf(phoneCallDto.getOutcome()))
                .notes(phoneCallDto.getNotes())
                .contactPerson(phoneCallDto.getContactPerson())
                .contactNumber(phoneCallDto.getContactNumber())
                .build();

        return recordCollectionAction(actionDto, currentUser);
    }

    @Transactional
    @Override
    public PenaltyResultDto applyPenalty(ApplyPenaltyDto penaltyDto, User currentUser) {
        log.debug("Applying penalty to loan: {}, amount: {}", penaltyDto.getLoanId(), penaltyDto.getAmount());

        Loan loan = loanRepository.findById(penaltyDto.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        // Update loan penalty
        BigDecimal currentPenalty = loan.getPenaltyAccrued() != null ?
                loan.getPenaltyAccrued() : BigDecimal.ZERO;
        BigDecimal newPenalty = currentPenalty.add(penaltyDto.getAmount());
        loan.setPenaltyAccrued(newPenalty);
        loan.setUpdatedAt(LocalDateTime.now());
        loan.setUpdatedBy(currentUser.getId());
        loanRepository.save(loan);

        // Create a collection action record for the penalty
        RecordCollectionActionDto actionDto = RecordCollectionActionDto.builder()
                .loanId(penaltyDto.getLoanId())
                .actionType(GeneralConfig.ActionType.ESCALATION)
                .actionDate(LocalDate.now())
                .actionTime(LocalTime.now())
                .outcome(GeneralConfig.Outcome.REFUSED_TO_PAY)
                .notes(String.format("Penalty applied: %s - %s",
                        penaltyDto.getReason(), penaltyDto.getNotes()))
                .build();

        recordCollectionAction(actionDto, currentUser);

        return PenaltyResultDto.builder()
                .loanId(loan.getId())
                .loanAccountNumber(loan.getLoanAccountNumber())
                .penaltyApplied(penaltyDto.getAmount())
                .totalPenalty(newPenalty)
                .status("APPLIED")
                .message("Penalty applied successfully")
                .build();
    }

    @Transactional
    @Override
    public EscalationResultDto escalateCase(EscalateCaseDto escalateDto, User currentUser) {
        log.debug("Escalating case for loan: {}", escalateDto.getLoanId());

        Loan loan = loanRepository.findById(escalateDto.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        // Update loan status or add escalation flag
        // This could update a collection_stage field if you have one

        // Create a collection action record for escalation
        RecordCollectionActionDto actionDto = RecordCollectionActionDto.builder()
                .loanId(escalateDto.getLoanId())
                .actionType(GeneralConfig.ActionType.ESCALATION)
                .actionDate(LocalDate.now())
                .actionTime(LocalTime.now())
                .outcome(GeneralConfig.Outcome.REFUSED_TO_PAY)
                .notes(String.format("Case escalated: %s - %s. Escalated to officer: %s",
                        escalateDto.getReason(), escalateDto.getNotes(),
                        escalateDto.getEscalateToOfficerId()))
                .assignedToId(escalateDto.getEscalateToOfficerId())
                .build();

        recordCollectionAction(actionDto, currentUser);

        return EscalationResultDto.builder()
                .loanId(loan.getId())
                .loanAccountNumber(loan.getLoanAccountNumber())
                .escalationLevel("SENIOR_OFFICER")
                .status("ESCALATED")
                .message("Case escalated successfully")
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ActivityDto> getRecentActivities(int limit, Long branchId, User currentUser) {
        log.debug("Getting recent activities, limit: {}, branch: {}", limit, branchId);

        Long effectiveBranchId = getEffectiveBranchId(branchId, currentUser);

        Pageable pageable = PageRequest.of(0, limit);
        List<CollectionAction> recentActions;

        if (effectiveBranchId != null) {
            recentActions = collectionActionRepository
                    .findRecentActionsByBranch(effectiveBranchId, pageable);
        } else {
            recentActions = collectionActionRepository
                    .findAllByOrderByCreatedAtDesc(pageable);
        }

        return recentActions.stream()
                .map(this::convertToActivityDto)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public CollectionStatisticsDto getCollectionStatistics(LocalDate date, Long branchId, User currentUser) {
        log.debug("Getting collection statistics for date: {}, branch: {}", date, branchId);

        // Apply permission filtering
        Long effectiveBranchId = getEffectiveBranchId(branchId, currentUser);

        // Get overdue stats
        Long totalOverdue = loanRepository.countOverdueLoans(effectiveBranchId, date);
        BigDecimal overdueAmount = loanRepository.sumOverdueAmount(effectiveBranchId, date);

        // Get total portfolio for PAR calculation
        BigDecimal totalPortfolio = loanRepository.sumOutstandingBalanceByBranch(effectiveBranchId);
        BigDecimal portfolioAtRisk = totalPortfolio.compareTo(BigDecimal.ZERO) > 0
                ? overdueAmount.multiply(BigDecimal.valueOf(100)).divide(totalPortfolio, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Get active officers count
        Integer activeOfficers = loanRepository.countActiveLoanOfficers(effectiveBranchId);

        // Get collection rate (last 30 days)
        LocalDate thirtyDaysAgo = date.minusDays(30);
        BigDecimal collectedLast30Days = loanRepository.sumCollectionsByDateRange(
                effectiveBranchId, thirtyDaysAgo, date);
        BigDecimal dueLast30Days = loanRepository.sumDueByDateRange(
                effectiveBranchId, thirtyDaysAgo, date);
        BigDecimal collectionRate = dueLast30Days.compareTo(BigDecimal.ZERO) > 0
                ? collectedLast30Days.multiply(BigDecimal.valueOf(100)).divide(dueLast30Days, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Get response rate (contacts made vs attempted)
        BigDecimal responseRate = calculateResponseRate(effectiveBranchId, date);

        // Get risk breakdown
        RiskBreakdownDto riskBreakdown = getRiskBreakdown(effectiveBranchId, date);
        StageBreakdownDto stageBreakdown = getStageBreakdown(effectiveBranchId, date);

        return CollectionStatisticsDto.builder()
                .totalOverdue(totalOverdue != null ? totalOverdue : 0L)
                .overdueAmount(overdueAmount != null ? overdueAmount : BigDecimal.ZERO)
                .portfolioAtRisk(portfolioAtRisk)
                .activeOfficers(activeOfficers != null ? activeOfficers : 0)
                .collectionRate(collectionRate)
                .responseRate(responseRate)
                .riskBreakdown(riskBreakdown)
                .stageBreakdown(stageBreakdown)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpcomingActionDto> getUpcomingCollectionActions(int limit, Long branchId, Long loanOfficerId, User currentUser) {
        log.debug("Getting upcoming collection actions, limit: {}, branch: {}, officer: {}",
                limit, branchId, loanOfficerId);

        Long effectiveBranchId = getEffectiveBranchId(branchId, currentUser);
        Long effectiveOfficerId = getEffectiveOfficerId(loanOfficerId, currentUser);

        List<UpcomingActionDto> actions = new ArrayList<>();

        // Get scheduled follow-up actions from collection_actions table
        Pageable pageable = PageRequest.of(0, limit);

        // Query 1: Get scheduled follow-up actions
        List<CollectionAction> scheduledActions;

        if (effectiveOfficerId != null) {
            // If filtered by specific officer
            scheduledActions = collectionActionRepository
                    .findScheduledActionsForUser(effectiveOfficerId, LocalDate.now(),pageable);
        } else if (effectiveBranchId != null) {
            // If filtered by branch, get actions for all officers in that branch
            // You'll need to add this method to repository
            scheduledActions = collectionActionRepository
                    .findScheduledActionsForBranch(effectiveBranchId, LocalDate.now(),pageable);
        } else {
            // Get all scheduled actions
            scheduledActions = collectionActionRepository
                    .findScheduledActions(LocalDate.now(),pageable);
        }

        // Convert to DTOs
        for (CollectionAction action : scheduledActions) {
            actions.add(convertToUpcomingActionDto(action));
        }

        // If we have fewer than limit, supplement with overdue loans that need follow-up
        if (actions.size() < limit) {
            int remainingNeeded = limit - actions.size();
            List<UpcomingActionDto> suggestedActions = getSuggestedFollowUpActions(
                    effectiveBranchId, effectiveOfficerId, remainingNeeded);
            actions.addAll(suggestedActions);
        }

        return actions;
    }

    /**
     * Get suggested follow-up actions for overdue loans that don't have scheduled actions
     */
    private List<UpcomingActionDto> getSuggestedFollowUpActions(Long branchId, Long officerId, int limit) {
        if (limit <= 0) return Collections.emptyList();

        Pageable pageable = PageRequest.of(0, limit);

        // Find overdue loans that don't have recent collection actions
        LocalDate oneWeekAgo = LocalDate.now().minusWeeks(1);
        List<Loan> overdueLoansNeedingFollowUp = loanRepository
                .findOverdueLoansWithoutRecentActions(branchId, officerId, oneWeekAgo, pageable);

        List<UpcomingActionDto> suggestions = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Loan loan : overdueLoansNeedingFollowUp) {
            UpcomingActionDto suggestion = UpcomingActionDto.builder()
                    .id(-loan.getId()) // Negative ID to indicate it's a suggestion, not a scheduled action
                    .loanId(loan.getId())
                    .loanAccountNumber(loan.getLoanAccountNumber())
                    .borrowerName(loan.getBorrower() != null ? loan.getBorrower().getFullName() : "Unknown")
                    .actionType("FOLLOW_UP")
                    .title("Suggested follow-up for overdue loan")
                    .description("Loan is " + loan.getDaysDelinquent() + " days overdue. Last action: " +
                            getLastActionDateDescription(loan))
                    .scheduledTime(now.plusDays(1).withHour(10).withMinute(0))
                    .priority(getPriorityFromDaysOverdue(loan.getDaysDelinquent()))
                    .status("SUGGESTED")
                    .assignedToId(loan.getLoanOfficer() != null ? loan.getLoanOfficer().getId() : null)
                    .assignedToName(loan.getLoanOfficer() != null ?
                            loan.getLoanOfficer().getFirstName() + " " +
                                    (loan.getLoanOfficer().getLastName() != null ? loan.getLoanOfficer().getLastName() : "") : null)
                    .build();

            suggestions.add(suggestion);
        }

        return suggestions;
    }

    /**
     * Get description of last action date
     */
    private String getLastActionDateDescription(Loan loan) {
        Optional<CollectionAction> lastAction = collectionActionRepository
                .findFirstByLoanIdOrderByActionDateDesc(loan.getId());

        if (lastAction.isPresent()) {
            LocalDate lastActionDate = lastAction.get().getActionDate();
            long daysAgo = ChronoUnit.DAYS.between(lastActionDate, LocalDate.now());

            if (daysAgo == 0) {
                return "Today";
            } else if (daysAgo == 1) {
                return "Yesterday";
            } else {
                return daysAgo + " days ago";
            }
        } else {
            return "Never";
        }
    }

    /**
     * Convert CollectionAction to UpcomingActionDto
     */
    private UpcomingActionDto convertToUpcomingActionDto(CollectionAction action) {
        String priority = "MEDIUM";
        if (action.getLoan() != null && action.getLoan().getDaysDelinquent() != null) {
            priority = getPriorityFromDaysOverdue(action.getLoan().getDaysDelinquent());
        }

        String description = generateActionDescription(action);

        return UpcomingActionDto.builder()
                .id(action.getId())
                .loanId(action.getLoan().getId())
                .loanAccountNumber(action.getLoan().getLoanAccountNumber())
                .borrowerName(action.getLoan().getBorrower() != null ?
                        action.getLoan().getBorrower().getFullName() : "Unknown")
                .actionType(action.getActionType() != null ? action.getActionType().name() : "FOLLOW_UP")
                .title(generateActionTitle(action))
                .description(description)
                .scheduledTime(LocalDateTime.of(action.getActionDate(),
                        action.getActionTime() != null ? action.getActionTime() : LocalTime.of(9, 0)))
                .priority(priority)
                .status(action.getActionStatus() != null ? action.getActionStatus().name() : "SCHEDULED")
                .assignedToId(action.getAssignedTo() != null ? action.getAssignedTo().getId() : null)
                .assignedToName(action.getAssignedTo() != null ?
                        action.getAssignedTo().getFirstName() + " " +
                                (action.getAssignedTo().getLastName() != null ? action.getAssignedTo().getLastName() : "") : null)
                .build();
    }

    /**
     * Generate a title for the action
     */
    private String generateActionTitle(CollectionAction action) {
        if (action.getActionType() == GeneralConfig.ActionType.FOLLOW_UP) {
            if (action.getPromiseAmount() != null) {
                return "Follow-up on payment promise of " +
                        formatCurrency(action.getPromiseAmount());
            } else {
                return "Follow-up call for " + action.getLoan().getLoanAccountNumber();
            }
        } else if (action.getActionType() == GeneralConfig.ActionType.FIELD_VISIT) {
            return "Field visit to " + action.getLoan().getBorrower().getFullName();
        } else if (action.getActionType() == GeneralConfig.ActionType.PHONE_CALL) {
            return "Call " + action.getLoan().getBorrower().getFullName();
        } else if (action.getActionType() == GeneralConfig.ActionType.MEETING) {
            return "Meeting with " + action.getLoan().getBorrower().getFullName();
        } else {
            return action.getActionType() + " for loan " + action.getLoan().getLoanAccountNumber();
        }
    }

    /**
     * Generate description for the action
     */
    private String generateActionDescription(CollectionAction action) {
        StringBuilder desc = new StringBuilder();

        if (action.getLoan() != null && action.getLoan().getDaysDelinquent() != null) {
            desc.append(action.getLoan().getDaysDelinquent()).append(" days overdue. ");
        }

        if (action.getNotes() != null && !action.getNotes().isEmpty()) {
            desc.append(action.getNotes());
        } else if (action.getOutcome() != null) {
            desc.append("Previous outcome: ").append(action.getOutcome().name());
        }

        if (action.getPromiseAmount() != null && action.getPromiseDate() != null) {
            desc.append(" Promised to pay ").append(formatCurrency(action.getPromiseAmount()))
                    .append(" by ").append(action.getPromiseDate());
        }

        return desc.toString();
    }

    /**
     * Format currency for display
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0";
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toString();
    }


    @Override
    @Transactional
    public CollectionActionDto recordCollectionAction(RecordCollectionActionDto actionDto, User currentUser) {
        log.debug("Recording collection action for loan: {}, type: {}",
                actionDto.getLoanId(), actionDto.getActionType());

        // Find the loan
        Loan loan = loanRepository.findById(actionDto.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with ID: " + actionDto.getLoanId()));

        //Validate before recording
        validateCollectionAction(actionDto,loan);

        // Find assigned user if provided
        User assignedTo = null;
        if (actionDto.getAssignedToId() != null) {
            assignedTo = userService.getUserById(actionDto.getAssignedToId());
        }

        // Create new CollectionAction entity
        CollectionAction action = CollectionAction.builder()
                .loan(loan)
                .actionType(actionDto.getActionType())
                .actionStatus(GeneralConfig.ActionStatus.COMPLETED) // Default to completed for recorded actions
                .actionDate(actionDto.getActionDate() != null ?
                        actionDto.getActionDate() : LocalDate.now())
                .actionTime(actionDto.getActionTime() != null ?
                        actionDto.getActionTime() : LocalTime.now())

                // Contact Information
                .contactPerson(actionDto.getContactPerson())
                .contactNumber(actionDto.getContactNumber())
                .contactMethod(actionDto.getContactMethod())

                // Outcome Details
                .outcome(actionDto.getOutcome())
                .notes(actionDto.getNotes())
                .followUpDate(actionDto.getFollowUpDate())
                .followUpTime(actionDto.getFollowUpTime())
                .followUpAction(actionDto.getFollowUpAction())

                // Promise to Pay
                .promiseAmount(actionDto.getPromiseAmount())
                .promiseDate(actionDto.getPromiseDate())
                .paymentConfirmed(false) // Initially not confirmed

                // Assignment
                .assignedTo(assignedTo)
                .performedBy(currentUser)

                // Location (for field visits)
                .visitLatitude(actionDto.getVisitLatitude())
                .visitLongitude(actionDto.getVisitLongitude())
                .visitAddress(actionDto.getVisitAddress())

                // System fields (BaseEntity handles createdAt/updatedAt)
                .build();

        // Handle file attachments if present
        if (actionDto.getAttachment() != null && !actionDto.getAttachment().isEmpty()) {
            String attachmentUrl = saveAttachmentFile(actionDto.getAttachment(),
                    loan.getLoanAccountNumber(), "attachments");
            action.setAttachmentUrl(attachmentUrl);
        }

        if (actionDto.getRecording() != null && !actionDto.getRecording().isEmpty()) {
            String recordingUrl = saveAttachmentFile(actionDto.getRecording(),
                    loan.getLoanAccountNumber(), "recordings");
            action.setRecordingUrl(recordingUrl);
        }

        // Save to database
        CollectionAction savedAction = collectionActionRepository.save(action);

        // Update loan last contact date if needed
        if (loan.getLastContactDate() == null ||
                savedAction.getActionDate().isAfter(loan.getLastContactDate())) {
            loan.setLastContactDate(savedAction.getActionDate());
            loanRepository.save(loan);
        }

        // If this is a promise to pay, you might want to create a follow-up action
        if (actionDto.getOutcome() == GeneralConfig.Outcome.PROMISED_TO_PAY &&
                actionDto.getFollowUpDate() != null) {
            createFollowUpAction(loan, savedAction, currentUser);
        }

        //Audit Section
        Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName ="";
        Long createdById=null;
        if(currentUser1.isPresent()){
            createdByName=currentUser1.get().getFullName();
            createdById=currentUser1.get().getId();
        }
        if (Objects.nonNull(savedAction.getId())) {
            auditService.masterAuditLogs(
                    savedAction.getLoan().getBorrower().getId(),
                    GeneralConfig.BorrowerActivityType.COLLECTION_ACTIVITY,
                    "COLLECTION",
                    "Collection Action :"+savedAction.getActionType() +" Loan No:"+savedAction.getLoan().getLoanAccountNumber()+ " for borrower with ID: "+savedAction.getLoan().getBorrower().getId()+ "has been Updated by:"+createdByName+"-"+createdById
            );
        }
        //End Audit Section
        log.info("Collection action recorded successfully with ID: {}", savedAction.getId());

        return convertToCollectionActionDto(savedAction);
    }

    /**
     * Helper method to save attachment files
     */
    private String saveAttachmentFile(MultipartFile file, String loanNumber, String folder) {
        try {
            // Create directory if it doesn't exist
            String uploadDir = "uploads/collections/" + folder + "/" + loanNumber;
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String fileName = System.currentTimeMillis() + "_" +
                    file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.-]", "_");

            // Save file
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return relative path for database storage
            return "/" + uploadDir + "/" + fileName;

        } catch (IOException e) {
            log.error("Failed to save attachment file: {}", e.getMessage());
            throw new BusinessException("Failed to save attachment: " + e.getMessage());
        }
    }

    /**
     * Create a follow-up action based on promise to pay
     */
    private void createFollowUpAction(Loan loan, CollectionAction originalAction, User currentUser) {
        CollectionAction followUp = CollectionAction.builder()
                .loan(loan)
                .actionType(GeneralConfig.ActionType.FOLLOW_UP)
                .actionStatus(GeneralConfig.ActionStatus.SCHEDULED)
                .actionDate(originalAction.getFollowUpDate())
                .actionTime(originalAction.getFollowUpTime() != null ?
                        originalAction.getFollowUpTime() : LocalTime.of(9, 0)) // Default 9 AM
                .contactPerson(originalAction.getContactPerson())
                .contactNumber(originalAction.getContactNumber())
                .notes("Follow-up on promise to pay from " +
                        originalAction.getActionDate() +
                        ". Amount promised: " + originalAction.getPromiseAmount())
                .assignedTo(originalAction.getAssignedTo() != null ?
                        originalAction.getAssignedTo() : originalAction.getPerformedBy())
                .performedBy(currentUser)
                .build();

        collectionActionRepository.save(followUp);
        log.debug("Created follow-up action for loan: {}", loan.getLoanAccountNumber());
    }


    private void validateCollectionAction(RecordCollectionActionDto actionDto, Loan loan) {
        List<String> errors = new ArrayList<>();

        if (actionDto.getActionType() == null) {
            errors.add("Action type is required");
        }

        if (actionDto.getOutcome() == null) {
            errors.add("Outcome is required");
        }

        // Validate promise to pay fields
        if (actionDto.getOutcome() == GeneralConfig.Outcome.PROMISED_TO_PAY) {
            if (actionDto.getPromiseAmount() == null ||
                    actionDto.getPromiseAmount().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Promise amount is required and must be greater than 0");
            }
            if (actionDto.getPromiseDate() == null) {
                errors.add("Promise date is required when promising to pay");
            }
            if (actionDto.getPromiseDate() != null &&
                    actionDto.getPromiseDate().isBefore(LocalDate.now())) {
                errors.add("Promise date cannot be in the past");
            }
        }

        // Validate follow-up
        if (actionDto.getFollowUpDate() != null &&
                actionDto.getFollowUpDate().isBefore(LocalDate.now())) {
            errors.add("Follow-up date cannot be in the past");
        }

        // Validate contact information for certain action types
        if (actionDto.getActionType() == GeneralConfig.ActionType.PHONE_CALL ||
                actionDto.getActionType() == GeneralConfig.ActionType.SMS) {
            if (actionDto.getContactNumber() == null ||
                    actionDto.getContactNumber().trim().isEmpty()) {
                errors.add("Contact number is required for phone/SMS actions");
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed: " + String.join(", ", errors));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollectionActionDto> getCollectionActions(Long loanId) {
        log.debug("Getting collection actions for loan: {}", loanId);

        List<CollectionAction> actions = collectionActionRepository
                .findByLoanIdOrderByActionDateDesc(loanId);

        return actions.stream()
                .map(this::convertToCollectionActionDto)
                .collect(Collectors.toList());
    }

    private CollectionActionDto convertToCollectionActionDto(CollectionAction action) {
        return CollectionActionDto.builder()
                .id(action.getId())
                .loanId(action.getLoan().getId())
                .loanAccountNumber(action.getLoan().getLoanAccountNumber())
                .borrowerName(action.getLoan().getBorrower() != null ?
                        action.getLoan().getBorrower().getFullName() : null)
                .actionType(action.getActionType().name())
                .actionStatus(action.getActionStatus().name())
                .actionDate(action.getActionDate())
                .actionTime(action.getActionTime())
                .contactPerson(action.getContactPerson())
                .contactNumber(action.getContactNumber())
                .contactMethod(action.getContactMethod() != null ?
                        action.getContactMethod().name() : null)
                .outcome(action.getOutcome() != null ? action.getOutcome().name() : null)
                .notes(action.getNotes())
                .followUpDate(action.getFollowUpDate())
                .followUpAction(action.getFollowUpAction() != null ?
                        action.getFollowUpAction().name() : null)
                .promiseAmount(action.getPromiseAmount())
                .promiseDate(action.getPromiseDate())
                .paymentConfirmed(action.getPaymentConfirmed())
                .assignedToId(action.getAssignedTo() != null ?
                        action.getAssignedTo().getId() : null)
                .assignedToName(action.getAssignedTo() != null ?
                        action.getAssignedTo().getFirstName() + " " +
                                action.getAssignedTo().getLastName() : null)
                .performedById(action.getPerformedBy().getId())
                .performedByName(action.getPerformedBy().getFirstName() + " " +
                        action.getPerformedBy().getLastName())
                .visitLatitude(action.getVisitLatitude())
                .visitLongitude(action.getVisitLongitude())
                .visitAddress(action.getVisitAddress())
                .attachmentUrl(action.getAttachmentUrl())
                .recordingUrl(action.getRecordingUrl())
                .createdAt(action.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public BulkReminderResultDto sendBulkReminders(BulkReminderRequestDto request, User currentUser) {
        log.debug("Sending bulk reminders, type: {}, count: {}",
                request.getReminderType(),
                request.getSendToAllOverdue() ? "ALL" : request.getLoanIds().size());

        // Determine which loans to send to
        List<Long> loanIds = request.getLoanIds();
        if (request.getSendToAllOverdue()) {
            Pageable pageable = PageRequest.of(0, 1000);
            Page<Loan> overdueLoans = loanRepository.findOverdueLoans(
                    request.getBranchId(), null,
                    request.getMinDaysOverdue(), request.getMaxDaysOverdue(),
                    pageable);
            loanIds = overdueLoans.getContent().stream()
                    .map(Loan::getId)
                    .collect(Collectors.toList());
        }

        // Process each loan (mock implementation)
        List<ReminderResultDto> results = new ArrayList<>();

        // Use atomic counters or process without lambda
        for (Long loanId : loanIds) {
            Optional<Loan> loanOpt = loanRepository.findById(loanId);
            if (loanOpt.isPresent()) {
                Loan loan = loanOpt.get();
                boolean success = Math.random() > 0.1; // 90% success rate for mock

                ReminderResultDto result = ReminderResultDto.builder()
                        .loanId(loanId)
                        .loanAccountNumber(loan.getLoanAccountNumber())
                        .borrowerName(loan.getBorrower() != null ? loan.getBorrower().getFullName() : "Unknown")
                        .borrowerPhone(loan.getBorrower() != null ? loan.getBorrower().getPhoneNumber() : null)
                        .borrowerEmail(loan.getBorrower() != null ? loan.getBorrower().getEmail() : null)
                        .success(success)
                        .errorMessage(success ? null : "Failed to send reminder")
                        .build();
                results.add(result);
            }
        }

        // Calculate totals after processing
        int successful = (int) results.stream().filter(ReminderResultDto::getSuccess).count();
        int failed = results.size() - successful;

        return BulkReminderResultDto.builder()
                .totalSent(results.size())
                .successful(successful)
                .failed(failed)
                .results(results)
                .build();
    }

    @Override
    @Transactional
    public TaskAssignmentResultDto assignCollectionTasks(TaskAssignmentRequestDto request, User currentUser) {
        log.debug("Assigning collection tasks to user: {}, count: {}",
                request.getAssignToUserId(), request.getLoanIds().size());

        List<AssignmentResultDto> results = new ArrayList<>();
        int successful = 0;
        int failed = 0;

        for (Long loanId : request.getLoanIds()) {
            Optional<Loan> loanOpt = loanRepository.findById(loanId);
            if (loanOpt.isPresent()) {
                // In a real implementation, you would create tasks in a Tasks table
                AssignmentResultDto result = AssignmentResultDto.builder()
                        .loanId(loanId)
                        .loanAccountNumber(loanOpt.get().getLoanAccountNumber())
                        .borrowerName(loanOpt.get().getBorrower() != null ?
                                loanOpt.get().getBorrower().getFullName() : "Unknown")
                        .success(true)
                        .build();
                results.add(result);
                successful++;
            } else {
                AssignmentResultDto result = AssignmentResultDto.builder()
                        .loanId(loanId)
                        .success(false)
                        .errorMessage("Loan not found")
                        .build();
                results.add(result);
                failed++;
            }
        }

        return TaskAssignmentResultDto.builder()
                .totalAssigned(results.size())
                .successful(successful)
                .failed(failed)
                .results(results)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CollectionReportDto generateCollectionReport(LocalDate startDate, LocalDate endDate,
                                                        Long branchId, Long loanOfficerId,
                                                        User currentUser) {
        log.debug("Generating collection report from {} to {}", startDate, endDate);

        Long effectiveBranchId = getEffectiveBranchId(branchId, currentUser);
        Long effectiveOfficerId = getEffectiveOfficerId(loanOfficerId, currentUser);

        // Get overdue stats
        Long totalOverdue = loanRepository.countOverdueLoans(effectiveBranchId, endDate);
        BigDecimal totalOverdueAmount = loanRepository.sumOverdueAmount(effectiveBranchId, endDate);

        // Get collection stats
        BigDecimal totalCollectedAmount = loanRepository.sumCollectionsByDateRange(
                effectiveBranchId, startDate, endDate);
        Integer totalCollected = loanRepository.countCollectionsByDateRange(
                effectiveBranchId, startDate, endDate);

        // Calculate collection rate
        BigDecimal totalDue = loanRepository.sumDueByDateRange(effectiveBranchId, startDate, endDate);
        BigDecimal collectionRate = totalDue.compareTo(BigDecimal.ZERO) > 0
                ? totalCollectedAmount.multiply(BigDecimal.valueOf(100)).divide(totalDue, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Generate mock daily collections
        List<DailyCollectionDto> dailyCollections = generateMockDailyCollections(startDate, endDate);

        // Generate mock officer performance
        List<OfficerPerformanceDto> officerPerformance = generateMockOfficerPerformance(effectiveBranchId);

        // Generate aging breakdown
        List<AgingBreakdownDto> agingBreakdown = generateMockAgingBreakdown(effectiveBranchId, endDate);

        return CollectionReportDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .branchId(effectiveBranchId)
                .branchName(effectiveBranchId != null ? "Branch " + effectiveBranchId : "All Branches")
                .loanOfficerId(effectiveOfficerId)
                .loanOfficerName(effectiveOfficerId != null ? "Officer " + effectiveOfficerId : "All Officers")
                .totalOverdue(totalOverdue != null ? totalOverdue.intValue() : 0)
                .totalOverdueAmount(totalOverdueAmount != null ? totalOverdueAmount : BigDecimal.ZERO)
                .totalCollected(totalCollected != null ? totalCollected : 0)
                .totalCollectedAmount(totalCollectedAmount != null ? totalCollectedAmount : BigDecimal.ZERO)
                .collectionRate(collectionRate)
                .dailyCollections(dailyCollections)
                .officerPerformance(officerPerformance)
                .agingBreakdown(agingBreakdown)
                .totalCalls(45) // Mock data
                .totalVisits(23) // Mock data
                .totalFollowUps(67) // Mock data
                .promisesToPay(34) // Mock data
                .build();
    }

// ==================== HELPER METHODS ====================

    private Long getEffectiveBranchId(Long requestedBranchId, User currentUser) {
        if (currentUser.getRole() == User.UserRole.SUPER_ADMIN ||
                hasCurrentUserPermission("LOAN_VIEW_ALL")) {
            return requestedBranchId;
        } else if (hasCurrentUserPermission("LOAN_VIEW_BRANCH")) {
            // If user can only view their branch, ignore requested branch and use user's branch
            return currentUser.getBranchId();
        }
        return null;
    }

    private Long getEffectiveOfficerId(Long requestedOfficerId, User currentUser) {
        if (currentUser.getRole() == User.UserRole.SUPER_ADMIN ||
                hasCurrentUserPermission("LOAN_VIEW_ALL")) {
            return requestedOfficerId;
        } else if (hasCurrentUserPermission("LOAN_VIEW_OWN")) {
            // If user can only view their own loans, use their ID
            return currentUser.getId();
        }
        return null;
    }


    private String getPriorityFromDaysOverdue(Integer daysOverdue) {
        if (daysOverdue == null) return "LOW";
        if (daysOverdue <= 7) return "LOW";
        if (daysOverdue <= 30) return "MEDIUM";
        if (daysOverdue <= 90) return "HIGH";
        return "CRITICAL";
    }

    private BigDecimal calculateResponseRate(Long branchId, LocalDate date) {
        // Mock implementation - in real app, calculate from action logs
        return BigDecimal.valueOf(78.5);
    }

    private RiskBreakdownDto getRiskBreakdown(Long branchId, LocalDate date) {
        // Mock implementation - in real app, query from database
        return RiskBreakdownDto.builder()
                .low(15L)
                .medium(23L)
                .high(8L)
                .critical(4L)
                .lowAmount(BigDecimal.valueOf(15000))
                .mediumAmount(BigDecimal.valueOf(45000))
                .highAmount(BigDecimal.valueOf(32000))
                .criticalAmount(BigDecimal.valueOf(28000))
                .build();
    }

    private StageBreakdownDto getStageBreakdown(Long branchId, LocalDate date) {
        // Mock implementation - in real app, query from database
        return StageBreakdownDto.builder()
                .new_(28L)
                .followUp(12L)
                .escalated(7L)
                .legal(3L)
                .newAmount(BigDecimal.valueOf(42000))
                .followUpAmount(BigDecimal.valueOf(31000))
                .escalatedAmount(BigDecimal.valueOf(25000))
                .legalAmount(BigDecimal.valueOf(18000))
                .build();
    }

    private List<DailyCollectionDto> generateMockDailyCollections(LocalDate startDate, LocalDate endDate) {
        List<DailyCollectionDto> collections = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            collections.add(DailyCollectionDto.builder()
                    .date(current)
                    .loansCollected((int) (Math.random() * 10) + 1)
                    .amountCollected(BigDecimal.valueOf(Math.random() * 100000))
                    .build());
            current = current.plusDays(1);
        }
        return collections;
    }

    private List<OfficerPerformanceDto> generateMockOfficerPerformance(Long branchId) {
        List<OfficerPerformanceDto> performances = new ArrayList<>();
        String[] officers = {"John Smith", "Jane Doe", "Bob Johnson", "Alice Brown"};

        for (int i = 0; i < officers.length; i++) {
            performances.add(OfficerPerformanceDto.builder()
                    .officerId((long) (i + 1))
                    .officerName(officers[i])
                    .assignedLoans((int) (Math.random() * 30) + 10)
                    .resolvedLoans((int) (Math.random() * 20) + 5)
                    .resolvedAmount(BigDecimal.valueOf(Math.random() * 50000))
                    .callsMade((int) (Math.random() * 50) + 20)
                    .visitsMade((int) (Math.random() * 15) + 5)
                    .collectionRate(BigDecimal.valueOf(Math.random() * 100))
                    .build());
        }
        return performances;
    }

    private List<AgingBreakdownDto> generateMockAgingBreakdown(Long branchId, LocalDate asOfDate) {
        List<AgingBreakdownDto> aging = new ArrayList<>();

        aging.add(AgingBreakdownDto.builder()
                .bucket("1-7 days")
                .count(18)
                .amount(BigDecimal.valueOf(45000))
                .percentage(BigDecimal.valueOf(28.5))
                .build());

        aging.add(AgingBreakdownDto.builder()
                .bucket("8-30 days")
                .count(23)
                .amount(BigDecimal.valueOf(89000))
                .percentage(BigDecimal.valueOf(35.2))
                .build());

        aging.add(AgingBreakdownDto.builder()
                .bucket("31-90 days")
                .count(12)
                .amount(BigDecimal.valueOf(67000))
                .percentage(BigDecimal.valueOf(22.1))
                .build());

        aging.add(AgingBreakdownDto.builder()
                .bucket("90+ days")
                .count(7)
                .amount(BigDecimal.valueOf(42000))
                .percentage(BigDecimal.valueOf(14.2))
                .build());

        return aging;
    }



    /**
     * Check if current user has a specific permission
     */
    private boolean hasCurrentUserPermission(String permission) {
        if (permission == null) {
            return false;
        }

        try {
            User currentUser = getCurrentUser();

            if (currentUser == null) {
                log.warn("No current user found");
                return false;
            }
            User.UserRole userRole = currentUser.getRole();

            if (userRole == null) {
                log.warn("User {} has no role assigned", currentUser.getUsername());
                return false;
            }
            if(userRole == User.UserRole.SUPER_ADMIN){
                return true;
            }

            boolean hasPermission = rolePermissionRepository.existsByRoleAndPermission(userRole, permission);

            log.debug("Current user {} with role {} has permission {}: {}",
                    currentUser.getUsername(), userRole, permission, hasPermission);

            return hasPermission;

        } catch (Exception e) {
            log.error("Error checking permission {}: {}", permission, e.getMessage());
            return false;
        }
    }



    private User getCurrentUser() {
        Long currentUserId = securityUtils.getCurrentUserId();
        return userService.getUserById(currentUserId);
    }

    private boolean isSuperAdmin(User user) {
        return user != null && user.getRole() == User.UserRole.SUPER_ADMIN;
    }

/// pdf report///

@Transactional(readOnly = true)
@Override
public byte[] exportCollectionReport(CollectionReportDto report, String format) {
            log.debug("Exporting collection report in format: {}", format);

            if (format.equalsIgnoreCase("PDF")) {
                return generatePdfReport(report);
            } else {
                throw new BusinessException("Unsupported format: " + format);
            }
        }


    private byte[] generatePdfReport(CollectionReportDto report) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Use shared methods from PdfGenerationService
            pdfService.addDocumentHeader(document, "Collection Report", "Performance Summary");

            // Add period information
            addPeriodInfo(document, report);

            // Add summary statistics using shared methods
            pdfService.addSectionTitle(document, "Summary Statistics");
            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            pdfService.addTableHeader(summaryTable, "Total Overdue");
            pdfService.addTableHeader(summaryTable, "Overdue Amount");
            pdfService.addTableHeader(summaryTable, "Total Collected");
            pdfService.addTableHeader(summaryTable, "Collection Rate");

            pdfService.addTableCell(summaryTable, String.valueOf(report.getTotalOverdue()));
            pdfService.addAmountCell(summaryTable, report.getTotalOverdueAmount());
            pdfService.addAmountCell(summaryTable, report.getTotalCollectedAmount());
            pdfService.addPercentageCell(summaryTable, Double.parseDouble(String.valueOf(report.getCollectionRate())));
            document.add(summaryTable);
            document.add(new Paragraph(" "));

            // Add officer performance table
            pdfService.addSectionTitle(document, "Officer Performance");
            PdfPTable officerTable = new PdfPTable(7);
            officerTable.setWidthPercentage(100);
            pdfService.addTableHeader(officerTable, "Officer");
            pdfService.addTableHeader(officerTable, "Assigned");
            pdfService.addTableHeader(officerTable, "Resolved");
            pdfService.addTableHeader(officerTable, "Amount");
            pdfService.addTableHeader(officerTable, "Calls");
            pdfService.addTableHeader(officerTable, "Visits");
            pdfService.addTableHeader(officerTable, "Rate");

            for (OfficerPerformanceDto officer : report.getOfficerPerformance()) {
                pdfService.addTableCell(officerTable, officer.getOfficerName());
                pdfService.addTableCell(officerTable, String.valueOf(officer.getAssignedLoans()));
                pdfService.addTableCell(officerTable, String.valueOf(officer.getResolvedLoans()));
                pdfService.addAmountCell(officerTable, officer.getResolvedAmount());
                pdfService.addTableCell(officerTable, String.valueOf(officer.getCallsMade()));
                pdfService.addTableCell(officerTable, String.valueOf(officer.getVisitsMade()));
                pdfService.addPercentageCell(officerTable, Double.parseDouble(String.valueOf(officer.getCollectionRate())));
            }
            document.add(officerTable);
            document.add(new Paragraph(" "));

            // Add aging breakdown
            pdfService.addSectionTitle(document, "Aging Breakdown");
            PdfPTable agingTable = new PdfPTable(4);
            agingTable.setWidthPercentage(100);
            pdfService.addTableHeader(agingTable, "Bucket");
            pdfService.addTableHeader(agingTable, "Count");
            pdfService.addTableHeader(agingTable, "Amount");
            pdfService.addTableHeader(agingTable, "Percentage");

            for (AgingBreakdownDto aging : report.getAgingBreakdown()) {
                pdfService.addTableCell(agingTable, aging.getBucket());
                pdfService.addTableCell(agingTable, String.valueOf(aging.getCount()));
                pdfService.addAmountCell(agingTable, aging.getAmount());
                pdfService.addPercentageCell(agingTable, Double.parseDouble(String.valueOf(aging.getPercentage())));
            }
            document.add(agingTable);
            document.add(new Paragraph(" "));

            // Add activity summary
            pdfService.addSectionTitle(document, "Activity Summary");
            PdfPTable activityTable = new PdfPTable(4);
            activityTable.setWidthPercentage(100);
            pdfService.addTableHeader(activityTable, "Total Calls");
            pdfService.addTableHeader(activityTable, "Total Visits");
            pdfService.addTableHeader(activityTable, "Follow-ups");
            pdfService.addTableHeader(activityTable, "Promises to Pay");

            pdfService.addTableCell(activityTable, String.valueOf(report.getTotalCalls()));
            pdfService.addTableCell(activityTable, String.valueOf(report.getTotalVisits()));
            pdfService.addTableCell(activityTable, String.valueOf(report.getTotalFollowUps()));
            pdfService.addTableCell(activityTable, String.valueOf(report.getPromisesToPay()));
            document.add(activityTable);
            document.add(new Paragraph(" "));

            // Use shared footer
            pdfService.addDocumentFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (DocumentException | IOException e) {
            log.error("Error generating PDF report", e);
            throw new BusinessException("Failed to generate PDF report: " + e.getMessage());
        }
    }

    private void addPeriodInfo(Document document, CollectionReportDto report) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        pdfService.addTableRow(infoTable, "Period:", report.getStartDate() + " to " + report.getEndDate());
        pdfService.addTableRow(infoTable, "Branch:", report.getBranchName() != null ? report.getBranchName() : "All Branches");
        pdfService.addTableRow(infoTable, "Officer:", report.getLoanOfficerName() != null ? report.getLoanOfficerName() : "All Officers");
        document.add(infoTable);
        document.add(new Paragraph(" "));
    }


        private void addSummaryStatistics(Document document, CollectionReportDto report) throws DocumentException {
            pdfService.addSectionTitle(document, "Summary Statistics");

            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{25f, 25f, 25f, 25f});

            // Headers
            pdfService.addTableHeader(summaryTable, "Total Overdue");
            pdfService.addTableHeader(summaryTable, "Overdue Amount");
            pdfService.addTableHeader(summaryTable, "Total Collected");
            pdfService.addTableHeader(summaryTable, "Collection Rate");

            // Data
            pdfService.addTableCell(summaryTable, String.valueOf(report.getTotalOverdue()));
            pdfService.addAmountCell(summaryTable, report.getTotalOverdueAmount());
            pdfService.addAmountCell(summaryTable, report.getTotalCollectedAmount());
            pdfService.addPercentageCell(summaryTable, Double.parseDouble(String.valueOf(report.getCollectionRate())));

            document.add(summaryTable);
            document.add(new Paragraph(" "));
        }

        private void addOfficerPerformanceTable(Document document, CollectionReportDto report) throws DocumentException {
            List<OfficerPerformanceDto> officers = report.getOfficerPerformance();
            if (officers == null || officers.isEmpty()) {
                return;
            }

            pdfService.addSectionTitle(document, "Officer Performance");

            PdfPTable officerTable = new PdfPTable(7);
            officerTable.setWidthPercentage(100);
            officerTable.setWidths(new float[]{20f, 12f, 12f, 15f, 10f, 10f, 15f});

            // Headers
            pdfService.addTableHeader(officerTable, "Officer");
            pdfService.addTableHeader(officerTable, "Assigned");
            pdfService.addTableHeader(officerTable, "Resolved");
            pdfService.addTableHeader(officerTable, "Amount");
            pdfService.addTableHeader(officerTable, "Calls");
            pdfService.addTableHeader(officerTable, "Visits");
            pdfService.addTableHeader(officerTable, "Rate");

            // Data rows
            for (OfficerPerformanceDto officer : officers) {
                pdfService.addTableCell(officerTable, officer.getOfficerName());
                pdfService.addTableCell(officerTable, String.valueOf(officer.getAssignedLoans()));
                pdfService.addTableCell(officerTable, String.valueOf(officer.getResolvedLoans()));
                pdfService.addAmountCell(officerTable, officer.getResolvedAmount());
                pdfService.addTableCell(officerTable, String.valueOf(officer.getCallsMade()));
                pdfService.addTableCell(officerTable, String.valueOf(officer.getVisitsMade()));
                pdfService.addPercentageCell(officerTable, Double.parseDouble(String.valueOf(officer.getCollectionRate())));
            }

            document.add(officerTable);
            document.add(new Paragraph(" "));
        }

        private void addAgingBreakdown(Document document, CollectionReportDto report) throws DocumentException {
            List<AgingBreakdownDto> aging = report.getAgingBreakdown();
            if (aging == null || aging.isEmpty()) {
                return;
            }

            pdfService.addSectionTitle(document, "Aging Breakdown");

            PdfPTable agingTable = new PdfPTable(4);
            agingTable.setWidthPercentage(100);
            agingTable.setWidths(new float[]{30f, 20f, 25f, 25f});

            // Headers
            pdfService.addTableHeader(agingTable, "Bucket");
            pdfService.addTableHeader(agingTable, "Count");
            pdfService.addTableHeader(agingTable, "Amount");
            pdfService.addTableHeader(agingTable, "Percentage");

            // Data rows
            for (AgingBreakdownDto item : aging) {
                pdfService.addTableCell(agingTable, item.getBucket());
                pdfService.addTableCell(agingTable, String.valueOf(item.getCount()));
                pdfService.addAmountCell(agingTable, item.getAmount());
                pdfService.addPercentageCell(agingTable, Double.parseDouble(String.valueOf(item.getPercentage())));
            }

            document.add(agingTable);
            document.add(new Paragraph(" "));
        }

        private void addActivitySummary(Document document, CollectionReportDto report) throws DocumentException {
            pdfService.addSectionTitle(document, "Activity Summary");

            PdfPTable activityTable = new PdfPTable(4);
            activityTable.setWidthPercentage(100);
            activityTable.setWidths(new float[]{25f, 25f, 25f, 25f});

            // Headers
            pdfService.addTableHeader(activityTable, "Total Calls");
            pdfService.addTableHeader(activityTable, "Total Visits");
            pdfService.addTableHeader(activityTable, "Follow-ups");
            pdfService.addTableHeader(activityTable, "Promises to Pay");

            // Data
            pdfService.addTableCell(activityTable, String.valueOf(report.getTotalCalls()));
            pdfService.addTableCell(activityTable, String.valueOf(report.getTotalVisits()));
            pdfService.addTableCell(activityTable, String.valueOf(report.getTotalFollowUps()));
            pdfService.addTableCell(activityTable, String.valueOf(report.getPromisesToPay()));

            document.add(activityTable);
            document.add(new Paragraph(" "));
        }


        /// /Reminders Section///


        @Transactional
        @Override
        public void scheduleBulkReminders(ScheduleReminderRequestDto request, User currentUser) {
            log.debug("Scheduling bulk reminders for date: {}, type: {}, scope: {}",
                    request.getReminderDate(), request.getReminderType(), request.getRecipientScope());

            // Validate request
            validateScheduleReminderRequest(request);

            // Determine which loans to send reminders to
            List<Long> loanIds = getRecipientLoanIds(request, currentUser);

            if (loanIds.isEmpty()) {
                log.warn("No recipients found for bulk reminder scheduling");
                throw new BusinessException("No recipients found for the selected criteria");
            }

            // Create reminder schedule records
            List<ReminderSchedule> schedules = new ArrayList<>();

            for (Long loanId : loanIds) {
                ReminderSchedule schedule = ReminderSchedule.builder()
                        .loanId(loanId)
                        .scheduledDate(request.getReminderDate())
                        .scheduledTime(request.getReminderTime())
                        .reminderType(request.getReminderType())
                        .frequency(request.getFrequency() != null ? request.getFrequency() : "ONCE")
                        .messageTemplate(request.getMessageTemplate())
                        .status("PENDING")
                        .recipientScope(request.getRecipientScope())
                        .branchId(request.getBranchId())
                        .loanOfficerId(request.getLoanOfficerId())
                        .recurring(request.getRecurring() != null ? request.getRecurring() : false)
                        .recurrenceInterval(request.getRecurrenceInterval())
                        .recurrenceUnit(request.getRecurrenceUnit())
                        .endDate(request.getEndDate())
                        .build();

               // schedule.setCreatedBy(currentUser);
                schedule.setCreatedAt(LocalDateTime.now());

                schedules.add(schedule);
            }

            // Save all schedules
            reminderScheduleRepository.saveAll(schedules);

            // If sendNow is true, send immediately
            if (request.getSendNow() != null && request.getSendNow()) {
                sendRemindersImmediately(schedules, currentUser);
            }

            log.info("Scheduled {} reminders for {} recipients", schedules.size(), loanIds.size());
        }

    /**
     * Validate the schedule reminder request
     */
    private void validateScheduleReminderRequest(ScheduleReminderRequestDto request) {
        List<String> errors = new ArrayList<>();

        if (request.getReminderDate() == null) {
            errors.add("Reminder date is required");
        }

        if (request.getReminderDate() != null && request.getReminderDate().isBefore(LocalDate.now())) {
            errors.add("Reminder date cannot be in the past");
        }

        if (request.getReminderType() == null || request.getReminderType().trim().isEmpty()) {
            errors.add("Reminder type is required");
        }

        if (request.getMessageTemplate() == null || request.getMessageTemplate().trim().isEmpty()) {
            errors.add("Message template is required");
        }

        // Validate recipient scope
        if (request.getRecipientScope() == null) {
            errors.add("Recipient scope is required");
        } else {
            switch (request.getRecipientScope()) {
                case "BY_BRANCH":
                    if (request.getBranchId() == null) {
                        errors.add("Branch ID is required when scope is BY_BRANCH");
                    }
                    break;
                case "BY_OFFICER":
                    if (request.getLoanOfficerId() == null) {
                        errors.add("Loan officer ID is required when scope is BY_OFFICER");
                    }
                    break;
                case "SELECTED_LOANS":
                    if (request.getLoanIds() == null || request.getLoanIds().isEmpty()) {
                        errors.add("Loan IDs are required when scope is SELECTED_LOANS");
                    }
                    break;
                case "ALL_OVERDUE":
                    // No additional validation needed
                    break;
                default:
                    errors.add("Invalid recipient scope: " + request.getRecipientScope());
            }
        }

        // Validate recurring settings
        if (request.getRecurring() != null && request.getRecurring()) {
            if (request.getRecurrenceInterval() == null || request.getRecurrenceInterval() <= 0) {
                errors.add("Recurrence interval must be greater than 0 for recurring reminders");
            }
            if (request.getRecurrenceUnit() == null) {
                errors.add("Recurrence unit is required for recurring reminders");
            }
            if (request.getEndDate() != null && request.getEndDate().isBefore(request.getReminderDate())) {
                errors.add("End date cannot be before start date");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed: " + String.join(", ", errors));
        }
    }

    /**
     * Get loan IDs for recipients based on the request scope
     */
    private List<Long> getRecipientLoanIds(ScheduleReminderRequestDto request, User currentUser) {
        List<Long> loanIds = new ArrayList<>();

        switch (request.getRecipientScope()) {
            case "ALL_OVERDUE":
                // Get all overdue loans
                Pageable pageable = PageRequest.of(0, 1000);
                Page<Loan> overdueLoans = loanRepository.findOverdueLoans(
                        request.getBranchId(),
                        request.getLoanOfficerId(),
                        1, // min days overdue
                        null, // max days overdue
                        pageable);
                loanIds = overdueLoans.getContent().stream()
                        .map(Loan::getId)
                        .collect(Collectors.toList());
                break;

            case "SELECTED_LOANS":
                // Use provided loan IDs
                loanIds = request.getLoanIds();
                break;

            case "BY_BRANCH":
                // Get all overdue loans for the branch
                Pageable branchPageable = PageRequest.of(0, 1000);
                Page<Loan> branchLoans = loanRepository.findOverdueLoans(
                        request.getBranchId(),
                        null,
                        1,
                        null,
                        branchPageable);
                loanIds = branchLoans.getContent().stream()
                        .map(Loan::getId)
                        .collect(Collectors.toList());
                break;

            case "BY_OFFICER":
                // Get all overdue loans assigned to the officer
                Pageable officerPageable = PageRequest.of(0, 1000);
                Page<Loan> officerLoans = loanRepository.findOverdueLoans(
                        request.getBranchId(),
                        request.getLoanOfficerId(),
                        1,
                        null,
                        officerPageable);
                loanIds = officerLoans.getContent().stream()
                        .map(Loan::getId)
                        .collect(Collectors.toList());
                break;

            default:
                throw new BusinessException("Invalid recipient scope: " + request.getRecipientScope());
        }

        return loanIds;
    }

    /**
     * Send reminders immediately (not scheduled)
     */
    private void sendRemindersImmediately(List<ReminderSchedule> schedules, User currentUser) {
        // Process each schedule and send the reminder
        for (ReminderSchedule schedule : schedules) {
            try {
                sendReminder(schedule, currentUser);
                schedule.setStatus("SENT");
                schedule.setSentAt(LocalDateTime.now());
            } catch (Exception e) {
                log.error("Failed to send reminder for schedule {}: {}", schedule.getId(), e.getMessage());
                schedule.setStatus("FAILED");
                schedule.setErrorMessage(e.getMessage());
            }
            reminderScheduleRepository.save(schedule);
        }
    }

    /**
     * Send a single reminder
     */
    private void sendReminder(ReminderSchedule schedule, User currentUser) {
        // Get loan details
        Loan loan = loanRepository.findById(schedule.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        // Personalize the message
        String personalizedMessage = personalizeMessage(schedule.getMessageTemplate(), loan);

        // Send based on reminder type
        if ("SMS".equals(schedule.getReminderType()) || "BOTH".equals(schedule.getReminderType())) {
            sendSms(loan.getBorrower().getPhoneNumber(), personalizedMessage);
        }

        if ("EMAIL".equals(schedule.getReminderType()) || "BOTH".equals(schedule.getReminderType())) {
            if (loan.getBorrower().getEmail() != null) {
                sendEmail(loan.getBorrower().getEmail(), "Payment Reminder", personalizedMessage);
            }
        }

        log.debug("Reminder sent for loan: {}", loan.getLoanAccountNumber());
    }

    /**
     * Personalize message with loan details
     */
    private String personalizeMessage(String template, Loan loan) {
        String message = template;
        message = message.replace("{{borrowerName}}",
                loan.getBorrower() != null ? loan.getBorrower().getFullName() : "Valued Customer");
        message = message.replace("{{loanNumber}}", loan.getLoanAccountNumber());
        message = message.replace("{{amount}}", formatCurrency(loan.getOutstandingBalance()));
        message = message.replace("{{daysOverdue}}",
                String.valueOf(loan.getDaysDelinquent() != null ? loan.getDaysDelinquent() : 0));
        message = message.replace("{{dueDate}}",
                loan.getMaturityDate() != null ? loan.getMaturityDate().toString() : "N/A");
        message = message.replace("{{branchName}}",
                loan.getBranch() != null ? loan.getBranch().getName() : "Head Office");
        return message;
    }

    /**
     * Send SMS (mock implementation - integrate with SMS provider)
     */
    private void sendSms(String phoneNumber, String message) {
        log.debug("Sending SMS to {}: {}", phoneNumber, message);
        // TODO: Integrate with actual SMS provider (e.g., Twilio, Africa's Talking)
        // This is a mock implementation
    }

    /**
     * Send Email (mock implementation - integrate with email provider)
     */
    private void sendEmail(String email, String subject, String message) {
        log.debug("Sending email to {}: {} - {}", email, subject, message);
        // TODO: Integrate with actual email service (e.g., JavaMail, SendGrid)
        // This is a mock implementation
    }

    /**
     * Get scheduled reminders
     */
    @Transactional(readOnly = true)
    public List<ReminderScheduleDto> getScheduledReminders(User currentUser) {
        log.debug("Getting scheduled reminders for user: {}", currentUser.getUsername());

        List<ReminderSchedule> schedules = reminderScheduleRepository
                .findByStatusOrderByScheduledDateAsc("PENDING");

        return schedules.stream()
                .map(this::convertToReminderScheduleDto)
                .collect(Collectors.toList());
    }

    /**
     * Cancel a scheduled reminder
     */
    @Transactional
    public void cancelReminder(Long reminderId, User currentUser) {
        log.debug("Cancelling reminder: {} by user: {}", reminderId, currentUser.getUsername());

        ReminderSchedule schedule = reminderScheduleRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder schedule not found"));

        schedule.setStatus("CANCELLED");
        schedule.setCancelledBy(currentUser);
        schedule.setCancelledAt(LocalDateTime.now());

        reminderScheduleRepository.save(schedule);
    }

    /**
     * Convert ReminderSchedule entity to DTO
     */
    private ReminderScheduleDto convertToReminderScheduleDto(ReminderSchedule schedule) {
        Loan loan = loanRepository.findById(schedule.getLoanId()).orElse(null);

        return ReminderScheduleDto.builder()
                .id(schedule.getId())
                .scheduledDate(schedule.getScheduledDate())
                .scheduledTime(schedule.getScheduledTime())
                .reminderType(schedule.getReminderType())
                .status(schedule.getStatus())
                .recipientCount(1) // For now, count each schedule as 1 recipient
                .messageTemplate(schedule.getMessageTemplate())
                .loanId(schedule.getLoanId())
                .loanAccountNumber(loan != null ? loan.getLoanAccountNumber() : null)
                .borrowerName(loan != null && loan.getBorrower() != null ?
                        loan.getBorrower().getFullName() : null)
                .createdAt(schedule.getCreatedAt())
                .createdBy(schedule.getCreatedBy() != null ?
                        schedule.getCreatedBy().toString() : null)
                .build();
    }


    // ==================== Collection Performance Methods ====================

    @Override
    @Transactional(readOnly = true)
    public CollectionPerformanceDto getCollectionPerformance(LocalDate startDate, LocalDate endDate,
                                                             Long branchId, Long officerId,
                                                             User currentUser) {
        log.debug("Getting collection performance from {} to {}, branch: {}, officer: {}",
                startDate, endDate, branchId, officerId);

        Long effectiveBranchId = getEffectiveBranchId(branchId, currentUser);
        Long effectiveOfficerId = getEffectiveOfficerId(officerId, currentUser);

        // Get summary statistics
        PerformanceSummaryDto summary = getPerformanceSummary(startDate, endDate, effectiveBranchId, currentUser);

        // Get officer performance
        List<OfficerPerformanceDto> officerPerformance = getOfficerPerformance(
                startDate, endDate, effectiveBranchId, effectiveOfficerId, currentUser);

        // Get daily trends
        List<DailyCollectionTrendDto> dailyTrends = getCollectionTrends(
                startDate, endDate, effectiveBranchId, currentUser);

        return CollectionPerformanceDto.builder()
                .collectionRate(summary.getCollectionRate())
                .averageRecoveryTime(summary.getAverageRecoveryDays())
                .successRate(summary.getSuccessRate())
                .totalCollectedAmount(summary.getTotalCollected())
                .averageResponseTime(Integer.parseInt(String.valueOf(summary.getAverageResponseTime())) )
                .improvementTrend(summary.getImprovementTrend())
                .portfolioAtRisk(calculatePortfolioAtRisk(effectiveBranchId, endDate))
                .totalOverdue(summary.getTotalOverdue())
                .overdueAmount(summary.getOverdueAmount())
                .officerPerformance(officerPerformance)
                .dailyCollections(dailyTrends)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPerformanceReport(CollectionPerformanceDto performance, String format) {
        log.debug("Exporting performance report in format: {}", format);

        if (format.equalsIgnoreCase("PDF")) {
            return generatePerformancePdfReport(performance);
        } else {
            throw new BusinessException("Unsupported format: " + format);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OfficerPerformanceDetailDto getOfficerPerformanceDetails(Long officerId, LocalDate startDate,
                                                                    LocalDate endDate, User currentUser) {
        log.debug("Getting officer performance details for officer: {} from {} to {}",
                officerId, startDate, endDate);

        // Get officer details
        User officer = userService.getUserById(officerId);
        if (officer == null) {
            throw new ResourceNotFoundException("Officer not found with ID: " + officerId);
        }

        // Get loans assigned to this officer
        Pageable pageable = PageRequest.of(0, 1000);
        Page<Loan> assignedLoans = loanRepository.findByLoanOfficerId(officerId, pageable);

        // Get resolved loans (loans that have been paid during the period)
        List<Loan> resolvedLoans = loanRepository.findResolvedLoansByOfficerAndDateRange(
                officerId, startDate, endDate);

        // Calculate collection rate
        BigDecimal totalAssignedAmount = assignedLoans.getContent().stream()
                .map(Loan::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalResolvedAmount = resolvedLoans.stream()
                .map(Loan::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal collectionRate = totalAssignedAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalResolvedAmount.multiply(BigDecimal.valueOf(100))
                .divide(totalAssignedAmount, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculate average recovery days
        double avgRecoveryDays = resolvedLoans.stream()
                .mapToLong(loan -> {
                    if (loan.getDisbursementDate() != null && loan.getClosedDate() != null) {
                        return ChronoUnit.DAYS.between(loan.getDisbursementDate(), loan.getClosedDate());
                    }
                    return 0;
                })
                .average()
                .orElse(0);

        // Get collection actions made by this officer
        List<CollectionAction> actions = collectionActionRepository
                .findByPerformedByIdAndActionDateBetween(officerId, startDate, endDate);

        long callsMade = actions.stream()
                .filter(a -> a.getActionType() == GeneralConfig.ActionType.PHONE_CALL)
                .count();

        long visitsMade = actions.stream()
                .filter(a -> a.getActionType() == GeneralConfig.ActionType.FIELD_VISIT)
                .count();

        // Get recent activities (last 5 actions)
        List<ActivityDto> recentActivities = actions.stream()
                .sorted((a1, a2) -> a2.getActionDate().compareTo(a1.getActionDate()))
                .limit(5)
                .map(this::convertToActivityDto)
                .collect(Collectors.toList());

        // Get daily performance
        List<DailyPerformanceDto> dailyPerformance = getDailyPerformance(officerId, startDate, endDate);

        // Calculate success rate
        long successfulActions = actions.stream()
                .filter(a -> a.getOutcome() != null &&
                        (a.getOutcome() == GeneralConfig.Outcome.PROMISED_TO_PAY ||
                                a.getOutcome() == GeneralConfig.Outcome.FULL_PAYMENT ||
                                a.getOutcome() == GeneralConfig.Outcome.PARTIAL_PAYMENT))
                .count();

        BigDecimal successRate = actions.isEmpty() ? BigDecimal.ZERO :
                BigDecimal.valueOf(successfulActions)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(actions.size()), 2, RoundingMode.HALF_UP);

        return OfficerPerformanceDetailDto.builder()
                .officerId(officerId)
                .officerName(officer.getFirstName() + " " +
                        (officer.getLastName() != null ? officer.getLastName() : ""))
                .assignedLoans(assignedLoans.getNumberOfElements())
                .resolvedLoans(resolvedLoans.size())
                .resolvedAmount(totalResolvedAmount)
                .collectionRate(collectionRate)
                .avgRecoveryDays((int) avgRecoveryDays)
                .callsMade((int) callsMade)
                .visitsMade((int) visitsMade)
                .successRate(successRate)
                .recentActivities(recentActivities)
                .dailyPerformance(dailyPerformance)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PerformanceSummaryDto getPerformanceSummary(LocalDate startDate, LocalDate endDate,
                                                       Long branchId, User currentUser) {
        log.debug("Getting performance summary from {} to {}, branch: {}", startDate, endDate, branchId);

        Long effectiveBranchId = getEffectiveBranchId(branchId, currentUser);

        // Get total collected amount
        BigDecimal totalCollected = loanRepository.sumCollectionsByDateRange(
                effectiveBranchId, startDate, endDate);

        // Get total transactions
        Integer totalTransactions = loanRepository.countCollectionsByDateRange(
                effectiveBranchId, startDate, endDate);

        // Get total due amount for the period
        BigDecimal totalDue = loanRepository.sumDueByDateRange(effectiveBranchId, startDate, endDate);

        // Calculate collection rate
        BigDecimal collectionRate = totalDue.compareTo(BigDecimal.ZERO) > 0
                ? totalCollected.multiply(BigDecimal.valueOf(100))
                .divide(totalDue, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculate average recovery days
        List<Loan> resolvedLoans = loanRepository.findResolvedLoansByDateRange(
                effectiveBranchId, startDate, endDate);

        double avgRecoveryDays = resolvedLoans.stream()
                .mapToLong(loan -> {
                    if (loan.getDisbursementDate() != null && loan.getClosedDate() != null) {
                        return ChronoUnit.DAYS.between(loan.getDisbursementDate(), loan.getClosedDate());
                    }
                    return 0;
                })
                .average()
                .orElse(0);

        // Calculate success rate from collection actions
        List<CollectionAction> actions = collectionActionRepository
                .findByActionDateBetween(startDate, endDate);

        long successfulActions = actions.stream()
                .filter(a -> a.getOutcome() != null &&
                        (a.getOutcome() == GeneralConfig.Outcome.PROMISED_TO_PAY ||
                                a.getOutcome() == GeneralConfig.Outcome.FULL_PAYMENT ||
                                a.getOutcome() == GeneralConfig.Outcome.PARTIAL_PAYMENT))
                .count();

        BigDecimal successRate = actions.isEmpty() ? BigDecimal.ZERO :
                BigDecimal.valueOf(successfulActions)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(actions.size()), 2, RoundingMode.HALF_UP);

        // Get overdue stats
        Long totalOverdue = loanRepository.countOverdueLoans(effectiveBranchId, endDate);
        BigDecimal overdueAmount = loanRepository.sumOverdueAmount(effectiveBranchId, endDate);

        // Calculate improvement trend (compare to previous period)
        long daysDiff = ChronoUnit.DAYS.between(startDate, endDate);
        LocalDate prevStartDate = startDate.minusDays(daysDiff);
        LocalDate prevEndDate = endDate.minusDays(daysDiff);

        BigDecimal prevCollected = loanRepository.sumCollectionsByDateRange(
                effectiveBranchId, prevStartDate, prevEndDate);

        BigDecimal improvementTrend = prevCollected.compareTo(BigDecimal.ZERO) > 0
                ? totalCollected.subtract(prevCollected)
                .multiply(BigDecimal.valueOf(100))
                .divide(prevCollected, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Get active officers count
        Integer activeOfficers = loanRepository.countActiveLoanOfficers(effectiveBranchId);

        // Calculate average response time (mock implementation)
        BigDecimal avgResponseTime = BigDecimal.valueOf(48); // 48 hours default

        return PerformanceSummaryDto.builder()
                .totalCollected(totalCollected)
                .totalTransactions(totalTransactions)
                .collectionRate(collectionRate)
                .averageRecoveryDays((int) avgRecoveryDays)
                .successRate(successRate)
                .totalOverdue(totalOverdue != null ? totalOverdue.intValue() : 0)
                .overdueAmount(overdueAmount != null ? overdueAmount : BigDecimal.ZERO)
                .activeOfficers(activeOfficers != null ? activeOfficers : 0)
                .improvementTrend(improvementTrend)
                .averageResponseTime(avgResponseTime)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyCollectionTrendDto> getCollectionTrends(LocalDate startDate, LocalDate endDate,
                                                             Long branchId, User currentUser) {
        log.debug("Getting collection trends from {} to {}, branch: {}", startDate, endDate, branchId);

        Long effectiveBranchId = getEffectiveBranchId(branchId, currentUser);
        List<DailyCollectionTrendDto> trends = new ArrayList<>();

        LocalDate current = startDate;
        BigDecimal totalCollected = loanRepository.sumCollectionsByDateRange(
                effectiveBranchId, startDate, endDate);

        while (!current.isAfter(endDate)) {
            BigDecimal dailyAmount = loanRepository.sumCollectionsByDateRange(
                    effectiveBranchId, current, current);
            Integer loansCollected = loanRepository.countCollectionsByDateRange(
                    effectiveBranchId, current, current);

            BigDecimal percentage = totalCollected.compareTo(BigDecimal.ZERO) > 0
                    ? dailyAmount.multiply(BigDecimal.valueOf(100))
                    .divide(totalCollected, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            trends.add(DailyCollectionTrendDto.builder()
                    .date(current)
                    .amountCollected(dailyAmount)
                    .loansCollected(loansCollected != null ? loansCollected : 0)
                    .collectionPercentage(percentage)
                    .build());

            current = current.plusDays(1);
        }

        return trends;
    }

// ==================== Private Helper Methods ====================

    /**
     * Get officer performance list
     */
    private List<OfficerPerformanceDto> getOfficerPerformance(LocalDate startDate, LocalDate endDate,
                                                              Long branchId, Long officerId,
                                                              User currentUser) {
        List<OfficerPerformanceDto> performances = new ArrayList<>();

        List<User> officers;
        if (officerId != null) {
            User officer = userService.getUserById(officerId);
            officers = officer != null ? Collections.singletonList(officer) : Collections.emptyList();
        } else {
            // Get all collection officers
            officers = userService.getUsersByRole(User.UserRole.COLLECTION_OFFICER);
        }

        for (User officer : officers) {
            // Get assigned loans
            Pageable pageable = PageRequest.of(0, 1000);
            Page<Loan> assignedLoans = loanRepository.findByLoanOfficerId(officer.getId(), pageable);

            // Get resolved loans
            List<Loan> resolvedLoans = loanRepository.findResolvedLoansByOfficerAndDateRange(
                    officer.getId(), startDate, endDate);

            // Calculate resolved amount
            BigDecimal resolvedAmount = resolvedLoans.stream()
                    .map(Loan::getPrincipalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate collection rate
            BigDecimal totalAssigned = assignedLoans.getContent().stream()
                    .map(Loan::getPrincipalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal collectionRate = totalAssigned.compareTo(BigDecimal.ZERO) > 0
                    ? resolvedAmount.multiply(BigDecimal.valueOf(100))
                    .divide(totalAssigned, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // Get collection actions
            List<CollectionAction> actions = collectionActionRepository
                    .findByPerformedByIdAndActionDateBetween(officer.getId(), startDate, endDate);

            long callsMade = actions.stream()
                    .filter(a -> a.getActionType() == GeneralConfig.ActionType.PHONE_CALL)
                    .count();

            long visitsMade = actions.stream()
                    .filter(a -> a.getActionType() == GeneralConfig.ActionType.FIELD_VISIT)
                    .count();

            performances.add(OfficerPerformanceDto.builder()
                    .officerId(officer.getId())
                    .officerName(officer.getFirstName() + " " +
                            (officer.getLastName() != null ? officer.getLastName() : ""))
                    .assignedLoans(assignedLoans.getNumberOfElements())
                    .resolvedLoans(resolvedLoans.size())
                    .resolvedAmount(resolvedAmount)
                    .callsMade((int) callsMade)
                    .visitsMade((int) visitsMade)
                    .collectionRate(collectionRate)
                    .build());
        }

        return performances;
    }

    /**
     * Get daily performance for an officer
     */
    private List<DailyPerformanceDto> getDailyPerformance(Long officerId, LocalDate startDate, LocalDate endDate) {
        List<DailyPerformanceDto> dailyPerformance = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            // Get actions for this day
            List<CollectionAction> actions = collectionActionRepository
                    .findByPerformedByIdAndActionDateBetween(officerId, current, current);

            long callsMade = actions.stream()
                    .filter(a -> a.getActionType() == GeneralConfig.ActionType.PHONE_CALL)
                    .count();

            long visitsMade = actions.stream()
                    .filter(a -> a.getActionType() == GeneralConfig.ActionType.FIELD_VISIT)
                    .count();

            // Get amount collected on this day
            BigDecimal amountCollected = loanRepository.sumCollectionsByDateRange(
                    null, current, current);

            // Get loans resolved on this day
            Integer loansResolved = loanRepository.countCollectionsByDateRange(
                    null, current, current);

            dailyPerformance.add(DailyPerformanceDto.builder()
                    .date(current)
                    .amountCollected(amountCollected)
                    .callsMade((int) callsMade)
                    .visitsMade((int) visitsMade)
                    .loansResolved(loansResolved != null ? loansResolved : 0)
                    .build());

            current = current.plusDays(1);
        }

        return dailyPerformance;
    }

    /**
     * Calculate Portfolio at Risk (PAR)
     */
    private BigDecimal calculatePortfolioAtRisk(Long branchId, LocalDate asOfDate) {
        BigDecimal totalPortfolio = loanRepository.sumOutstandingBalanceByBranch(branchId);
        BigDecimal overdueAmount = loanRepository.sumOverdueAmount(branchId, asOfDate);

        return totalPortfolio.compareTo(BigDecimal.ZERO) > 0
                ? overdueAmount.multiply(BigDecimal.valueOf(100))
                .divide(totalPortfolio, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    /**
     * Convert CollectionAction to ActivityDto
     */
    private ActivityDto convertToActivityDto(CollectionAction action) {
        String icon = getActionIcon(action.getActionType());
        String description = generateActionDescription(action);

        return ActivityDto.builder()
                .id(action.getId())
                .icon(icon)
                .description(description)
                .time(action.getActionDate().atTime(
                        action.getActionTime() != null ? action.getActionTime() : LocalTime.MIDNIGHT))
                .type(action.getActionType().name())
                .build();
    }

    /**
     * Get icon for action type
     */
    private String getActionIcon(GeneralConfig.ActionType actionType) {
        switch (actionType) {
            case PHONE_CALL:
                return "pi pi-phone";
            case SMS:
                return "pi pi-comment";
            case EMAIL:
                return "pi pi-envelope";
            case FIELD_VISIT:
            case HOME_VISIT:
            case OFFICE_VISIT:
                return "pi pi-map-marker";
            case MEETING:
                return "pi pi-users";
            case FOLLOW_UP:
                return "pi pi-clock";
            default:
                return "pi pi-note";
        }
    }

    /**
     * Generate PDF report for performance data
     */
    private byte[] generatePerformancePdfReport(CollectionPerformanceDto performance) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            pdfService.addDocumentHeader(document, "Performance Report", "Collection Performance Analysis");

            // Add summary statistics
            pdfService.addSectionTitle(document, "Performance Summary");
            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            pdfService.addTableHeader(summaryTable, "Collection Rate");
            pdfService.addTableHeader(summaryTable, "Avg Recovery Time");
            pdfService.addTableHeader(summaryTable, "Success Rate");
            pdfService.addTableHeader(summaryTable, "Total Collected");

            pdfService.addPercentageCell(summaryTable, performance.getCollectionRate().doubleValue());
            pdfService.addTableCell(summaryTable, performance.getAverageRecoveryTime() + " days");
            pdfService.addPercentageCell(summaryTable, performance.getSuccessRate().doubleValue());
            pdfService.addAmountCell(summaryTable, performance.getTotalCollectedAmount());

            document.add(summaryTable);
            document.add(new Paragraph(" "));

            // Add officer performance table
            pdfService.addSectionTitle(document, "Officer Performance");
            PdfPTable officerTable = new PdfPTable(6);
            officerTable.setWidthPercentage(100);
            pdfService.addTableHeader(officerTable, "Officer");
            pdfService.addTableHeader(officerTable, "Assigned");
            pdfService.addTableHeader(officerTable, "Resolved");
            pdfService.addTableHeader(officerTable, "Amount");
            pdfService.addTableHeader(officerTable, "Calls");
            pdfService.addTableHeader(officerTable, "Rate");

            for (OfficerPerformanceDto officer : performance.getOfficerPerformance()) {
                pdfService.addTableCell(officerTable, officer.getOfficerName());
                pdfService.addTableCell(officerTable, String.valueOf(officer.getAssignedLoans()));
                pdfService.addTableCell(officerTable, String.valueOf(officer.getResolvedLoans()));
                pdfService.addAmountCell(officerTable, officer.getResolvedAmount());
                pdfService.addTableCell(officerTable, String.valueOf(officer.getCallsMade()));
                pdfService.addPercentageCell(officerTable, officer.getCollectionRate().doubleValue());
            }
            document.add(officerTable);
            document.add(new Paragraph(" "));

            pdfService.addDocumentFooter(document);
            document.close();

            return baos.toByteArray();

        } catch (DocumentException | IOException e) {
            log.error("Error generating performance PDF report", e);
            throw new BusinessException("Failed to generate PDF report: " + e.getMessage());
        }
    }





    }

