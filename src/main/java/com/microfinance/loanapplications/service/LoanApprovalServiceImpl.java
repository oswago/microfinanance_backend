package com.microfinance.loanapplications.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.audit.service.AuditService;
import com.microfinance.common.service.NotificationService;
import com.microfinance.exception.BusinessException;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.loanapplications.dto.*;
import com.microfinance.loanapplications.dto.ApplicationApprovalDto;
import com.microfinance.loanapplications.dto.approval.*;
import com.microfinance.loanapplications.entity.*;
import com.microfinance.loanapplications.mapper.ApprovalMapper;
import com.microfinance.loanapplications.mapper.LoanApplicationMapper;
import com.microfinance.loanapplications.repository.*;
import com.microfinance.loanproducts.repository.LoanProductRepository;
import com.microfinance.system.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.microfinance.loanapplications.dto.approval.ApprovalDecisionDto.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoanApprovalServiceImpl implements LoanApprovalService {

    // Repositories
    private final LoanApplicationRepository loanApplicationRepository;
    private final ApplicationApprovalRepository approvalRepository;
    private final ApprovalConditionRepository approvalConditionRepository;
    private final LoanRepository loanRepository;
    private final BranchRepository branchRepository;
    private final LoanProductRepository loanProductRepository;

    private final UserRepository userRepository;
    private final ApprovalDelegationRepository approvalDelegationRepository;
    private final ApprovalCommentRepository approvalCommentRepository;
    private final ApprovalEscalationRepository approvalEscalationRepository;
    private final ApprovalReminderRepository approvalReminderRepository;

    @Autowired
    private final RepaymentScheduleGenerationService scheduleGenerationService;

    @Autowired
    private ApplicationApprovalRepository repository;

    // Mappers
    private final LoanApplicationMapper loanApplicationMapper;
    private final ApprovalMapper approvalMapper;

    // Services
    private final SecurityUtils securityUtils;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    // Cache for frequently accessed data
    private final Map<Long, ApprovalWorkflowDto> workflowCache = new ConcurrentHashMap<>();
    private final Map<String, ApprovalStatsDto> statsCache = new ConcurrentHashMap<>();
    private final ApprovalWorkflowRulesService workflowRules;

    // Constants
    private static final int APPROVAL_SLA_HOURS = 24;
    private static final int MAX_BULK_APPROVAL_LIMIT = 50;
    private static final int CACHE_TTL_MINUTES = 5;

    @Override
    @Transactional
    public LoanApplicationDto approveApplication(Long applicationId, ApprovalDecisionDto dto, User approver) {
        log.info("========== STARTING APPROVAL PROCESS ==========");
        log.info("Approving application {} by user {}", applicationId, approver.getUsername());

        long startTime = System.currentTimeMillis();
        try {
            // ===== STEP 1: Validate application exists =====
            log.debug("STEP 1: Fetching application from database...");
            LoanApplication application = loanApplicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format("Loan application not found with id: %d", applicationId)));
            log.debug("✓ Application found: ID={}, Number={}, Status={}",
                    application.getId(), application.getApplicationNumber(), application.getStatus());

            // ===== STEP 2: Validate application state =====
            log.debug("STEP 2: Validating application for approval...");
            validateApplicationForApproval(application, approver);
            log.debug("✓ Application validation passed");

            // ===== STEP 3: Check user permissions =====
            log.debug("STEP 3: Checking user approval permissions...");
            if (!canUserApproveApplication(applicationId, approver)) {
                log.warn("✗ User {} does not have permission to approve application {}",
                        approver.getUsername(), application.getApplicationNumber());
                throw new BusinessException(
                        String.format("User %s does not have permission to approve application %s",
                                approver.getUsername(), application.getApplicationNumber()));
            }
            log.debug("✓ User has approval permission");


            // ===== NEW: Determine current approval level and check if final =====
            int currentLevel = determineApprovalLevel(application, approver);
            int totalLevels = getTotalApprovalLevels(application);
            boolean isFinalApproval = (currentLevel >= totalLevels);

            log.info("Current approval level: {}, Total levels: {}, Is final: {}",
                    currentLevel, totalLevels, isFinalApproval);

            // Validate that user can approve at this level based on amount
            if (!canUserApproveAtAmountLevel(application, approver)) {
                throw new BusinessException(String.format(
                        "User %s with role %s cannot approve amount %s. Maximum allowed: %s",
                        approver.getUsername(), approver.getRole(),
                        application.getAppliedAmount(),
                        getMaxApprovalAmountForRole(approver.getRole())));
            }

            // ===== STEP 4: Create approval record =====
            log.debug("STEP 4: Creating approval record...");
            ApplicationApproval approval = createApprovalRecord(application, approver, dto,
                    GeneralConfig.ApprovalDecision.APPROVED);
            approval = approvalRepository.save(approval);
            log.debug("✓ Approval record created with ID: {}", approval.getId());

            // ===== STEP 5: Update application after approval =====
            log.debug("STEP 5: Updating application after approval...");
            boolean finalApproval = isFinalApproval(application, approver);
            log.debug("Is final approval: {}", finalApproval);
            updateApplicationAfterApproval(application, approver, dto);
            log.debug("✓ Application updated and ✓ Approval handling complete");

            // ===== STEP 7: Save application =====
            log.debug("STEP 7: Saving application...");
            LoanApplication savedApp = loanApplicationRepository.save(application);
            log.debug("✓ Application saved with ID: {}", savedApp.getId());

            // ===== STEP 8: Clear caches =====
            log.debug("STEP 8: Clearing caches...");
            clearCaches(applicationId);
            log.debug("✓ Caches cleared");

            // ===== STEP 9: Send notifications =====
            log.debug("STEP 9: Sending notifications...");
            //sendApprovalNotifications(application, approver, dto);
            sendApprovalNotificationsWithLevel(application, approver, dto, currentLevel, totalLevels, isFinalApproval);
            log.debug("✓ Notifications sent");

            // ===== STEP 10: Audit log =====
            log.debug("STEP 10: Creating audit log...");
            auditService.logApprovalAction(applicationId, "APPROVE", approver.getId(), dto.getComments());

            //Audit Section
            Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
            String createdByName ="";
            Long createdById=null;
            if(currentUser1.isPresent()){
                createdByName=currentUser1.get().getFullName();
                createdById=currentUser1.get().getId();
            }


            if (Objects.nonNull(savedApp.getId())) {
                // Build audit message safely
                StringBuilder auditMessage = new StringBuilder()
                        .append("Loan Application of ID: ").append(savedApp.getId());

                // Safe check for loan
                if (savedApp.getLoan() != null) {
                    auditMessage.append(" Loan No: ").append(savedApp.getLoan().getLoanAccountNumber());
                } else {
                    auditMessage.append(" (Loan not yet associated)");
                }
                auditMessage.append(" has been APPROVED by: ").append(createdByName).append("-").append(createdById);

                auditService.masterAuditLogs(
                        savedApp.getBorrower().getId(),
                        GeneralConfig.BorrowerActivityType.LOAN_APPLICATION_APPROVAL_ACTIVITY,
                        "APPLICATION_APPROVAL",
                        auditMessage.toString()
                );
               //InAppNotification
                if(isFinalApproval){
                    notificationService.createLoanApprovedNotification(
                            applicationId,
                            application.getApplicationNumber(),
                            approver.getId(),
                            application.getBorrower().getId()
                    );
                }

            }
            //End Audit Section

            log.debug("✓ Audit log created");
            LoanApplicationDto result = enrichLoanApplicationDto(savedApp);

            return result;

        } catch (StackOverflowError e) {
            throw new BusinessException("StackOverflowError due to circular reference: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ FAILED to approve application {}: {}", applicationId, e.getMessage());
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Stack trace:", e);
            throw new BusinessException("Failed to approve application: " + e.getMessage(), String.valueOf(e));
        }
    }


    /**
     * Get total number of approval levels required for this application
     */

    private int getTotalApprovalLevels(LoanApplication application) {
        return workflowRules.getTotalApprovalLevels(application);
    }

    /**
     * Check if user can approve based on amount limits
     */


    private boolean canUserApproveAtAmountLevel(LoanApplication application, User approver) {
        return workflowRules.canUserApproveAtAmountLevel(application.getAppliedAmount(), approver.getRole());
    }

    /**
     * Get maximum approval amount for a user role
     */

    private BigDecimal getMaxApprovalAmountForRole(User.UserRole role) {
        return workflowRules.getMaxApprovalAmountForRole(role);
    }

    /**
     * Get the role required for the next approval level
     */

    private String getNextApprovalRole(LoanApplication application, int currentLevel) {
        return workflowRules.getNextApprovalRole(application, currentLevel);
    }


        private String getCurrentStep(Exception e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (element.getMethodName().contains("approveApplication") ||
                    element.getMethodName().contains("enrichLoanApplicationDto") ||
                    element.getMethodName().contains("toDto")) {
                return element.getMethodName() + " at " + element.getLineNumber();
            }
        }
        return "unknown";
    }


    @Override
    @Transactional
    public LoanApplicationDto rejectApplication(Long applicationId, ApprovalDecisionDto dto, User approver) {
        log.info("Rejecting application {} by user {}", applicationId, approver.getUsername());

        try {
            LoanApplication application = loanApplicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format("Loan application not found with id: %d", applicationId)));

            validateApplicationForRejection(application, approver);

            // Create rejection record
            ApplicationApproval approval = createApprovalRecord(application, approver, dto,
                    GeneralConfig.ApprovalDecision.REJECTED);
            approvalRepository.save(approval);

            // Update application
            application.setStatus(GeneralConfig.LoanApplicationStatus.REJECTED);
            application.setRejectionReason(dto.getComments());
            application.setRejectedDate(LocalDateTime.now());
            application.setRejectedBy(approver.getUsername());
            application.setStage(GeneralConfig.ApplicationStage.CLOSED);

            // Handle rejection workflow
            handleRejectionWorkflow(application, dto);

            LoanApplication savedApp = loanApplicationRepository.save(application);

            // Clear caches
            clearCaches(applicationId);

            // Send notifications
            sendRejectionNotifications(application, approver, dto);

            // Audit
            auditService.logApprovalAction(applicationId, "REJECT", approver.getId(), dto.getComments());

            //Audit Section
            Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
            String createdByName ="";
            Long createdById=null;
            if(currentUser1.isPresent()){
                createdByName=currentUser1.get().getFullName();
                createdById=currentUser1.get().getId();
            }
            if (Objects.nonNull(savedApp.getId())) {
                auditService.masterAuditLogs(
                        savedApp.getBorrower().getId(),
                        GeneralConfig.BorrowerActivityType.LOAN_APPLICATION_REJECTED,
                        "APPLICATION_REJECTION",
                        "Loan Application of ID:"+savedApp.getId()+" Loan No:"+savedApp.getLoan().getLoanAccountNumber()+  " has been REJECTED by:"+createdByName+"-"+createdById
                );
            }
               //IanAppnotification
                notificationService.createLoanRejectedNotification(
                        applicationId,
                        application.getApplicationNumber(),
                        dto.getComments(),
                        application.getBorrower().getId()
                );

            //End Audit Section

            log.info("Application {} rejected successfully by {}", applicationId, approver.getUsername());

            return enrichLoanApplicationDto(savedApp);

        } catch (Exception e) {
            log.error("Failed to reject application {}: {}", applicationId, e.getMessage(), e);
            throw new BusinessException("Failed to reject application: " + e.getMessage(), String.valueOf(e));
        }
    }

    @Override
    @Transactional
    public LoanApplicationDto returnForRevision(Long applicationId, ApprovalDecisionDto dto, User approver) {
        log.info("Returning application {} for revision by user {}", applicationId, approver.getUsername());

        try {
            LoanApplication application = loanApplicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format("Loan application not found with id: %d", applicationId)));

            validateApplicationForReturn(application, approver);

            // Create return record
            ApplicationApproval approval = createApprovalRecord(application, approver, dto,
                    GeneralConfig.ApprovalDecision.RETURNED_FOR_REVISION);
            approvalRepository.save(approval);

            // Update application
            application.setStatus(GeneralConfig.LoanApplicationStatus.NEEDS_REVISION);
            application.setStage(GeneralConfig.ApplicationStage.APPLICATION);
            application.setRevisionNotes(dto.getComments());
            application.setReturnedDate(LocalDateTime.now());
            application.setReturnedBy(approver.getUsername());

            // Create conditions if specified
            if (!CollectionUtils.isEmpty(dto.getRequirements())) {
                createRevisionConditions(application, dto.getRequirements(), approver);
            }

            LoanApplication savedApp = loanApplicationRepository.save(application);

            // Clear caches
            clearCaches(applicationId);

            // Send notifications
            sendReturnNotifications(application, approver, dto);

            // Audit
            auditService.logApprovalAction(applicationId, "RETURN", approver.getId(), dto.getComments());
            //Audit Section
            Optional<User> currentUser1 = userRepository.findById(securityUtils.getCurrentUserId());
            String createdByName ="";
            Long createdById=null;
            if(currentUser1.isPresent()){
                createdByName=currentUser1.get().getFullName();
                createdById=currentUser1.get().getId();
            }
            if (Objects.nonNull(savedApp.getId())) {
                auditService.masterAuditLogs(
                        savedApp.getBorrower().getId(),
                        GeneralConfig.BorrowerActivityType.LOAN_APPLICATION_RETURNED,
                        "LOAN_APPLICATION",
                        "Loan Application of ID:"+savedApp.getId()+" Loan No:"+savedApp.getLoan().getLoanAccountNumber()+  " has been RETURNED for REVISION by:"+createdByName+"-"+createdById
                );
            }
            //End Audit Section


            log.info("Application {} returned for revision by {}", applicationId, approver.getUsername());

            return enrichLoanApplicationDto(savedApp);

        } catch (Exception e) {
            log.error("Failed to return application {} for revision: {}", applicationId, e.getMessage(), e);
            throw new BusinessException("Failed to return application for revision: " + e.getMessage(), String.valueOf(e));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanApplicationDto> getApplicationsForApproval(User approver) {
        log.debug("Getting applications for approval for user: {}", approver.getUsername());

        // Get applications using the default method with fixed statuses
        List<LoanApplication> applications = loanApplicationRepository
                .findPendingApprovalsForUser(approver.getId(), approver.getBranchId());

        // Apply priority-based sorting
        List<LoanApplication> sortedApplications = sortApplicationsByPriority(applications);

        // Filter by additional permission checks
        List<LoanApplication> filteredApplications = sortedApplications.stream()
                .filter(app -> canUserApproveApplication(app.getId(), approver))
                .collect(Collectors.toList());

        return filteredApplications.stream()
                .map(this::enrichLoanApplicationDto)
                .collect(Collectors.toList());
    }



    private List<LoanApplication> sortApplicationsByPriority(List<LoanApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return applications;
        }

        return applications.stream()
                .sorted((a1, a2) -> {
                    // Compare by large amount priority
                    boolean a1IsLarge = isLargeAmount(a1);
                    boolean a2IsLarge = isLargeAmount(a2);

                    if (a1IsLarge && !a2IsLarge) return -1;
                    if (!a1IsLarge && a2IsLarge) return 1;

                    // Both are same large amount priority, check overdue
                    boolean a1IsOverdue = isOverdue(a1);
                    boolean a2IsOverdue = isOverdue(a2);

                    if (a1IsOverdue && !a2IsOverdue) return -1;
                    if (!a1IsOverdue && a2IsOverdue) return 1;

                    // Both are same priority, sort by submission date
                    return a1.getSubmittedDate().compareTo(a2.getSubmittedDate());
                })
                .collect(Collectors.toList());
    }



    private boolean isLargeAmount(LoanApplication application) {
        return workflowRules.isLargeAmount(application);
    }

private boolean isOverdue(LoanApplication application) {
    return workflowRules.isOverdue(application);
}

    @Transactional(readOnly = true)
    @Override
    public Page<PendingApprovalDto> getPendingApprovals(User approver, Pageable pageable,
                                                        ApprovalFilterDto filter) {
        log.info("Getting pending approvals with filters for user: {}", approver.getUsername());

        // Convert dates to LocalDateTime
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (filter != null) {
            if (filter.getStartDate() != null) {
                startDateTime = filter.getStartDate().atStartOfDay();
                log.info("Start date filter: {}", startDateTime);
            }
            if (filter.getEndDate() != null) {
                endDateTime = filter.getEndDate().atTime(LocalTime.MAX);
                log.info("End date filter: {}", endDateTime);
            }
        }

        // Convert BigDecimal to Double
        Double minAmount = filter != null && filter.getMinAmount() != null
                ? filter.getMinAmount().doubleValue()
                : null;
        Double maxAmount = filter != null && filter.getMaxAmount() != null
                ? filter.getMaxAmount().doubleValue()
                : null;

        log.info("Query parameters - userId: {}, userBranchId: {}, branchId: {}, minAmount: {}, maxAmount: {}, productType: {}",
                approver.getId(), approver.getBranchId(),
                filter != null ? filter.getBranchId() : null,
                minAmount, maxAmount,
                filter != null ? filter.getProductType() : null);

        // Get filtered applications
        List<LoanApplication> applications = loanApplicationRepository
                .findPendingApprovalsWithFilters(
                        approver.getId(),
                        approver.getBranchId(),
                        filter != null ? filter.getBranchId() : null,
                        minAmount,
                        maxAmount,
                        filter != null ? filter.getProductType() : null,
                        startDateTime,
                        endDateTime
                );

        log.info("STEP 1 - Repository returned {} applications", applications.size());

        if (!applications.isEmpty()) {
            applications.forEach(app -> {
                log.info("  - App ID: {}, Status: {}, Submitted: {}",
                        app.getId(), app.getStatus(), app.getSubmittedDate());
            });
        } else {
            log.info("No applications found by repository query!");

            // DEBUG: Check if there are ANY pending applications in the database
            List<LoanApplication> allWithStatus = loanApplicationRepository
                    .findByStatusIn(List.of(
                            GeneralConfig.LoanApplicationStatus.SUBMITTED,
                            GeneralConfig.LoanApplicationStatus.UNDER_REVIEW,
                            GeneralConfig.LoanApplicationStatus.PENDING_APPROVAL
                    ));
            log.info("Total applications with pending status in DB: {}", allWithStatus.size());

            if (!allWithStatus.isEmpty()) {
                allWithStatus.forEach(app -> {
                    log.info("  - Found in DB: ID={}, Status={}, CreatedBy={}, BranchId={}",
                            app.getId(), app.getStatus(), app.getCreatedBy(),
                            app.getBranch() != null ? app.getBranch().getId() : null);
                });
            }
        }

        // Apply priority-based sorting
        List<LoanApplication> sortedApplications = applyPrioritySorting(applications);
        log.info("STEP 2 - After sorting: {} applications", sortedApplications.size());

        // Apply additional filtering based on approval permissions
        List<LoanApplication> filteredApplications = sortedApplications.stream()
                .filter(app -> {
                    boolean canApprove = canUserApproveApplication(app.getId(), approver);
                    log.info("  - App {} can be approved by {}: {}",
                            app.getId(), approver.getUsername(), canApprove);
                    return canApprove;
                })
                .collect(Collectors.toList());

        log.info("STEP 3 - After permission filtering: {} applications", filteredApplications.size());

        // Apply pagination
        int totalSize = filteredApplications.size();
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), totalSize);

        List<LoanApplication> pagedApplications = totalSize > start
                ? filteredApplications.subList(start, end)
                : Collections.emptyList();

        log.info("STEP 4 - After pagination (start={}, end={}): {} applications",
                start, end, pagedApplications.size());

        // Convert to DTOs
        List<PendingApprovalDto> dtos = pagedApplications.stream()
                .map(this::convertToPendingApprovalDto)
                .collect(Collectors.toList());

        log.info("Final result: returning {} DTOs", dtos.size());

        return new PageImpl<>(dtos, pageable, totalSize);
    }

    /**
     * Apply priority-based sorting to applications:
     * 1. Large amounts (≥ 500,000)
     * 2. Overdue applications (submitted 3+ days ago)
     * 3. Others by submission date (oldest first)
     */
    private List<LoanApplication> applyPrioritySorting(List<LoanApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return applications;
        }

        // Group applications by priority
        Map<Integer, List<LoanApplication>> groupedByPriority = new TreeMap<>();

        for (LoanApplication app : applications) {
            int priority = calculatePriorityScore(app);
            groupedByPriority.computeIfAbsent(priority, k -> new ArrayList<>()).add(app);
        }

        // Sort each group by submission date and combine
        List<LoanApplication> sortedApplications = new ArrayList<>();
        for (Map.Entry<Integer, List<LoanApplication>> entry : groupedByPriority.entrySet()) {
            List<LoanApplication> group = entry.getValue();
            group.sort(Comparator.comparing(LoanApplication::getSubmittedDate));
            sortedApplications.addAll(group);
        }

        return sortedApplications;
    }

    /**
     * Calculate days since submission for an application
     */
    private Long calculateDaysSinceSubmission(LoanApplication application) {
        return workflowRules.calculateDaysSinceSubmission(application);
    }

    /**
     * Calculate priority score:
     * 0 = High priority (large amount ≥ 500,000)
     * 1 = Medium priority (overdue, submitted 3+ days ago)
     * 2 = Low priority (others)
     */

    private int calculatePriorityScore(LoanApplication application) {
        return workflowRules.calculatePriorityScore(application);
    }


    /**
     * Helper class to store application with priority information
     */
    private static class ApplicationWithPriority {
        LoanApplication application;
        Long daysSinceSubmission;
        int priorityScore;

        ApplicationWithPriority(LoanApplication application, Long daysSinceSubmission, int priorityScore) {
            this.application = application;
            this.daysSinceSubmission = daysSinceSubmission;
            this.priorityScore = priorityScore;
        }
    }


    @Override
    @Transactional(readOnly = true)
    public List<ApplicationApprovalDto> getApprovalHistory(Long applicationId) {
        log.debug("Getting approval history for application: {}", applicationId);

        List<ApplicationApproval> approvals = approvalRepository
                .findByLoanApplicationIdOrderByCreatedAtDesc(applicationId);

        List<ApplicationApprovalDto> list = new ArrayList<>();
        for (ApplicationApproval approval : approvals) {
           ApplicationApprovalDto applicationApprovalDto = convertToApprovalDto(approval);
            list.add(applicationApprovalDto);
        }
        return list;
    }

    @Override
    public ApprovalWorkflowDto getApprovalWorkflow(Long applicationId) {
        log.debug("Getting approval workflow for application: {}", applicationId);

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        User currentUser = userService.getUserById(securityUtils.getCurrentUserId());

        // Get existing approvals
        List<ApplicationApproval> approvals = approvalRepository
                .findByLoanApplicationIdOrderByApprovalLevelAsc(applicationId);

        // Get conditions
        List<ApprovalCondition> conditions = approvalConditionRepository
                .findByLoanApplicationId(applicationId);

        return buildApprovalWorkflow(application, approvals, conditions, currentUser);
    }


    @Override
    @Transactional(readOnly = true)
    public boolean canUserApproveApplication(Long applicationId, User user) {
        log.debug("Checking if user {} can approve application {}", user.getUsername(), applicationId);

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Loan application not found with id: %d", applicationId)));
        // Comprehensive validation
        return validateUserCanApprove(application, user);
    }

    @Transactional(readOnly = true)
    @Override
    public ApprovalWorkflowDto getApprovalWorkflow(Long applicationId, User currentUser) {
        log.debug("Getting approval workflow for application: {}", applicationId);

        // Check cache first
        ApprovalWorkflowDto cached = workflowCache.get(applicationId);
        if (cached != null) {
            return cached;
        }

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Loan application not found with id: %d", applicationId)));

        // Get approvals and build workflow
        List<ApplicationApproval> approvals = approvalRepository
                .findByLoanApplicationIdOrderByApprovalLevelAsc(applicationId);

        List<ApprovalCondition> conditions = approvalConditionRepository
                .findByLoanApplicationId(applicationId);

        ApprovalWorkflowDto workflow = buildApprovalWorkflow(application, approvals, conditions, currentUser);

        // Cache the result
        workflowCache.put(applicationId, workflow);

        return workflow;
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalPerformanceDto getApprovalPerformance(Long approverId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting approval performance for approver: {}, period: {} to {}",
                approverId, startDate, endDate);

        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        List<ApplicationApproval> approvals = approvalRepository
                .findByApproverIdAndDecisionDateBetween(
                        approverId,
                        startDate.atStartOfDay(),
                        endDate.plusDays(1).atStartOfDay()
                );

        return calculatePerformanceMetrics(approverId, approvals, startDate, endDate);
    }

    @Transactional(readOnly = true)
    @Override
    public ApprovalStatsDto getApprovalStatistics(User user, Long branchId, LocalDate startDate,
                                                  LocalDate endDate, String period) {
        log.debug("Getting approval statistics for user: {}", user.getUsername());

        String cacheKey = buildStatsCacheKey(user.getId(), branchId, period);
        ApprovalStatsDto cached = statsCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Determine date range
        LocalDate[] dateRange = calculateDateRange(startDate, endDate, period);
        startDate = dateRange[0];
        endDate = dateRange[1];

        // Calculate statistics
        ApprovalStatsDto stats = calculateApprovalStatistics(user, branchId, startDate, endDate);

        // Cache the result
        statsCache.put(cacheKey, stats);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalStatsDto getApprovalStatisticsForUser(User currentUser, Long targetUserId, Long branchId,
                                                         LocalDate startDate, LocalDate endDate, String period) {

        log.debug("Getting approval statistics for target user: {} requested by: {}",
                targetUserId, currentUser.getUsername());

        // Check permissions
       /* if (!canViewOtherUsersStats(currentUser, targetUserId)) {
            throw new BusinessException("You do not have permission to view statistics for this user");
        }*/

        User targetUser = userService.getUserById(targetUserId);

        // Determine date range
        LocalDate[] dateRange = calculateDateRange(startDate, endDate, period);
        startDate = dateRange[0];
        endDate = dateRange[1];

        // Calculate statistics for target user
        return calculateApprovalStatistics(targetUser, branchId, startDate, endDate);
    }

    @Transactional
    @Override
    public BulkApprovalResult bulkApprovePendingApplications(BulkApprovalRequestDto request, User approver) {
        log.info("Processing bulk approval for {} applications by {}",
                request.getApplicationIds().size(), approver.getUsername());

        // Validate bulk approval limit
        if (request.getApplicationIds().size() > MAX_BULK_APPROVAL_LIMIT) {
            throw new BusinessException(
                    String.format("Bulk approval limit exceeded. Maximum allowed: %d", MAX_BULK_APPROVAL_LIMIT));
        }

        List<Long> successful = Collections.synchronizedList(new ArrayList<>());
        List<BulkApprovalResult.BulkApprovalError> errors = Collections.synchronizedList(new ArrayList<>());

        // Process applications in parallel for better performance
        request.getApplicationIds().parallelStream().forEach(appId -> {
            try {
                // Validate each application
                LoanApplication application = loanApplicationRepository.findById(appId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                String.format("Application not found: %d", appId)));

                if (!canUserApproveApplication(appId, approver)) {
                    throw new BusinessException("User does not have permission to approve this application");
                }

                // Create decision DTO
                ApprovalDecisionDto decisionDto = builder()
                        .comments(request.getComments())
                        .approvalRole(approver.getRole().name())
                        .decision("APPROVE")
                        .sendNotification(request.getSendNotifications())
                        .build();

                // Approve the application
                approveApplication(appId, decisionDto, approver);
                successful.add(appId);

                log.debug("Successfully approved application {} in bulk", appId);

            } catch (Exception e) {
                errors.add(BulkApprovalResult.BulkApprovalError.builder()
                        .applicationId(appId)
                        .errorMessage(e.getMessage())
                        .errorCode("BULK_APPROVAL_ERROR")
                        .build());
                log.error("Error bulk approving application {}: {}", appId, e.getMessage());
            }
        });

        BulkApprovalResult result = BulkApprovalResult.builder()
                .totalProcessed(request.getApplicationIds().size())
                .successfulCount(successful.size())
                .failedCount(errors.size())
                .successfulApplicationIds(successful)
                .errors(errors)
                .processedAt(LocalDateTime.now())
                .build();

        log.info("Bulk approval completed: {} successful, {} failed", successful.size(), errors.size());

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public ApprovalAnalyticsDto getApprovalAnalytics(String period, Long branchId, Long approverId,
                                                     String productType) {
        log.debug("Getting approval analytics for period: {}, branch: {}", period, branchId);

        // Determine date range
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = calculateAnalyticsStartDate(period, endDate);

        // Get analytics data
        return calculateApprovalAnalytics(startDate, endDate, branchId, approverId, productType);
    }

    @Transactional(readOnly = true)
    @Override
    public byte[] exportApprovals(String format, LocalDate startDate, LocalDate endDate,
                                  String status, Long branchId, Long approverId) {
        log.info("Exporting approvals in {} format for period {} to {}", format, startDate, endDate);

        // Get approvals for export
        List<ApplicationApproval> approvals = getApprovalsForExport(startDate, endDate, status, branchId, approverId);

        // Convert to requested format
        return convertToExportFormat(approvals, format);
    }

    @Transactional(readOnly = true)
    @Override
    public SLAStatusDto getSLAStatus(Long applicationId) {
        log.debug("Getting SLA status for application: {}", applicationId);

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Loan application not found with id: %d", applicationId)));

        if (application.getSubmittedDate() == null) {
            throw new BusinessException("Application has not been submitted yet");
        }

        return calculateSLAStatus(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalConditionDto> getApprovalConditions(Long applicationId) {
        log.debug("Getting approval conditions for application: {}", applicationId);

        List<ApprovalCondition> conditions = approvalConditionRepository
                .findByLoanApplicationId(applicationId);

        return conditions.stream()
                .map(this::convertToConditionDto)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public LoanApplicationDto addApprovalCondition(Long applicationId, ApprovalConditionDto conditionDto, User user) {
        log.info("Adding approval condition for application {} by user {}", applicationId, user.getUsername());

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Loan application not found with id: %d", applicationId)));

        // Validate user permission
        if (!canUserApproveApplication(applicationId, user)) {
            throw new BusinessException("User does not have permission to add approval conditions");
        }

        // Create condition
        ApprovalCondition condition = ApprovalCondition.builder()
                .loanApplication(application)
                .conditionType(conditionDto.getConditionType())
                .description(conditionDto.getDescription())
                .mandatory(conditionDto.getIsMandatory())
                .dueDate(conditionDto.getDueDate() != null ?
                        LocalDateTime.parse(conditionDto.getDueDate()) : null)
                .status(GeneralConfig.ConditionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        condition.setCreatedBy(user.getId());

        approvalConditionRepository.save(condition);

        // Clear workflow cache
        clearCaches(applicationId);

        log.info("Added approval condition for application {}: {}", applicationId, conditionDto.getConditionType());

        return enrichLoanApplicationDto(application);
    }

    @Override
    @Transactional
    public LoanApplicationDto completeApprovalCondition(Long applicationId, String conditionType, User user) {
        log.info("Completing approval condition for application {}: {}", applicationId, conditionType);

        ApprovalCondition condition = approvalConditionRepository
                .findByLoanApplicationIdAndConditionType(applicationId, conditionType)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Approval condition not found: %s", conditionType)));

        condition.setStatus(GeneralConfig.ConditionStatus.COMPLETED);
        condition.setCompletedDate(LocalDateTime.now());
        condition.setCompletedBy(user);
        condition.setUpdatedAt(LocalDateTime.now());

        approvalConditionRepository.save(condition);

        // Clear workflow cache
        clearCaches(applicationId);

        log.info("Completed approval condition for application {}: {}", applicationId, conditionType);

        LoanApplication application = condition.getLoanApplication();
        return enrichLoanApplicationDto(application);
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalSummaryDto getApprovalSummary(Long applicationId, User currentUser) {
        log.debug("Getting approval summary for application: {}", applicationId);

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Loan application not found with id: %d", applicationId)));

        List<ApplicationApproval> approvals = approvalRepository
                .findByLoanApplicationIdOrderByApprovalLevelAsc(applicationId);

        List<ApprovalCondition> conditions = approvalConditionRepository
                .findByLoanApplicationId(applicationId);

        return buildApprovalSummary(application, approvals, conditions, currentUser);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void validateApplicationForApproval(LoanApplication application, User approver) {
        if (!application.canBeApproved()) {
            throw new BusinessException(
                    String.format("Application is not in approvable state. Current status: %s",
                            application.getStatus()));
        }

        if (application.getSubmittedDate() == null) {
            throw new BusinessException("Application has not been submitted for approval");
        }

        // Check SLA status
        SLAStatusDto slaStatus = calculateSLAStatus(application);
        if (slaStatus.isBreached()) {
            log.warn("Approving application {} with breached SLA", application.getId());
        }
    }

    private void validateApplicationForRejection(LoanApplication application, User approver) {
        if (!application.canBeApproved()) {
            throw new BusinessException(
                    String.format("Application cannot be rejected. Current status: %s",
                            application.getStatus()));
        }
    }

    private void validateApplicationForReturn(LoanApplication application, User approver) {
        if (!application.canBeApproved()) {
            throw new BusinessException(
                    String.format("Application cannot be returned for revision. Current status: %s",
                            application.getStatus()));
        }
    }

    private ApplicationApproval createApprovalRecord(LoanApplication application, User approver,
                                                     ApprovalDecisionDto dto, GeneralConfig.ApprovalDecision decision) {

        String approvalRoleName=approver.getRole().name();
        // Determine what role this approval represents (not who approved it)
        int level = determineApprovalLevel(application, approver);
        BigDecimal amount = application.getAppliedAmount();
        // Always use the expected role for this level, not the approver's actual role
        String expectedRoleForLevel = workflowRules.getRoleForLevel(level, amount);
        log.info("Creating approval record - Level: {}, Expected Role: {}, Actual Approver: {} ({})",
                level, expectedRoleForLevel, approver.getUsername(), approver.getRole());

        return ApplicationApproval.builder()
                .loanApplication(application)
                .approver(approver)
                .decision(decision)
                .comments(dto.getComments())
                .approvalLevel(determineApprovalLevel(application, approver))
                .approvalRole(expectedRoleForLevel)  // Use expected role, not approver's role
                .decisionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .overrideLimits(String.valueOf(dto.getOverrideLimits()))
                .overrideReason(dto.getOverrideReason())
                .build();
    }


    private int determineApprovalLevel(LoanApplication application, User approver) {
        List<ApplicationApproval> existingApprovals = approvalRepository
                .findByLoanApplicationIdOrderByApprovalLevelAsc(application.getId());
        if (existingApprovals.isEmpty()) {
            log.info("No existing approvals for application {}, starting at level 1", application.getId());
            return 1;
        }
        // Find the highest level that has been APPROVED
        int maxApprovedLevel = existingApprovals.stream()
                .filter(a -> a.getDecision() == GeneralConfig.ApprovalDecision.APPROVED)
                .mapToInt(ApplicationApproval::getApprovalLevel)
                .max()
                .orElse(0);
        // The next level to approve is one more than the highest approved level
        int nextLevel = maxApprovedLevel + 1;
        log.info("Application {} - Max approved level: {}, Next level: {}",
                application.getId(), maxApprovedLevel, nextLevel);
        // Optional: Check for escalation (if needed)
        if (shouldEscalateApproval(application, approver, maxApprovedLevel)) {
            log.info("Escalation triggered for application {}, using level: {}",
                    application.getId(), nextLevel);
        }

        return nextLevel;
    }



    private boolean shouldEscalateApproval(LoanApplication application, User approver, int currentLevel) {
        BigDecimal amount = application.getAppliedAmount();
        System.out.println(">>>>>Am here at Approvals 5");

        // Amount-based escalation
        if (amount.compareTo(new BigDecimal("1000000")) > 0 && currentLevel < 3) {
            return true; // Need credit committee
        }

        System.out.println(">>>>>Am here at Approvals 6");

        // Risk-based escalation
        if (application.getRiskScore() != null && application.getRiskScore() > 70 && currentLevel < 3) {
            return true;
        }
        System.out.println(">>>>>Am here at Approvals 7");

        // ✅ FIXED VERSION - Get the product type name or code directly:
        String productTypeName = null;
        if (application.getLoanProduct() != null && application.getLoanProduct().getProductType() != null) {
            // Get a specific field, NOT the whole object
            productTypeName = application.getLoanProduct().getProductType().getName();
            // OR use: productTypeName = application.getLoanProduct().getProductType().getCode();
        }

        System.out.println(">>>>>Am here at Approvals 8 with productType: " + productTypeName);

        // Use productTypeName instead of productType
        if ("BUSINESS".equals(productTypeName) && amount.compareTo(new BigDecimal("500000")) > 0 && currentLevel < 2) {
            return true;
        }

        System.out.println(">>>>>Am here at Approvals 9");

        return false;
    }


    private void updateApplicationAfterApproval(LoanApplication application, User approver, ApprovalDecisionDto dto) {
        int currentLevel = determineApprovalLevel(application, approver);
        int totalLevels = getTotalApprovalLevels(application);

        // This is the level that was just approved
        int approvedLevel = currentLevel - 1;
        int nextLevel = currentLevel;  // The level just approved is now complete

        application.setCurrentApprovalLevel(String.valueOf(nextLevel));
        application.setLastApprovalDate(LocalDateTime.now());

        // Determine if this is the final approval
        boolean isFinal = nextLevel >= totalLevels;

        if (isFinal) {
            application.setStatus(GeneralConfig.LoanApplicationStatus.APPROVED);
            application.setStage(GeneralConfig.ApplicationStage.DISBURSEMENT);
            application.setNextApprovalRole("COMPLETED");
        } else {
            if (nextLevel >= totalLevels - 1) {
                application.setStatus(GeneralConfig.LoanApplicationStatus.PENDING_FINAL_APPROVAL);
            } else {
                application.setStatus(GeneralConfig.LoanApplicationStatus.PENDING_APPROVAL);
            }
            application.setStage(GeneralConfig.ApplicationStage.APPROVAL);
            application.setNextApprovalRole(getNextApprovalRole(application, approvedLevel));
        }

        log.info("Application {} - Approved level {}/{}, next role: {}",
                application.getId(), nextLevel, totalLevels, application.getNextApprovalRole());
    }



    private void handleFinalApproval(LoanApplication application, User approver, ApprovalDecisionDto dto) {
        // Update application status
        application.setStatus(GeneralConfig.LoanApplicationStatus.APPROVED);
        application.setStage(GeneralConfig.ApplicationStage.DISBURSEMENT);
        application.setApprovedDate(LocalDateTime.now());
        application.setApprovedBy(approver.getUsername());

        // Create loan record
        System.out.println(">>>Handle at 1-----"+application);
        createLoanFromApplication(application, approver);

        System.out.println(">>>Handle at 2-----");
        // Mark all conditions as completed
        completeAllConditions(application.getId());
    }

    private void handleIntermediateApproval(LoanApplication application, User approver) {
        int currentLevel = Integer.parseInt(application.getCurrentApprovalLevel());
        int nextLevel = currentLevel + 1;
        int totalLevels = getTotalApprovalLevels(application);
        // Update status based on next level
        if (nextLevel >= totalLevels) {
            application.setStatus(GeneralConfig.LoanApplicationStatus.PENDING_FINAL_APPROVAL);
        } else {
            application.setStatus(GeneralConfig.LoanApplicationStatus.PENDING_APPROVAL);
        }
        application.setStage(GeneralConfig.ApplicationStage.APPROVAL);
        application.setCurrentApprovalLevel(String.valueOf(nextLevel));
        application.setNextApprovalRole(getNextApprovalRole(application, currentLevel));
        application.setLastApprovalDate(LocalDateTime.now());

        log.info("Application moved to approval level {}/{}, next role: {}",
                nextLevel, totalLevels, application.getNextApprovalRole());
    }


    private void handleRejectionWorkflow(LoanApplication application, ApprovalDecisionDto dto) {
        // Log rejection analytics
        logRejectionAnalytics(application, dto);

        // Update related entities
        updateRelatedEntitiesOnRejection(application);
    }

    private void createRevisionConditions(LoanApplication application, List<String> requirements, User user) {
        for (String requirement : requirements) {
            ApprovalCondition condition = ApprovalCondition.builder()
                    .loanApplication(application)
                    .conditionType("REVISION_REQUIREMENT")
                    .description(requirement)
                    .mandatory(true)
                    .dueDate(LocalDateTime.now().plusDays(7)) // 7 days to complete
                    .status(GeneralConfig.ConditionStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            condition.setCreatedBy(user.getId());

            approvalConditionRepository.save(condition);
        }
    }


    private Loan createLoanFromApplication(LoanApplication application, User approver) {
        Loan loan = Loan.builder()
                .loanApplication(application)
                .borrower(application.getBorrower())
                .loanAccountNumber(generateLoanAccountNumber())
                .principalAmount(application.getAppliedAmount())
                .interestRate(application.getLoanProduct().getInterestRate())
                .tenureMonths(application.getTenureMonths())
                .status(GeneralConfig.LoanStatus.PENDING_DISBURSEMENT)
                .createdAt(LocalDateTime.now())
                .build();
        loan.setCreatedBy(approver.getId());
        loan.calculateLoanTotals();
        loan.setTotalPaid(BigDecimal.ZERO);
        // Generate repayment schedule using centralized service
        List<RepaymentSchedule> schedules = scheduleGenerationService.generateOrUpdateRepaymentSchedule(loan, approver);
        loan.setRepaymentSchedules(schedules);

        return loanRepository.save(loan);
    }


    private String generateLoanAccountNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.format("%04d", new Random().nextInt(10000));
        return "LN" + timestamp.substring(timestamp.length() - 8) + random;
    }

    private List<RepaymentSchedule> generateRepaymentSchedule(Loan loan) {
        List<RepaymentSchedule> schedules = new ArrayList<>();
        BigDecimal monthlyInterestRate = loan.getInterestRate()
                .divide(new BigDecimal("1200"), 6, RoundingMode.HALF_UP);

        BigDecimal remainingPrincipal = loan.getPrincipalAmount();

        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .dueDate(LocalDate.from(LocalDateTime.now().plusMonths(i)))
                    .principalAmount(calculatePrincipalForMonth(remainingPrincipal, monthlyInterestRate,
                            loan.getTenureMonths(), i))
                    .interestAmount(calculateInterestForMonth(remainingPrincipal, monthlyInterestRate))
                    .status(GeneralConfig.InstallmentStatus.PENDING)
                    .build();
            schedule.setPenaltyAmount(BigDecimal.valueOf(0.0));
           schedule.setInterestDue(schedule.getInterestAmount());
           schedule.setInterestPaid(BigDecimal.valueOf(0.0));
           schedule.setPrincipalDue(schedule.getPrincipalAmount());
           schedule.setPrincipalPaid(BigDecimal.valueOf(0.0));
           schedule.setTotalPaid(BigDecimal.valueOf(0.0));
           schedule.setTotalDue(schedule.getPenaltyAmount().add(schedule.getInterestAmount()));

            schedules.add(schedule);

            // Update remaining principal
            remainingPrincipal = remainingPrincipal.subtract(schedule.getPrincipalAmount());
        }

        return schedules;
    }

    private BigDecimal calculatePrincipalForMonth(BigDecimal remainingPrincipal, BigDecimal monthlyInterestRate,
                                                  int totalMonths, int currentMonth) {
        // Simplified calculation - in production use proper amortization formula
        return remainingPrincipal.divide(new BigDecimal(totalMonths - currentMonth + 1), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateInterestForMonth(BigDecimal remainingPrincipal, BigDecimal monthlyInterestRate) {
        return remainingPrincipal.multiply(monthlyInterestRate).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean validateUserCanApprove(LoanApplication application, User user) {
        boolean isSuperAdmin = user.getRole() == User.UserRole.SUPER_ADMIN;

        // 1. Check role
        if (!hasApprovalRole(user.getRole())) {
            return false;
        }

        // 2. Check branch access
        if (!hasBranchAccess(application, user) && !isSuperAdmin) {
            return false;
        }

        // 3. Check amount limits
        if (!hasAmountAuthority(application, user)) {
            return false;
        }

        // 4. Check approval level
        if (!isCorrectApprovalLevel(application, user)) {
            return false;
        }

        // 5. Check conditions
        if (!areConditionsMet(application)) {
            return false;
        }

        // 6. Check if already approved by this user
        if (hasAlreadyApproved(application, user) && !isSuperAdmin) {
            return false;
        }

        return true;
    }

    private boolean hasApprovalRole(User.UserRole role) {
        return role == User.UserRole.BRANCH_MANAGER ||
                role == User.UserRole.CREDIT_APPROVER ||
                role == User.UserRole.SUPER_ADMIN ||
                role == User.UserRole.REGIONAL_MANAGER;
    }

    private boolean hasBranchAccess(LoanApplication application, User user) {
        if (user.getBranchId() == null || application.getBranch() == null) {
            return true; // Allow if no branch restriction
        }
        return user.getBranchId().equals(application.getBranch().getId());
    }

    private boolean hasAmountAuthority(LoanApplication application, User user) {
        BigDecimal amount = application.getAppliedAmount();
     return workflowRules.hasAmountAuthority(application, user);
    }



    private boolean isCorrectApprovalLevel(LoanApplication application, User user) {
        int neededLevel = determineCurrentApprovalLevel(application);

        switch (user.getRole()) {
            case BRANCH_MANAGER:
                return neededLevel == 1;
            case CREDIT_APPROVER:
                return neededLevel == 2;
            case REGIONAL_MANAGER:
                return neededLevel == 3;
            case SUPER_ADMIN:
                return true;
            default:
                return false;
        }
    }

    private int determineCurrentApprovalLevel(LoanApplication application) {
        List<ApplicationApproval> approvals = approvalRepository
                .findByLoanApplicationIdOrderByApprovalLevelAsc(application.getId());

        if (approvals.isEmpty()) {
            return 1;
        }

        // Find the highest completed approval level
        int maxCompletedLevel = approvals.stream()
                .filter(a -> a.getDecision() == GeneralConfig.ApprovalDecision.APPROVED)
                .mapToInt(ApplicationApproval::getApprovalLevel)
                .max()
                .orElse(0);

        return maxCompletedLevel + 1;
    }

    private boolean areConditionsMet(LoanApplication application) {
        List<ApprovalCondition> conditions = approvalConditionRepository
                .findByLoanApplicationId(application.getId());

        return conditions.stream()
                .filter(ApprovalCondition::getMandatory)
                .allMatch(c -> c.getStatus() == GeneralConfig.ConditionStatus.COMPLETED);
    }

    private boolean hasAlreadyApproved(LoanApplication application, User user) {
        return approvalRepository.existsByLoanApplicationIdAndApproverId(application.getId(), user.getId());
    }

    private ApprovalWorkflowDto buildApprovalWorkflow(LoanApplication application,
                                                      List<ApplicationApproval> approvals,
                                                      List<ApprovalCondition> conditions,
                                                      User currentUser) {
        List<ApprovalWorkflowStepDto> workflowSteps = buildWorkflowSteps(application, approvals, conditions);

        int completedSteps = (int) workflowSteps.stream()
                .filter(step -> "COMPLETED".equals(step.getStatus()) ||
                        "APPROVED".equals(step.getStatus()) ||
                        "REJECTED".equals(step.getStatus()))
                .count();

        String currentStage = determineCurrentStage(workflowSteps);
        String nextApprovalRole = determineNextApprovalRole(application, approvals);
        boolean canCurrentUserApprove = canUserApproveApplication(application.getId(), currentUser);

        return ApprovalWorkflowDto.builder()
                .applicationId(application.getId())
                .applicationNumber(application.getApplicationNumber())
                .currentStatus(application.getStatus().name())
                .currentStage(currentStage)
                .workflowSteps(workflowSteps)
                .totalSteps(workflowSteps.size())
                .completedSteps(completedSteps)
                .nextApprovalRole(nextApprovalRole)
                .canCurrentUserApprove(canCurrentUserApprove)
                .currentUserRole(currentUser.getRole().name())
                .build();
    }


    private List<ApprovalWorkflowStepDto> buildWorkflowSteps(LoanApplication application,
                                                             List<ApplicationApproval> approvals,
                                                             List<ApprovalCondition> conditions) {
        return workflowRules.buildWorkflowSteps(application, approvals, conditions);
    }


    private boolean determineIfCurrentStep(int stepNumber, List<ApplicationApproval> approvals) {
        // Find highest completed step
        int highestCompleted = approvals.stream()
                .filter(a -> a.getDecision() == GeneralConfig.ApprovalDecision.APPROVED)
                .mapToInt(ApplicationApproval::getApprovalLevel)
                .max()
                .orElse(0);

        // Current step is the next one after highest completed
        return stepNumber == highestCompleted + 1;
    }

    private void setSLAInfo(ApprovalWorkflowStepDto step, LoanApplication application) {
        if (application.getSubmittedDate() == null) {
            return;
        }

        // Each step gets 2 days for completion
        LocalDateTime stepDueDate = application.getSubmittedDate()
                .plusDays(2L * step.getStepNumber());

        step.setSlaDeadline(stepDueDate.toEpochSecond(java.time.ZoneOffset.UTC));
        step.setIsOverdue(LocalDateTime.now().isAfter(stepDueDate) &&
                !step.getIsCompleted() &&
                !"APPROVED".equals(step.getStatus()));
    }

    private String determineCurrentStage(List<ApprovalWorkflowStepDto> steps) {
        return steps.stream()
                .filter(step -> step.getIsCurrentStep() ||
                        ("PENDING".equals(step.getStatus()) && !step.getIsCompleted()))
                .findFirst()
                .map(step -> {
                    if (step.getIsCurrentStep()) {
                        return "In " + step.getStepName();
                    } else {
                        return "Waiting for " + step.getStepName();
                    }
                })
                .orElse("Completed");
    }


    private String determineNextApprovalRole(LoanApplication application, List<ApplicationApproval> approvals) {
        return workflowRules.getNextApprovalRoleFromApprovals(application, approvals);
    }

    private ApprovalPerformanceDto calculatePerformanceMetrics(Long approverId,
                                                               List<ApplicationApproval> approvals,
                                                               LocalDate startDate, LocalDate endDate) {
        long totalReviewed = approvals.size();
        long approvedCount = approvals.stream()
                .filter(a -> a.getDecision() == GeneralConfig.ApprovalDecision.APPROVED)
                .count();
        long rejectedCount = approvals.stream()
                .filter(a -> a.getDecision() == GeneralConfig.ApprovalDecision.REJECTED)
                .count();
        long returnedCount = approvals.stream()
                .filter(a -> a.getDecision() == GeneralConfig.ApprovalDecision.RETURNED_FOR_REVISION)
                .count();

        // Calculate average processing time
        double avgProcessingTime = approvals.stream()
                .filter(a -> a.getDecisionDate() != null && a.getCreatedAt() != null)
                .mapToLong(a -> ChronoUnit.HOURS.between(a.getCreatedAt(), a.getDecisionDate()))
                .average()
                .orElse(0.0);

        // Calculate SLA compliance
        double slaComplianceRate = calculateSLAComplianceRate(approvals);

        // Calculate monthly performance
        List<ApprovalPerformanceDto.MonthlyPerformance> monthlyPerformance =
                calculateMonthlyPerformance(approvals, startDate, endDate);

        User approver = userService.getUserById(approverId);
        String approverName = approver != null ?
                approver.getFirstName() + " " + approver.getLastName() : "Unknown";

        return ApprovalPerformanceDto.builder()
                .approverId(approverId)
                .approverName(approverName)
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalDecisions(totalReviewed)
                .approvedCount(approvedCount)
                .rejectedCount(rejectedCount)
                .returnedCount(returnedCount)
                .approvalRate(totalReviewed > 0 ? (double) approvedCount / totalReviewed * 100 : 0)
                .rejectionRate(totalReviewed > 0 ? (double) rejectedCount / totalReviewed * 100 : 0)
                .avgProcessingTimeHours(avgProcessingTime)
                .onTimeApprovalRate(slaComplianceRate)
                .slaBreaches(countSLABreaches(approvals))
                .satisfactionScore(calculateSatisfactionScore(approvals))
                .monthlyPerformance(monthlyPerformance)
                .build();
    }

    private double calculateSLAComplianceRate(List<ApplicationApproval> approvals) {
        if (approvals.isEmpty()) return 0.0;

        long compliantCount = approvals.stream()
                .filter(approval -> {
                    if (approval.getDecisionDate() == null || approval.getCreatedAt() == null) {
                        return false;
                    }
                    long hours = ChronoUnit.HOURS.between(
                            approval.getCreatedAt(), approval.getDecisionDate());
                    return hours <= APPROVAL_SLA_HOURS;
                })
                .count();

        return (double) compliantCount / approvals.size() * 100;
    }

    private List<ApprovalPerformanceDto.MonthlyPerformance> calculateMonthlyPerformance(
            List<ApplicationApproval> approvals, LocalDate startDate, LocalDate endDate) {

        Map<String, List<ApplicationApproval>> approvalsByMonth = approvals.stream()
                .collect(Collectors.groupingBy(a -> {
                    LocalDateTime date = a.getDecisionDate() != null ? a.getDecisionDate() : a.getCreatedAt();
                    return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                }));

        return approvalsByMonth.entrySet().stream()
                .map(entry -> {
                    List<ApplicationApproval> monthlyApprovals = entry.getValue();
                    long approvedCount = monthlyApprovals.stream()
                            .filter(a -> a.getDecision() == GeneralConfig.ApprovalDecision.APPROVED)
                            .count();
                    long rejectedCount = monthlyApprovals.stream()
                            .filter(a -> a.getDecision() == GeneralConfig.ApprovalDecision.REJECTED)
                            .count();
                    long returnedCount = monthlyApprovals.stream()
                            .filter(a -> a.getDecision() == GeneralConfig.ApprovalDecision.RETURNED_FOR_REVISION)
                            .count();

                    double avgProcessingTime = monthlyApprovals.stream()
                            .filter(a -> a.getDecisionDate() != null && a.getCreatedAt() != null)
                            .mapToLong(a -> ChronoUnit.HOURS.between(a.getCreatedAt(), a.getDecisionDate()))
                            .average()
                            .orElse(0.0);

                    return ApprovalPerformanceDto.MonthlyPerformance.builder()
                            .month(entry.getKey())
                            .approvedCount(approvedCount)
                            .rejectedCount(rejectedCount)
                            .returnedCount(returnedCount)
                            .avgProcessingTime(avgProcessingTime)
                            .totalDecisions((long) monthlyApprovals.size())
                            .build();
                })
                .sorted(Comparator.comparing(ApprovalPerformanceDto.MonthlyPerformance::getMonth))
                .collect(Collectors.toList());
    }

    private int countSLABreaches(List<ApplicationApproval> approvals) {
        return (int) approvals.stream()
                .filter(approval -> {
                    if (approval.getDecisionDate() == null || approval.getCreatedAt() == null) {
                        return false;
                    }
                    long hours = ChronoUnit.HOURS.between(
                            approval.getCreatedAt(), approval.getDecisionDate());
                    return hours > APPROVAL_SLA_HOURS;
                })
                .count();
    }

    private double calculateSatisfactionScore(List<ApplicationApproval> approvals) {
        // Simplified calculation - in production, this would use actual satisfaction data
        long approvedCount = approvals.stream()
                .filter(a -> a.getDecision() == GeneralConfig.ApprovalDecision.APPROVED)
                .count();

        if (approvals.isEmpty()) return 0.0;

        // Base score on approval rate with SLA compliance bonus
        double approvalRate = (double) approvedCount / approvals.size() * 100;
        double slaComplianceRate = calculateSLAComplianceRate(approvals);

        return (approvalRate * 0.7) + (slaComplianceRate * 0.3);
    }

    private ApprovalStatsDto calculateApprovalStatistics(User user, Long branchId,
                                                         LocalDate startDate, LocalDate endDate) {
        // Count pending approvals
        long pendingCount = loanApplicationRepository.countPendingForUser(
                user.getId(), user.getBranchId(), startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        // Count today's statistics
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        long approvedToday = approvalRepository.countByApproverAndDecisionBetweenDates(
                user.getId(), GeneralConfig.ApprovalDecision.APPROVED, todayStart, todayEnd);
        long rejectedToday = approvalRepository.countByApproverAndDecisionBetweenDates(
                user.getId(), GeneralConfig.ApprovalDecision.REJECTED, todayStart, todayEnd);
        long returnedToday = approvalRepository.countByApproverAndDecisionBetweenDates(
                user.getId(), GeneralConfig.ApprovalDecision.RETURNED_FOR_REVISION, todayStart, todayEnd);


        // Calculate average processing time
        List<ApplicationApproval> recentApprovals = approvalRepository
                .findByApproverIdAndDecisionDateBetween(
                        user.getId(),
                        startDate.atStartOfDay(),
                        endDate.plusDays(1).atStartOfDay()
                );

        double avgProcessingTime = recentApprovals.stream()
                .filter(a -> a.getDecisionDate() != null && a.getCreatedAt() != null)
                .mapToLong(a -> ChronoUnit.HOURS.between(a.getCreatedAt(), a.getDecisionDate()))
                .average()
                .orElse(0.0);

        // Calculate on-time completion rate
        double onTimeRate = calculateSLAComplianceRate(recentApprovals);

        return ApprovalStatsDto.builder()
                .pending(pendingCount)
                .approvedToday(approvedToday)
                .rejectedToday(rejectedToday)
                .returnedToday(returnedToday)
                .avgProcessingTime(Math.round(avgProcessingTime))
                .totalProcessed((long) recentApprovals.size())
                .onTimeCompletionRate(onTimeRate)
                .reportDate(LocalDate.now())
                .approverId(user.getId())
                .build();
    }

    private LocalDate[] calculateDateRange(LocalDate startDate, LocalDate endDate, String period) {
        if (startDate != null && endDate != null) {
            return new LocalDate[]{startDate, endDate};
        }

        if ("today".equals(period)) {
            LocalDate today = LocalDate.now();
            return new LocalDate[]{today, today};
        } else if ("week".equals(period)) {
            return new LocalDate[]{LocalDate.now().minusDays(7), LocalDate.now()};
        } else if ("month".equals(period)) {
            return new LocalDate[]{LocalDate.now().minusDays(30), LocalDate.now()};
        } else if ("quarter".equals(period)) {
            return new LocalDate[]{LocalDate.now().minusMonths(3), LocalDate.now()};
        } else if ("year".equals(period)) {
            return new LocalDate[]{LocalDate.now().minusYears(1), LocalDate.now()};
        } else {
            // Default to last 30 days
            return new LocalDate[]{LocalDate.now().minusDays(30), LocalDate.now()};
        }
    }


    private ApprovalAnalyticsDto calculateApprovalAnalytics(LocalDate startDate, LocalDate endDate,
                                                            Long branchId, Long approverId, String productType) {
        log.debug("Calculating approval analytics for period {} to {}, branch: {}, approver: {}, product: {}",
                startDate, endDate, branchId, approverId, productType);

        // Get approvals for analytics
        List<ApplicationApproval> approvals = getApprovalsForAnalytics(
                startDate, endDate, branchId, approverId, productType);

        // Calculate basic metrics
        long totalApplications = getTotalApplications(startDate, endDate, branchId, approverId, productType);
        long totalApproved = countApprovedApplications(approvals);
        long totalRejected = countRejectedApplications(approvals);
        long totalReturned = countReturnedApplications(approvals);
        double overallApprovalRate = calculateApprovalRate(totalApproved, totalApplications);
        double avgProcessingTime = calculateAvgProcessingTime(approvals);
        double overallRejectionRate = calculateApprovalRate(totalRejected, totalApplications);
        double overallReturnRate = calculateApprovalRate(totalReturned, totalApplications);

        // Calculate trends
        List<ApprovalAnalyticsDto.ApprovalTrend> trends = calculateApprovalTrends(
                startDate, endDate, branchId, approverId, productType);

        // Calculate product statistics
        List<ApprovalAnalyticsDto.ProductApprovalStats> productStats = calculateProductStats(
                startDate, endDate, branchId);

        // Calculate branch statistics
        List<ApprovalAnalyticsDto.BranchApprovalStats> branchStats = calculateBranchStats(
                startDate, endDate);

        // Calculate top approvers
        List<ApprovalAnalyticsDto.ApproverPerformance> topApprovers = calculateTopApprovers(
                startDate, endDate, branchId);

        // Calculate additional metrics
        double slaComplianceRate = calculateSLAComplianceRate(approvals);
        int slaBreaches = countSLABreaches(approvals);
        //double avgSatisfactionScore = calculateAverageSatisfactionScore(approvals);

        // Calculate amount metrics
        AmountMetrics amountMetrics = calculateAmountMetrics(approvals);

        ApprovalAnalyticsDto analyticsDto = ApprovalAnalyticsDto.builder()
                .reportDate(LocalDate.now())
                .periodType(determinePeriodType(startDate, endDate))
                .totalApplications(totalApplications)
                .totalApproved(totalApproved)
                .totalRejected(totalRejected)
                .totalReturned(totalReturned)
                .overallApprovalRate(overallApprovalRate)
                .overallRejectionRate(overallRejectionRate)
                .overallReturnRate(overallReturnRate)
                .avgProcessingTime(avgProcessingTime)
                .trends(trends)
                .productStats(productStats)
                .branchStats(branchStats)
                .topApprovers(topApprovers)
                .slaComplianceRate(slaComplianceRate)
                .slaBreaches(slaBreaches)
               // .avgSatisfactionScore(avgSatisfactionScore)
                .totalApprovedAmount(amountMetrics.getTotalApprovedAmount())
                .averageApprovedAmount(amountMetrics.getAverageApprovedAmount())
                .largestApprovedAmount(amountMetrics.getLargestApprovedAmount())
                .smallestApprovedAmount(amountMetrics.getSmallestApprovedAmount())
                .build();

        // Calculate derived metrics
        analyticsDto.calculateDerivedMetrics();

        log.debug("Analytics calculation completed. Total applications: {}, Approved: {}",
                totalApplications, totalApproved);

        return analyticsDto;
    }


    private String determinePeriodType(LocalDate startDate, LocalDate endDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);

        if (daysBetween == 0) return "DAY";
        if (daysBetween <= 7) return "WEEK";
        if (daysBetween <= 31) return "MONTH";
        if (daysBetween <= 93) return "QUARTER";
        if (daysBetween <= 365) return "YEAR";
        return "CUSTOM";
    }

    private LocalDate calculateAnalyticsStartDate(String period, LocalDate endDate) {
        if ("day".equals(period)) {
            return endDate;
        } else if ("week".equals(period)) {
            return endDate.minusDays(7);
        } else if ("month".equals(period)) {
            return endDate.minusMonths(1);
        } else if ("quarter".equals(period)) {
            return endDate.minusMonths(3);
        } else if ("year".equals(period)) {
            return endDate.minusYears(1);
        } else {
            return endDate.minusMonths(1);
        }
    }

    private List<ApplicationApproval> getApprovalsForAnalytics(LocalDate startDate, LocalDate endDate,
                                                               Long branchId, Long approverId, String productType) {
        // Implementation for getting approvals with filters
        // This would typically use a custom repository method
        return Collections.emptyList(); // Placeholder
    }

    private long getTotalApplications(LocalDate startDate, LocalDate endDate,
                                      Long branchId, Long approverId, String productType) {
        // Implementation for counting applications
        return 0L; // Placeholder
    }

    private double calculateApprovalRate(long approved, long total) {
        return total > 0 ? (double) approved / total * 100 : 0.0;
    }

    private List<ApprovalAnalyticsDto.ApprovalTrend> calculateApprovalTrends(LocalDate startDate, LocalDate endDate,
                                                                             Long branchId, Long approverId,
                                                                             String productType) {
        // Implementation for calculating trends
        return Collections.emptyList(); // Placeholder
    }

    private List<ApprovalAnalyticsDto.ProductApprovalStats> calculateProductStats(
            LocalDate startDate, LocalDate endDate, Long branchId) {

        log.debug("Calculating product approval statistics for period {} to {}, branch: {}",
                startDate, endDate, branchId);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Get product statistics from repository
        List<Object[]> productStatsData = loanProductRepository.findProductApprovalStats(
                startDateTime, endDateTime, branchId);

        if (productStatsData == null || productStatsData.isEmpty()) {
            log.debug("No product statistics found for the given period");
            return Collections.emptyList();
        }

        List<ApprovalAnalyticsDto.ProductApprovalStats> productStats = new ArrayList<>();

        for (Object[] data : productStatsData) {
            try {
                Long totalApplications = data[3] != null ? ((Number) data[3]).longValue() : 0L;
                Long approvedCount = data[4] != null ? ((Number) data[4]).longValue() : 0L;
                Double approvalRate = totalApplications > 0 ?
                        (double) approvedCount / totalApplications * 100 : 0.0;

                ApprovalAnalyticsDto.ProductApprovalStats stats = ApprovalAnalyticsDto.ProductApprovalStats.builder()
                        .productType(data[0] != null ? (String) data[0] : "UNKNOWN")
                        .productName(data[1] != null ? (String) data[1] : "Unknown Product")
                        .productCode(data[2] != null ? (String) data[2] : "N/A")
                        .totalApplications(totalApplications)
                        .approvedCount(approvedCount)
                        .rejectedCount(data[5] != null ? ((Number) data[5]).longValue() : 0L)
                        .approvalRate(approvalRate)
                        .avgProcessingTime(data[6] != null ? ((Number) data[6]).doubleValue() : 0.0)
                        .avgApprovedAmount(data[7] != null ? ((Number) data[7]).doubleValue() : 0.0)
                        .totalApprovedAmount(data[8] != null ? ((Number) data[8]).doubleValue() : 0.0)
                        .riskScore(data[9] != null ? ((Number) data[9]).intValue() : 0)
                        .riskLevel(determineRiskLevel(data[9] != null ? ((Number) data[9]).intValue() : 0))
                        .build();

                productStats.add(stats);

            } catch (Exception e) {
                log.error("Error processing product statistics data: {}", e.getMessage(), e);
            }
        }

        // Sort by total applications (descending)
        productStats.sort((a, b) -> b.getTotalApplications().compareTo(a.getTotalApplications()));

        log.debug("Calculated statistics for {} products", productStats.size());
        return productStats;
    }

    private List<ApprovalAnalyticsDto.BranchApprovalStats> calculateBranchStats(
            LocalDate startDate, LocalDate endDate) {

        log.debug("Calculating branch approval statistics for period {} to {}", startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Get branch statistics from repository
        List<Object[]> branchStatsData = branchRepository.findBranchApprovalStats(
                startDateTime, endDateTime);

        if (branchStatsData == null || branchStatsData.isEmpty()) {
            log.debug("No branch statistics found for the given period");
            return Collections.emptyList();
        }

        List<ApprovalAnalyticsDto.BranchApprovalStats> branchStats = new ArrayList<>();

        for (Object[] data : branchStatsData) {
            try {
                Long totalApplications = data[4] != null ? ((Number) data[4]).longValue() : 0L;
                Long approvedCount = data[5] != null ? ((Number) data[5]).longValue() : 0L;
                Double approvalRate = totalApplications > 0 ?
                        (double) approvedCount / totalApplications * 100 : 0.0;

                Long slaBreaches = data[10] != null ? ((Number) data[10]).longValue() : 0L;
                Double slaComplianceRate = totalApplications > 0 ?
                        (double) (totalApplications - slaBreaches) / totalApplications * 100 : 100.0;

                ApprovalAnalyticsDto.BranchApprovalStats stats = ApprovalAnalyticsDto.BranchApprovalStats.builder()
                        .branchId(data[0] != null ? ((Number) data[0]).longValue() : null)
                        .branchName(data[1] != null ? (String) data[1] : "Unknown Branch")
                        .branchCode(data[2] != null ? (String) data[2] : "N/A")
                        .region(data[3] != null ? (String) data[3] : "Unknown Region")
                        .totalApplications(totalApplications)
                        .approvedCount(approvedCount)
                        .rejectedCount(data[6] != null ? ((Number) data[6]).longValue() : 0L)
                        .approvalRate(approvalRate)
                        .avgProcessingTime(data[7] != null ? ((Number) data[7]).doubleValue() : 0.0)
                        .totalApprovedAmount(data[8] != null ? ((Number) data[8]).doubleValue() : 0.0)
                        .slaBreaches(slaBreaches.intValue())
                        .slaComplianceRate(slaComplianceRate)
                        .build();

                branchStats.add(stats);

            } catch (Exception e) {
                log.error("Error processing branch statistics data: {}", e.getMessage(), e);
            }
        }

        // Sort by approval rate (descending)
        branchStats.sort((a, b) -> b.getApprovalRate().compareTo(a.getApprovalRate()));

        log.debug("Calculated statistics for {} branches", branchStats.size());
        return branchStats;
    }


    private List<ApprovalAnalyticsDto.ApproverPerformance> calculateTopApprovers(
            LocalDate startDate, LocalDate endDate, Long branchId) {

        log.debug("Calculating top approvers for period {} to {}, branch: {}",
                startDate, endDate, branchId);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Get approver performance data from repository
        List<Object[]> approverPerformanceData = approvalRepository.findApproverPerformance(
                startDateTime, endDateTime, branchId, 10); // Top 10

        if (approverPerformanceData == null || approverPerformanceData.isEmpty()) {
            log.debug("No approver performance data found for the given period");
            return Collections.emptyList();
        }

        List<ApprovalAnalyticsDto.ApproverPerformance> topApprovers = new ArrayList<>();
        int rank = 1;

        for (Object[] data : approverPerformanceData) {
            try {
                Long totalDecisions = data[4] != null ? ((Number) data[4]).longValue() : 0L;
                Long approvedCount = data[5] != null ? ((Number) data[5]).longValue() : 0L;
                Double approvalRate = totalDecisions > 0 ?
                        (double) approvedCount / totalDecisions * 100 : 0.0;

                Long slaBreaches = data[11] != null ? ((Number) data[11]).longValue() : 0L;
                Double slaComplianceRate = totalDecisions > 0 ?
                        (double) (totalDecisions - slaBreaches) / totalDecisions * 100 : 100.0;

                ApprovalAnalyticsDto.ApproverPerformance performance = ApprovalAnalyticsDto.ApproverPerformance.builder()
                        .approverId(data[0] != null ? ((Number) data[0]).longValue() : null)
                        .approverName(data[1] != null ? (String) data[1] : "Unknown Approver")
                        .approverRole(data[2] != null ? (String) data[2] : "UNKNOWN_ROLE")
                        .approverBranch(data[3] != null ? (String) data[3] : "Unknown Branch")
                        .totalDecisions(totalDecisions)
                        .approvedCount(approvedCount)
                        .rejectedCount(data[6] != null ? ((Number) data[6]).longValue() : 0L)
                        .returnedCount(data[7] != null ? ((Number) data[7]).longValue() : 0L)
                        .approvalRate(approvalRate)
                        .avgProcessingTime(data[8] != null ? ((Number) data[8]).doubleValue() : 0.0)
                        .slaComplianceRate(slaComplianceRate)
                        .slaBreaches(slaBreaches.intValue())
                       // .satisfactionScore(data[9] != null ? ((Number) data[9]).doubleValue() : 0.0)
                        .totalApprovedAmount(data[10] != null ? ((Number) data[10]).doubleValue() : 0.0)
                        .escalations(data[12] != null ? ((Number) data[12]).intValue() : 0)
                        .rank(rank++)
                        .performanceCategory(determinePerformanceCategory(approvalRate,
                                data[8] != null ? ((Number) data[8]).doubleValue() : 0.0,
                                slaComplianceRate))
                        .build();

                topApprovers.add(performance);

            } catch (Exception e) {
                log.error("Error processing approver performance data: {}", e.getMessage(), e);
            }
        }

        log.debug("Calculated performance for {} approvers", topApprovers.size());
        return topApprovers;
    }


    private String determinePerformanceCategory(double approvalRate, double avgProcessingTime, double slaCompliance) {
        if (approvalRate >= 85 && avgProcessingTime <= 24 && slaCompliance >= 95) {
            return "EXCELLENT";
        } else if (approvalRate >= 75 && avgProcessingTime <= 48 && slaCompliance >= 85) {
            return "GOOD";
        } else if (approvalRate >= 60 && avgProcessingTime <= 72 && slaCompliance >= 70) {
            return "AVERAGE";
        } else {
            return "NEEDS_IMPROVEMENT";
        }
    }


    private String determineRiskLevel(int riskScore) {
        if (riskScore >= 80) {
            return "HIGH";
        } else if (riskScore >= 60) {
            return "MEDIUM";
        } else if (riskScore >= 40) {
            return "LOW";
        } else {
            return "VERY_LOW";
        }
    }



    private List<ApplicationApproval> getApprovalsForExport(LocalDate startDate, LocalDate endDate,
                                                            String status, Long branchId, Long approverId) {
        // Implementation for getting approvals for export
        return Collections.emptyList(); // Placeholder
    }

    private byte[] convertToExportFormat(List<ApplicationApproval> approvals, String format) {
        if ("csv".equalsIgnoreCase(format)) {
            return convertToCSV(approvals);
        } else if ("excel".equalsIgnoreCase(format)) {
            return convertToExcel(approvals);
        } else {
            throw new BusinessException("Unsupported export format: " + format);
        }
    }

    private byte[] convertToCSV(List<ApplicationApproval> approvals) {
        try (StringWriter writer = new StringWriter();
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("Application Number", "Borrower", "Amount", "Product",
                             "Approver", "Decision", "Comments", "Decision Date"))) {

            for (ApplicationApproval approval : approvals) {
                csvPrinter.printRecord(
                        approval.getLoanApplication().getApplicationNumber(),
                        approval.getLoanApplication().getBorrower().getFirstName() + " " +
                                approval.getLoanApplication().getBorrower().getLastName(),
                        approval.getLoanApplication().getAppliedAmount(),
                        approval.getLoanApplication().getLoanProduct().getName(),
                        approval.getApprover().getFirstName() + " " + approval.getApprover().getLastName(),
                        approval.getDecision().name(),
                        approval.getComments(),
                        approval.getDecisionDate()
                );
            }

            csvPrinter.flush();
            return writer.toString().getBytes();

        } catch (IOException e) {
            throw new BusinessException("Failed to generate CSV export", String.valueOf(e));
        }
    }

    private byte[] convertToExcel(List<ApplicationApproval> approvals) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Approvals");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Application Number", "Borrower", "Amount", "Product",
                    "Approver", "Decision", "Comments", "Decision Date"};

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Create data rows
            int rowNum = 1;
            for (ApplicationApproval approval : approvals) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(approval.getLoanApplication().getApplicationNumber());
                row.createCell(1).setCellValue(
                        approval.getLoanApplication().getBorrower().getFirstName() + " " +
                                approval.getLoanApplication().getBorrower().getLastName());
                row.createCell(2).setCellValue(
                        approval.getLoanApplication().getAppliedAmount().doubleValue());
                row.createCell(3).setCellValue(
                        approval.getLoanApplication().getLoanProduct().getName());
                row.createCell(4).setCellValue(
                        approval.getApprover().getFirstName() + " " + approval.getApprover().getLastName());
                row.createCell(5).setCellValue(approval.getDecision().name());
                row.createCell(6).setCellValue(approval.getComments());
                row.createCell(7).setCellValue(
                        approval.getDecisionDate() != null ? approval.getDecisionDate().toString() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new BusinessException("Failed to generate Excel export", String.valueOf(e));
        }
    }



    private SLAStatusDto calculateSLAStatus(LoanApplication application) {
        return workflowRules.calculateSLAStatus(application);
    }


    private PendingApprovalDto convertToPendingApprovalDto(LoanApplication application) {
        return PendingApprovalDto.builder()
                .id(application.getId())
                .applicationNumber(application.getApplicationNumber())
                .borrowerName(application.getBorrower() != null ?
                        application.getBorrower().getFirstName() + " " + application.getBorrower().getLastName() : "N/A")
                .borrowerNumber(application.getBorrower() != null ?
                        application.getBorrower().getBorrowerNumber() : "N/A")
                .borrowerPhone(application.getBorrower() != null ?
                        application.getBorrower().getPhoneNumber() : "N/A")
                .borrowerEmail(application.getBorrower() != null ?
                        application.getBorrower().getEmail() : "N/A")
                .loanProductName(application.getLoanProduct() != null ?
                        application.getLoanProduct().getName() : "N/A")
                .loanProductCode(application.getLoanProduct() != null ?
                        application.getLoanProduct().getProductCode() : "N/A")
                .appliedAmount(application.getAppliedAmount().doubleValue())
                .tenureMonths(application.getTenureMonths())
                .tenureUnit("MONTHS")
                .purpose(application.getPurpose())
                .purposeCategory(application.getPurposeCategory())
                .submittedDate(application.getSubmittedDate())
                .daysSinceSubmission(application.getSubmittedDate() != null ?
                        ChronoUnit.DAYS.between(application.getSubmittedDate(), LocalDateTime.now()) : 0)
                .currentApprovalLevel(application.getCurrentApprovalLevel() != null ?
                        Integer.valueOf(application.getCurrentApprovalLevel()) : 1)
                .status(application.getStatus().name())
                .branchName(application.getBranch() != null ?
                        application.getBranch().getName() : "N/A")
                .branchCode(application.getBranch() != null ?
                        application.getBranch().getCode() : "N/A")
                .branchId(application.getBranch() != null ?
                        application.getBranch().getId() : null)
                .createdBy(application.getCreatedBy() != null ?
                        application.getCreatedBy().toString() : "N/A")
                .createdDate(application.getCreatedAt())
                .interestRate(application.getLoanProduct() != null ?
                        application.getLoanProduct().getInterestRate().doubleValue() : 0.0)
                .interestType("FIXED")
                .processingFee(application.getProcessingFee() != null ?
                        application.getProcessingFee().doubleValue() : 0.0)
                .insuranceFee(application.getInsuranceFee() != null ?
                        application.getInsuranceFee().doubleValue() : 0.0)
                .previousLoans(0) // Would query borrower's loan history
                .repaymentRate(0.0) // Would calculate from history
                .riskScore(application.getRiskScore() != null ? application.getRiskScore() : 0)
                .additionalNotes(application.getAdditionalNotes())
                .build();
    }

    private ApplicationApprovalDto convertToApprovalDto(ApplicationApproval approval) {
        ApplicationApprovalDto dto = approvalMapper.toDto(approval);

        // Calculate additional fields
        if (approval.getDecisionDate() != null && approval.getCreatedAt() != null) {
            long processingHours = ChronoUnit.HOURS.between(
                    approval.getCreatedAt(), approval.getDecisionDate());
            dto.setProcessingTimeHours(processingHours);
            dto.setSlaCompliant(processingHours <= APPROVAL_SLA_HOURS);
        }

        return dto;
    }

    private ApprovalConditionDto convertToConditionDto(ApprovalCondition condition) {
        return ApprovalConditionDto.builder()
                .id(condition.getId())
                .applicationId(condition.getLoanApplication().getId())
                .conditionType(condition.getConditionType())
                .description(condition.getDescription())
                .isMandatory(condition.getMandatory())
                .dueDate(condition.getDueDate() != null ? condition.getDueDate().toString() : null)
                .status(condition.getStatus().name())
                .completedDate(condition.getCompletedDate() != null ?
                        LocalDate.parse(condition.getCompletedDate().toString()) : null)
                .completedBy(condition.getCompletedBy() != null ?
                        condition.getCompletedBy().getUsername() : null)
                .build();
    }

    private ApprovalSummaryDto buildApprovalSummary(LoanApplication application,
                                                    List<ApplicationApproval> approvals,
                                                    List<ApprovalCondition> conditions,
                                                    User currentUser) {
        List<ApprovalWorkflowStepDto> workflowSteps = buildWorkflowSteps(application, approvals, conditions);

        int completedApprovalLevels = (int) approvals.stream()
                .filter(a -> a.getDecision() != GeneralConfig.ApprovalDecision.PENDING)
                .count();

        String nextApprovalRole = determineNextApprovalRole(application, approvals);
        boolean canBeApprovedByCurrentUser = canUserApproveApplication(application.getId(), currentUser);
        String overallDecision = determineOverallDecision(approvals);

        return ApprovalSummaryDto.builder()
                .loanApplicationId(application.getId())
                .applicationNumber(application.getApplicationNumber())
                .currentStatus(application.getStatus().name())
                .currentStage(application.getStage().name())
                .totalApprovalLevels(3) // Could be dynamic based on amount
                .completedApprovalLevels(completedApprovalLevels)
                .nextApprovalRole(nextApprovalRole)
                .requiresMultipleApprovals(true)
                .workflowSteps(workflowSteps)
                .approvalHistory(approvalMapper.toDtoList(approvals))
                .canBeApprovedByCurrentUser(canBeApprovedByCurrentUser)
                .overallDecision(overallDecision)
                .conditions(conditions.stream()
                        .map(this::convertToConditionDto)
                        .collect(Collectors.toList()))
                .build();
    }

    private String determineOverallDecision(List<ApplicationApproval> approvals) {
        if (approvals.isEmpty()) return "PENDING";

        if (approvals.stream().anyMatch(a -> a.getDecision() == GeneralConfig.ApprovalDecision.REJECTED)) {
            return "REJECTED";
        }

        boolean allApproved = approvals.stream()
                .allMatch(a -> a.getDecision() == GeneralConfig.ApprovalDecision.APPROVED);

        return allApproved ? "APPROVED" : "PENDING";
    }




    private LoanApplicationDto enrichLoanApplicationDto(LoanApplication application) {
        log.debug("========== STARTING DTO ENRICHMENT ==========");
        log.debug("Application ID: {}, Number: {}", application.getId(), application.getApplicationNumber());

        try {
            // STEP 1: Base DTO conversion
            log.debug("STEP 1: Converting entity to base DTO using mapper...");
            long step1Start = System.currentTimeMillis();
            LoanApplicationDto dto = null;
            try {
                dto = loanApplicationMapper.toDto(application);
                log.debug("✓ Base DTO created in {}ms", System.currentTimeMillis() - step1Start);
                log.debug("   - DTO ID: {}", dto.getId());
                log.debug("   - DTO Application Number: {}", dto.getApplicationNumber());
                log.debug("   - DTO Status: {}", dto.getStatus());
                log.debug("   - DTO Borrower ID: {}", dto.getBorrowerId());
                log.debug("   - DTO Loan Product ID: {}", dto.getLoanProductId());
            } catch (StackOverflowError e) {
                log.error("🔴 STACKOVERFLOW in loanApplicationMapper.toDto()!");
                log.error("This indicates a circular reference in the entity mapping");
                log.error("Check LoanApplicationMapper for bidirectional mappings");
                throw e;
            } catch (Exception e) {
                log.error("❌ Exception in loanApplicationMapper.toDto(): {}", e.getMessage());
                throw e;
            }

            // STEP 2: Get approval history
            log.debug("STEP 2: Fetching approval history for application {}", application.getId());
            long step2Start = System.currentTimeMillis();
            try {
                List<ApplicationApprovalDto> history = getApprovalHistory(application.getId());
                log.debug("✓ Approval history fetched in {}ms", System.currentTimeMillis() - step2Start);
                log.debug("   - Found {} approval records", history.size());

                if (!history.isEmpty()) {
                    log.debug("   - First approval ID: {}, Decision: {}",
                            history.get(0).getId(), history.get(0).getDecision());
                }

                dto.setApprovalHistory(history);
                log.debug("✓ Approval history set on DTO");
            } catch (StackOverflowError e) {
                log.error("🔴 STACKOVERFLOW in getApprovalHistory()!");
                log.error("Check ApprovalMapper for circular references");
                throw e;
            } catch (Exception e) {
                log.error("❌ Exception in getApprovalHistory(): {}", e.getMessage());
                throw e;
            }

            // STEP 3: Get approval conditions
            log.debug("STEP 3: Fetching approval conditions for application {}", application.getId());
            long step3Start = System.currentTimeMillis();
            try {
                List<ApprovalConditionDto> conditions = getApprovalConditions(application.getId());
                log.debug("✓ Approval conditions fetched in {}ms", System.currentTimeMillis() - step3Start);
                log.debug("   - Found {} conditions", conditions.size());
                dto.setApprovalConditions(conditions);
                log.debug("✓ Approval conditions set on DTO");
            } catch (StackOverflowError e) {
                log.error("🔴 STACKOVERFLOW in getApprovalConditions()!");
                throw e;
            } catch (Exception e) {
                log.error("❌ Exception in getApprovalConditions(): {}", e.getMessage());
                throw e;
            }

            // STEP 4: Get approval workflow (MOST LIKELY CULPRIT)
            log.debug("STEP 4: Building approval workflow for application {}", application.getId());
            long step4Start = System.currentTimeMillis();
            try {
                User currentUser = userService.getUserById(securityUtils.getCurrentUserId());
                log.debug("   - Current user: {} (ID: {})", currentUser.getUsername(), currentUser.getId());

                ApprovalWorkflowDto workflow = getApprovalWorkflow(application.getId(), currentUser);
                log.debug("✓ Approval workflow built in {}ms", System.currentTimeMillis() - step4Start);

                if (workflow != null) {
                    log.debug("   - Workflow steps: {}",
                            workflow.getWorkflowSteps() != null ? workflow.getWorkflowSteps().size() : 0);
                    log.debug("   - Total steps: {}", workflow.getTotalSteps());
                    log.debug("   - Completed steps: {}", workflow.getCompletedSteps());
                    log.debug("   - Next approval role: {}", workflow.getNextApprovalRole());

                    // Check for potential circular references in workflow steps
                    if (workflow.getWorkflowSteps() != null && !workflow.getWorkflowSteps().isEmpty()) {
                        ApprovalWorkflowStepDto firstStep = workflow.getWorkflowSteps().get(0);
                        log.debug("   - First step: Step {}, Role: {}, Status: {}",
                                firstStep.getStepNumber(), firstStep.getRole(), firstStep.getStatus());

                        // Use reflection to check for hidden fields
                        try {
                            java.lang.reflect.Field[] fields = firstStep.getClass().getDeclaredFields();
                            for (java.lang.reflect.Field field : fields) {
                                field.setAccessible(true);
                                Object value = field.get(firstStep);
                                if (value != null) {
                                    String typeName = value.getClass().getName();
                                    if (typeName.contains("ApplicationApprovalDto") ||
                                            typeName.contains("LoanApplicationDto")) {
                                        log.error("🔴 CIRCULAR REFERENCE FOUND in ApprovalWorkflowStepDto!");
                                        log.error("   Field: {}, Type: {}", field.getName(), typeName);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.debug("Reflection check failed: {}", e.getMessage());
                        }
                    }
                }

                dto.setApprovalWorkflow(workflow);
                log.debug("✓ Approval workflow set on DTO");

            } catch (StackOverflowError e) {
                log.error("🔴🔴🔴 STACKOVERFLOW in getApprovalWorkflow()! 🔴🔴🔴");
                log.error("This is the most likely location of the circular reference.");
                log.error("Check these classes for bidirectional references:");
                log.error("   1. ApprovalWorkflowDto");
                log.error("   2. ApprovalWorkflowStepDto");
                log.error("   3. ApplicationApprovalDto (if referenced in workflow steps)");
                log.error("   4. LoanApplicationDto (if referenced in workflow)");
                throw e;
            } catch (Exception e) {
                log.error("❌ Exception in getApprovalWorkflow(): {}", e.getMessage());
                throw e;
            }

            log.debug("✓ DTO enrichment completed successfully for application {}", application.getId());
            log.debug("========== DTO ENRICHMENT COMPLETE ==========");

            return dto;

        } catch (StackOverflowError e) {
            log.error("🔴🔴🔴 STACKOVERFLOW ERROR in enrichLoanApplicationDto! 🔴🔴🔴");
            log.error("Application ID: {}", application.getId());
            log.error("");
            log.error("=== CIRCULAR REFERENCE DIAGNOSIS ===");
            log.error("");
            log.error("Check these relationships in your code:");
            log.error("");
            log.error("1. LoanProduct ↔ ProductType (most common)");
            log.error("   - In LoanProduct: @ManyToOne private ProductType productType");
            log.error("   - In ProductType: @OneToMany private List<LoanProduct> loanProducts");
            log.error("   - Fix: Add @JsonIgnore to one side, preferably ProductType.loanProducts");
            log.error("");
            log.error("2. LoanApplication ↔ ApplicationApproval");
            log.error("   - In LoanApplication: @OneToMany private List<ApplicationApproval> approvals");
            log.error("   - In ApplicationApproval: @ManyToOne private LoanApplication loanApplication");
            log.error("   - Fix: Already have @JsonIgnore on approvals? Check ApplicationApprovalDto");
            log.error("");
            log.error("3. Workflow DTOs");
            log.error("   - Check if ApprovalWorkflowStepDto contains ApplicationApprovalDto");
            log.error("   - Check if ApprovalWorkflowDto contains LoanApplicationDto");
            log.error("");
            log.error("4. Mapper configurations");
            log.error("   - Check LoanProductMapper for productType mapping");
            log.error("   - Check ProductTypeMapper for loanProducts mapping");
            log.error("   - Check ApprovalMapper for loanApplication mapping");

            throw e;
        }
    }



    private void checkForCircularReferences(Object obj, String context, Set<Object> visited) {
        if (obj == null || visited.contains(obj)) return;
        visited.add(obj);
        log.debug("Checking {} - {}", context, obj.getClass().getSimpleName());
        // Check all fields using reflection
        for (java.lang.reflect.Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (value != null) {
                    String fieldType = value.getClass().getName();

                    // Check for DTO types that might cause circular references
                    if (fieldType.contains("ApplicationApprovalDto") ||
                            fieldType.contains("LoanApplicationDto") ||
                            fieldType.contains("LoanProductDto") ||
                            fieldType.contains("ProductTypeDto")) {

                        log.warn("⚠️ Potential circular reference: {}.{} = {}",
                                obj.getClass().getSimpleName(), field.getName(), fieldType);

                        // Recursively check this object
                        if (!visited.contains(value)) {
                            checkForCircularReferences(value, context + "." + field.getName(), visited);
                        }
                    }

                    // Check collections
                    if (value instanceof Collection) {
                        Collection<?> collection = (Collection<?>) value;
                        if (!collection.isEmpty()) {
                            Object first = collection.iterator().next();
                            String firstType = first.getClass().getName();
                            if (firstType.contains("Dto")) {
                                log.warn("⚠️ Collection contains DTOs: {} contains {}",
                                        field.getName(), firstType);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Could not check field {}: {}", field.getName(), e.getMessage());
            }
        }
    }



    private void clearCaches(Long applicationId) {
        // Clear workflow cache
        workflowCache.remove(applicationId);
        // Clear stats cache for all users
        statsCache.clear();
    }

    private String buildStatsCacheKey(Long userId, Long branchId, String period) {
        return String.format("stats:%d:%d:%s:%s",
                userId,
                branchId != null ? branchId : 0,
                period != null ? period : "default",
                LocalDate.now().toString());
    }


    private boolean isFinalApproval(LoanApplication application, User approver) {
        int currentLevel = determineApprovalLevel(application, approver);
        return workflowRules.isFinalApproval(application, currentLevel, approver);
    }


    private void escalateToNextLevel(LoanApplication application, User approver) {
        // Implementation for escalating to next approval level
        log.info("Escalating application {} to next approval level", application.getId());
        // Send escalation notifications
        sendEscalationNotifications(application, approver);
    }

    private void completeAllConditions(Long applicationId) {
        List<ApprovalCondition> conditions = approvalConditionRepository
                .findByLoanApplicationId(applicationId);

        conditions.forEach(condition -> {
            condition.setStatus(GeneralConfig.ConditionStatus.COMPLETED);
            condition.setCompletedDate(LocalDateTime.now());
            approvalConditionRepository.save(condition);
        });
    }

    private void logRejectionAnalytics(LoanApplication application, ApprovalDecisionDto dto) {
        // Implementation for logging rejection analytics
        log.debug("Logging rejection analytics for application {}", application.getId());
    }

    private void updateRelatedEntitiesOnRejection(LoanApplication application) {
        // Implementation for updating related entities on rejection
    }

    @Async
    private void sendApprovalNotificationsWithLevel(LoanApplication application, User approver,
                                                    ApprovalDecisionDto dto, int currentLevel,
                                                    int totalLevels, boolean isFinalApproval) {
        try {
            // Notify the current approver
            notificationService.sendApprovalNotification(
                    approver.getId(),
                    application.getApplicationNumber(),
                    "APPROVED",
                    String.format("You have approved this application at level %d of %d. Comments: %s",
                            currentLevel, totalLevels, dto.getComments()),
                    approver.getUsername()
            );
            // If not final approval, notify next approver
            if (!isFinalApproval) {
                String nextRole = getNextApprovalRole(application, currentLevel);
                List<User> nextApprovers = userRepository.findByRole(User.UserRole.valueOf(nextRole));

                for (User nextApprover : nextApprovers) {
                    notificationService.sendApprovalRequestNotification(
                            nextApprover.getId(),
                            application.getApplicationNumber(),
                            String.format("Application requires your approval at level %d of %d",
                                    currentLevel + 1, totalLevels),
                            String.format("Application %s has been approved at level %d and now requires your approval at level %d",
                                    application.getApplicationNumber(), currentLevel, currentLevel + 1),
                            null,null,null
                    );
                }

            } else {
                // Notify loan officer that approval is complete
                if (application.getCreatedBy() != null) {
                    notificationService.sendApprovalNotification(
                            application.getCreatedBy(),
                            application.getApplicationNumber(),
                            "FULLY_APPROVED",
                            "Your loan application has been fully approved and is ready for disbursement",
                            "System"
                    );
                }
            }

            // Notify borrower for final approval
            if (isFinalApproval && application.getBorrower() != null && application.getBorrower().getEmail() != null) {
                notificationService.sendBorrowerNotification(
                        application.getBorrower().getEmail(),
                        application.getApplicationNumber(),
                        "APPROVED",
                        "Congratulations! Your loan application has been fully approved."
                );
            }

        } catch (Exception e) {
            log.error("Failed to send approval notifications: {}", e.getMessage());
        }
    }

    @Async
    private void sendRejectionNotifications(LoanApplication application, User approver, ApprovalDecisionDto dto) {
        try {
            // Send to loan officer
            if (application.getCreatedBy() != null) {
                notificationService.sendApprovalNotification(
                        application.getCreatedBy(),
                        application.getApplicationNumber(),
                        "REJECTED",
                        dto.getComments(),
                        approver.getUsername()
                );
            }

            // Send to borrower
            if (application.getBorrower() != null && application.getBorrower().getEmail() != null) {
                notificationService.sendBorrowerNotification(
                        application.getBorrower().getEmail(),
                        application.getApplicationNumber(),
                        "REJECTED",
                        dto.getComments()
                );
            }

        } catch (Exception e) {
            log.error("Failed to send rejection notifications: {}", e.getMessage());
        }
    }

    @Async
    private void sendReturnNotifications(LoanApplication application, User approver, ApprovalDecisionDto dto) {
        try {
            // Send to loan officer
            if (application.getCreatedBy() != null) {
                notificationService.sendApprovalNotification(
                        application.getCreatedBy(),
                        application.getApplicationNumber(),
                        "RETURNED",
                        dto.getComments(),
                        approver.getUsername()
                );
            }

        } catch (Exception e) {
            log.error("Failed to send return notifications: {}", e.getMessage());
        }
    }

    // ========== COMMENTS IMPLEMENTATION ==========

    @Override
    public List<ApprovalCommentDto> getApprovalComments(Long applicationId) {
        log.debug("Getting approval comments for application: {}", applicationId);

        List<ApprovalComment> comments = approvalCommentRepository
                .findByLoanApplicationIdOrderByCreatedAtDesc(applicationId);

        return comments.stream()
                .map(this::convertToCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    public ApprovalCommentDto addApprovalComment(Long applicationId, AddCommentDto dto, User currentUser) {
        log.info("Adding comment to application {} by user: {}", applicationId, currentUser.getUsername());

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        ApprovalComment comment = ApprovalComment.builder()
                .loanApplication(application)
                .comment(dto.getComment())
                .commenter(currentUser)
                .commenterRole(currentUser.getRole().name())
                .isInternal(dto.isInternal())
                .parentCommentId(dto.getParentCommentId() != null ? Long.valueOf(dto.getParentCommentId()) : null)
                .createdAt(LocalDateTime.now())
                .createdBy(currentUser.getId())
                .build();

        ApprovalComment saved = approvalCommentRepository.save(comment);

        // Send notification if requested
        if (dto.isSendNotification()) {
            sendCommentNotification(application, currentUser, dto.getComment());
        }

        // Audit
        auditService.logApprovalAction(applicationId, "ADD_COMMENT", currentUser.getId(), dto.getComment());

        return convertToCommentDto(saved);
    }

    private ApprovalCommentDto convertToCommentDto(ApprovalComment comment) {
        return ApprovalCommentDto.builder()
                .id(comment.getId())
                .applicationId(comment.getLoanApplication().getId())
                .applicationNumber(comment.getLoanApplication().getApplicationNumber())
                .comment(comment.getComment())
                .commenterName(comment.getCommenter().getFullName())
                .commenterUsername(comment.getCommenter().getUsername())
                .commenterRole(comment.getCommenterRole())
                .commenterId(comment.getCommenter().getId())
                .createdAt(comment.getCreatedAt())
                .isInternal(comment.isInternal())
                .parentCommentId(comment.getParentCommentId() != null ? String.valueOf(comment.getParentCommentId()) : null)
                .build();
    }

// ========== TIMELINE IMPLEMENTATION ==========

    @Override
    public ApprovalTimelineDto getApprovalTimeline(Long applicationId) {
        log.debug("Getting approval timeline for application: {}", applicationId);

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        List<ApprovalTimelineDto.TimelineEvent> events = new ArrayList<>();

        // Add application creation event
        events.add(createTimelineEvent("APPLICATION_CREATED", "Application Created",
                "Application was created", application.getCreatedAt(),
                application.getCreatedByUser(), null, null));

        // Add submission event
        if (application.getSubmittedDate() != null) {
            events.add(createTimelineEvent("APPLICATION_SUBMITTED", "Application Submitted",
                    "Application was submitted for approval", application.getSubmittedDate(),
                    null, "DRAFT", "SUBMITTED"));
        }

        // Add approval history events
        List<ApplicationApproval> approvals = approvalRepository
                .findByLoanApplicationIdOrderByApprovalLevelAsc(applicationId);

        for (ApplicationApproval approval : approvals) {
            String eventType = approval.getDecision().name();
            String eventName = getDecisionDisplayName(approval.getDecision());
            String description = String.format("Application was %s by %s at level %d",
                    eventName.toLowerCase(), approval.getApprover().getFullName(), approval.getApprovalLevel());

            ApprovalTimelineDto.TimelineEvent event = createTimelineEvent(
                    eventType, eventName, description, approval.getDecisionDate(),
                    approval.getApprover(), approval.getLoanApplication().getStatus().name(),
                    getStatusAfterDecision(approval.getDecision(), approval.getApprovalLevel()));

            event.setDecision(approval.getDecision().name());
            event.setApprovalLevel(approval.getApprovalLevel());
            event.setComments(approval.getComments());

            events.add(event);
        }

        // Sort events chronologically
        events.sort(Comparator.comparing(ApprovalTimelineDto.TimelineEvent::getTimestamp));

        // Calculate statistics
        long totalProcessingHours = calculateTotalProcessingTime(approvals, application);

        return ApprovalTimelineDto.builder()
                .applicationId(application.getId())
                .applicationNumber(application.getApplicationNumber())
                .borrowerName(application.getBorrower().getFullName())
                .appliedAmount(application.getAppliedAmount())
                .events(events)
                .applicationDate(application.getCreatedAt())
                .submittedDate(application.getSubmittedDate())
                .currentStatus(application.getStatus().name())
                .currentStage(application.getStage().name())
                .totalEvents(events.size())
                .totalApprovals((int) approvals.stream().filter(a -> a.getDecision() == GeneralConfig.ApprovalDecision.APPROVED).count())
                .totalRejections((int) approvals.stream().filter(a -> a.getDecision() == GeneralConfig.ApprovalDecision.REJECTED).count())
                .totalReturns((int) approvals.stream().filter(a -> a.getDecision() == GeneralConfig.ApprovalDecision.RETURNED_FOR_REVISION).count())
                .totalProcessingTimeHours(totalProcessingHours)
                .build();
    }

    private ApprovalTimelineDto.TimelineEvent createTimelineEvent(String eventType, String eventName,
                                                                  String description, LocalDateTime timestamp, User actor, String statusBefore, String statusAfter) {

        return ApprovalTimelineDto.TimelineEvent.builder()
                .timestamp(timestamp)
                .eventType(eventType)
                .eventTypeDisplay(eventName)
                .description(description)
                .actor(actor != null ? actor.getFullName() : "System")
                .actorUsername(actor != null ? actor.getUsername() : "system")
                .actorRole(actor != null ? actor.getRole().name() : "SYSTEM")
                .statusBefore(statusBefore)
                .statusAfter(statusAfter)
                .build();
    }

    private String getDecisionDisplayName(GeneralConfig.ApprovalDecision decision) {
        switch (decision) {
            case APPROVED: return "Approved";
            case REJECTED: return "Rejected";
            case RETURNED_FOR_REVISION: return "Returned for Revision";
            default: return decision.name();
        }
    }

    private String getStatusAfterDecision(GeneralConfig.ApprovalDecision decision, int approvalLevel) {
        if (decision == GeneralConfig.ApprovalDecision.APPROVED) {
            return approvalLevel >= 3 ? "APPROVED" : "UNDER_REVIEW";
        } else if (decision == GeneralConfig.ApprovalDecision.REJECTED) {
            return "REJECTED";
        } else {
            return "NEEDS_REVISION";
        }
    }

    private long calculateTotalProcessingTime(List<ApplicationApproval> approvals, LoanApplication application) {
        if (approvals.isEmpty()) return 0;

        LocalDateTime firstApproval = approvals.get(0).getCreatedAt();
        LocalDateTime lastDecision = approvals.get(approvals.size() - 1).getDecisionDate();

        if (firstApproval == null || lastDecision == null) return 0;

        return ChronoUnit.HOURS.between(firstApproval, lastDecision);
    }

// ========== QUEUE POSITION IMPLEMENTATION ==========

    @Override
    public QueuePositionDto getApprovalQueuePosition(Long applicationId) {
        log.debug("Getting queue position for application: {}", applicationId);

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        // Get all pending applications in queue order
        List<LoanApplication> queue = getQueueOrder(application.getBranch().getId());

        // Find position
        int position = -1;
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getId().equals(applicationId)) {
                position = i;
                break;
            }
        }

        if (position == -1) {
            return QueuePositionDto.builder()
                    .applicationId(applicationId)
                    .applicationNumber(application.getApplicationNumber())
                    .positionInQueue(null)
                    .totalPendingInQueue(queue.size())
                    .message("Application is not in the pending approval queue")
                    .build();
        }

        // Calculate estimated wait time (assume 30 minutes per application)
        int estimatedWaitMinutes = (position + 1) * 30;

        // Get applications ahead
        List<QueuePositionDto.ApplicationAhead> applicationsAhead = new ArrayList<>();
        for (int i = 0; i < Math.min(position, 5); i++) { // Show up to 5 ahead
            LoanApplication ahead = queue.get(i);
            applicationsAhead.add(QueuePositionDto.ApplicationAhead.builder()
                    .applicationId(ahead.getId())
                    .applicationNumber(ahead.getApplicationNumber())
                    .borrowerName(ahead.getBorrower().getFullName())
                    .amount(ahead.getAppliedAmount())
                    .priorityScore(calculatePriorityScore(ahead))
                    .priorityLevel(getPriorityLevel(calculatePriorityScore(ahead)))
                    .submittedAt(ahead.getSubmittedDate())
                    .waitingHours(ChronoUnit.HOURS.between(ahead.getSubmittedDate(), LocalDateTime.now()))
                    .build());
        }

        // Calculate priority
        int priorityScore = calculatePriorityScore(application);
        String priorityLevel = getPriorityLevel(priorityScore);
        String priorityReason = getPriorityReason(application, priorityScore);

        // SLA information
        LocalDateTime slaDeadline = application.getSubmittedDate().plusHours(APPROVAL_SLA_HOURS);
        boolean isOverdue = LocalDateTime.now().isAfter(slaDeadline);
        long hoursRemaining = ChronoUnit.HOURS.between(LocalDateTime.now(), slaDeadline);

        return QueuePositionDto.builder()
                .applicationId(applicationId)
                .applicationNumber(application.getApplicationNumber())
                .positionInQueue(position + 1)
                .totalPendingInQueue(queue.size())
                .estimatedWaitTimeMinutes(estimatedWaitMinutes)
                .estimatedWaitTimeDisplay(formatDuration(estimatedWaitMinutes))
                .applicationsAheadCount(position)
                .applicationsAhead(applicationsAhead)
                .priorityScore(priorityScore)
                .priorityLevel(priorityLevel)
                .isPriority(priorityScore <= 1)
                .priorityReason(priorityReason)
                .submittedAt(application.getSubmittedDate())
                .hoursSinceSubmission(ChronoUnit.HOURS.between(application.getSubmittedDate(), LocalDateTime.now()))
                .slaDeadline(slaDeadline)
                .isOverdue(isOverdue)
                .hoursRemaining(Math.max(0, hoursRemaining))
                .build();
    }

    private List<LoanApplication> getQueueOrder(Long branchId) {
        return approvalRepository.findQueueOrderByBranch(branchId);
    }

    /**
     * Get priority level label based on priority score
     * @param priorityScore The priority score (0 = highest priority, higher numbers = lower priority)
     * @return Priority level label
     */

    private String getPriorityLevel(int priorityScore) {
        return workflowRules.getPriorityLevel(priorityScore);
    }



    private String formatDuration(int minutes) {
        if (minutes < 60) return minutes + " minutes";
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        if (remainingMinutes == 0) return hours + " hours";
        return hours + "h " + remainingMinutes + "m";
    }

    private String getPriorityReason(LoanApplication application, int priorityScore) {
        return workflowRules.getPriorityReason(application, priorityScore);
    }

// ========== DELEGATION IMPLEMENTATION ==========

    @Override
    public DelegateApprovalResult delegateApproval(Long applicationId, DelegateApprovalDto dto, User currentUser) {
        log.info("Delegating approval of application {} from user {} to user {}",
                applicationId, currentUser.getUsername(), dto.getDelegateTo());

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        User delegate = userRepository.findById(dto.getDelegateTo())
                .orElseThrow(() -> new ResourceNotFoundException("Delegate user not found: " + dto.getDelegateTo()));

        // Check if user can delegate this application
        if (!canUserApproveApplication(applicationId, currentUser)) {
            throw new BusinessException("You do not have permission to delegate this application");
        }

        // Check if already delegated
        Optional<ApprovalDelegation> existing = approvalDelegationRepository
                .findByLoanApplicationIdAndDelegatorIdAndStatus(applicationId, currentUser.getId(),
                        ApprovalDelegation.DelegationStatus.ACTIVE);

        if (existing.isPresent()) {
            throw new BusinessException("This application has already been delegated to another user");
        }

        // Calculate expiry date
        LocalDateTime expiryDate = dto.getExpiryDate();
        if (expiryDate == null && dto.getDuration() != null) {
            expiryDate = calculateExpiryDate(dto.getDuration());
        }

        // Create delegation record
        ApprovalDelegation delegation = ApprovalDelegation.builder()
                .loanApplication(application)
                .delegator(currentUser)
                .delegate(delegate)
                .reason(dto.getReason())
                .status(ApprovalDelegation.DelegationStatus.ACTIVE)
                .delegatedAt(LocalDateTime.now())
                .expiresAt(expiryDate)
                .keepPermissions(dto.isKeepDelegatePermissions())
                .createdAt(LocalDateTime.now())
                .createdBy(currentUser.getId())
                .build();

        ApprovalDelegation saved = approvalDelegationRepository.save(delegation);

        // Send notification
        if (dto.isNotifyDelegate()) {
            sendDelegationNotification(application, currentUser, delegate, dto.getReason(), expiryDate);
        }

        // Audit
        auditService.logApprovalAction(applicationId, "DELEGATE", currentUser.getId(),
                String.format("Delegated to %s: %s", delegate.getUsername(), dto.getReason()));

        return DelegateApprovalResult.builder()
                .delegationId(saved.getId())
                .applicationId(applicationId)
                .applicationNumber(application.getApplicationNumber())
                .delegatorId(currentUser.getId())
                .delegatorName(currentUser.getFullName())
                .delegateId(delegate.getId())
                .delegateName(delegate.getFullName())
                .delegateRole(delegate.getRole().name())
                .reason(dto.getReason())
                .delegatedAt(LocalDateTime.now())
                .expiresAt(expiryDate)
                .isActive(true)
                .success(true)
                .message(String.format("Approval delegated to %s successfully", delegate.getFullName()))
                .build();
    }

    @Override
    public List<ApprovalDelegationDto> getApprovalDelegations(Long delegatorId, Long delegateId, boolean activeOnly) {
        log.debug("Getting approval delegations - delegator: {}, delegate: {}, activeOnly: {}",
                delegatorId, delegateId, activeOnly);

        List<ApprovalDelegation> delegations;

        if (delegatorId != null) {
            delegations = approvalDelegationRepository.findByDelegatorIdAndStatus(delegatorId,
                    activeOnly ? ApprovalDelegation.DelegationStatus.ACTIVE : null);
        } else if (delegateId != null) {
            delegations = approvalDelegationRepository.findByDelegateIdAndStatus(delegateId,
                    activeOnly ? ApprovalDelegation.DelegationStatus.ACTIVE : null);
        } else {
            delegations = approvalDelegationRepository.findAll();
            if (activeOnly) {
                delegations = delegations.stream()
                        .filter(d -> d.getStatus() == ApprovalDelegation.DelegationStatus.ACTIVE)
                        .collect(Collectors.toList());
            }
        }

        return delegations.stream()
                .map(this::convertToDelegationDto)
                .collect(Collectors.toList());
    }

    private ApprovalDelegationDto convertToDelegationDto(ApprovalDelegation delegation) {
        return ApprovalDelegationDto.builder()
                .id(delegation.getId())
                .applicationId(delegation.getLoanApplication() != null ? delegation.getLoanApplication().getId() : null)
                .applicationNumber(delegation.getLoanApplication() != null ?
                        delegation.getLoanApplication().getApplicationNumber() : null)
                .delegatorId(delegation.getDelegator().getId())
                .delegatorName(delegation.getDelegator().getFullName())
                .delegatorUsername(delegation.getDelegator().getUsername())
                .delegateId(delegation.getDelegate().getId())
                .delegateName(delegation.getDelegate().getFullName())
                .delegateUsername(delegation.getDelegate().getUsername())
                .delegateRole(delegation.getDelegate().getRole().name())
                .reason(delegation.getReason())
                .status(delegation.getStatus().name())
                .delegatedAt(delegation.getDelegatedAt())
                .expiresAt(delegation.getExpiresAt())
                .revokedAt(delegation.getRevokedAt())
                .revokedBy(delegation.getRevokedBy() != null ? String.valueOf(delegation.getRevokedBy()) : null)
                .revocationReason(delegation.getRevocationReason())
                .isActive(delegation.getStatus() == ApprovalDelegation.DelegationStatus.ACTIVE)
                .build();
    }

    private LocalDateTime calculateExpiryDate(String duration) {
        LocalDateTime now = LocalDateTime.now();
        switch (duration.toLowerCase()) {
            case "4h": return now.plusHours(4);
            case "1d": return now.plusDays(1);
            case "3d": return now.plusDays(3);
            case "1w": return now.plusWeeks(1);
            default: return now.plusDays(1);
        }
    }

// ========== REMINDERS IMPLEMENTATION ==========

    @Override
    public List<ApprovalReminderDto> getApprovalReminders(Long approverId, boolean overdueOnly, int limit) {
        log.debug("Getting approval reminders for approver: {}, overdueOnly: {}", approverId, overdueOnly);

        Long targetApproverId = approverId != null ? approverId : securityUtils.getCurrentUserId();

        List<ApprovalReminder> reminders;

        if (overdueOnly) {
            reminders = approvalReminderRepository.findOverdueRemindersForApprover(targetApproverId, LocalDateTime.now());
        } else {
            reminders = approvalReminderRepository.findByApproverIdAndIsDismissedFalseOrderByDueDateAsc(targetApproverId);
        }

        if (limit > 0 && reminders.size() > limit) {
            reminders = reminders.subList(0, limit);
        }

        return reminders.stream()
                .map(this::convertToReminderDto)
                .collect(Collectors.toList());
    }

    @Override
    public void dismissApprovalReminder(Long reminderId, User currentUser, String reason) {
        log.info("Dismissing reminder {} by user: {}, reason: {}", reminderId, currentUser.getUsername(), reason);

        ApprovalReminder reminder = approvalReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found: " + reminderId));

        // Check if user can dismiss this reminder
        if (!reminder.getApproverId().equals(currentUser.getId())) {
            throw new BusinessException("You can only dismiss your own reminders");
        }
        // Use the repository method with all 4 parameters
        approvalReminderRepository.dismissReminder(
                reminderId,
                LocalDateTime.now(),
                currentUser.getId(),
                reason != null ? reason : "Dismissed by user"
        );
        // Audit
       /* auditService.logAction("DISMISS_REMINDER",
                String.format("Reminder %d dismissed by %s: %s", reminderId, currentUser.getUsername(), reason));

        */
    }

    private ApprovalReminderDto convertToReminderDto(ApprovalReminder reminder) {
        return ApprovalReminderDto.builder()
                .id(reminder.getId())
                .applicationId(reminder.getApplicationId())
                .applicationNumber(reminder.getApplicationNumber())
                .borrowerName(reminder.getBorrowerName())
                .amount(reminder.getAmount())
                .approverId(reminder.getApproverId())
                .approverName(reminder.getApproverName())
                .reminderType(reminder.getReminderType())
                .priority(reminder.getPriority())
                .message(reminder.getMessage())
                .createdAt(reminder.getCreatedAt())
                .dueDate(reminder.getDueDate())
                .isOverdue(reminder.getDueDate() != null && reminder.getDueDate().isBefore(LocalDateTime.now()))
                .isDismissed(reminder.getIsDismissed())
                .dismissedAt(reminder.getDismissedAt())
                .dismissedBy(reminder.getDismissedBy() != null ? String.valueOf(reminder.getDismissedBy()) : null)
                .reminderCount(reminder.getReminderCount())
                .lastSentAt(reminder.getLastSentAt())
                .build();
    }

// ========== ESCALATION IMPLEMENTATION ==========
    @Transactional
    @Override
    public EscalationResult escalateApproval(Long applicationId, EscalationDto dto, User currentUser) {
        log.info("Escalating application {} by user: {}, priority: {}",
                applicationId, currentUser.getUsername(), dto.getPriority());

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        // Check if escalation is allowed
        if (!canEscalateApplication(application, currentUser)) {
            throw new BusinessException("You do not have permission to escalate this application");
        }

        // Determine escalation target role
        String targetRole = dto.getEscalateToRole();
        if (targetRole == null) {
            targetRole = determineEscalationTargetRole(application);
        }

        // Find users to escalate to
        List<User> targets = userRepository.findByRole(User.UserRole.valueOf(targetRole));
        if (targets.isEmpty()) {
            throw new BusinessException("No users found with role: " + targetRole);
        }

        // Create escalation record
        ApprovalEscalation escalation = ApprovalEscalation.builder()
                .loanApplication(application)
                .escalatedBy(currentUser.getId())
                .reason(dto.getReason())
                .priority(dto.getPriority())
                .escalatedToRole(targetRole)
                .status(ApprovalEscalation.EscalationStatus.PENDING)
                .escalatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .createdBy(currentUser.getId())
                .build();

        ApprovalEscalation saved = approvalEscalationRepository.save(escalation);

        // Update application to indicate escalation
        application.setStage(GeneralConfig.ApplicationStage.ESCALATED);
        loanApplicationRepository.save(application);

        // Send notifications to targets
        List<EscalationResult.EscalationTarget> escalationTargets = new ArrayList<>();
        for (User target : targets) {
            boolean notified = sendEscalationNotification(application, currentUser, target, dto);
            escalationTargets.add(EscalationResult.EscalationTarget.builder()
                    .userId(target.getId())
                    .userName(target.getFullName())
                    .userRole(target.getRole().name())
                    .userEmail(target.getEmail())
                    .notified(notified)
                    .build());
        }

        // Audit
        auditService.logApprovalAction(applicationId, "ESCALATE", currentUser.getId(),
                String.format("Escalated with priority %s: %s", dto.getPriority(), dto.getReason()));

        return EscalationResult.builder()
                .escalationId(saved.getId())
                .applicationId(applicationId)
                .applicationNumber(application.getApplicationNumber())
                .escalatedBy(currentUser.getFullName())
                .escalatedByUsername(currentUser.getUsername())
                .reason(dto.getReason())
                .priority(dto.getPriority())
                .status("PENDING")
                .escalatedAt(LocalDateTime.now())
                .escalatedToRole(targetRole)
                .targets(escalationTargets)
                .success(true)
                .message(String.format("Application escalated to %s role(s)", targetRole))
                .build();
    }

    private boolean canEscalateApplication(LoanApplication application, User user) {
        // Only users who can approve can escalate
        if (!canUserApproveApplication(application.getId(), user)) {
            return false;
        }
        // Check if already escalated recently
        List<ApprovalEscalation> recentEscalations = approvalEscalationRepository
                .findByLoanApplicationIdOrderByEscalatedAtDesc(application.getId());
        if (!recentEscalations.isEmpty()) {
            ApprovalEscalation last = recentEscalations.get(0);
            if (last.getStatus() == ApprovalEscalation.EscalationStatus.PENDING) {
                return false; // Already pending escalation
            }
        }
        return true;
    }

    private String determineEscalationTargetRole(LoanApplication application) {
        BigDecimal amount = application.getAppliedAmount();
        return workflowRules.determineEscalationTargetRole(amount);
    }


// ========== HELPER METHODS FOR NOTIFICATIONS ==========

    private void sendCommentNotification(LoanApplication application, User commenter, String comment) {
        // Implementation for sending notifications
        log.debug("Sending comment notification for application: {}", application.getApplicationNumber());
    }

    private void sendDelegationNotification(LoanApplication application, User delegator, User delegate,
                                            String reason, LocalDateTime expiryDate) {
        // Implementation for sending delegation notifications
        log.debug("Sending delegation notification to: {}", delegate.getEmail());
    }

    private boolean sendEscalationNotification(LoanApplication application, User escalator,
                                               User target, EscalationDto dto) {
        // Implementation for sending escalation notifications
        log.debug("Sending escalation notification to: {}", target.getEmail());
        return true;
    }




    @Async
    private void sendEscalationNotifications(LoanApplication application, User approver) {
        try {
            // Send escalation notifications to higher authorities
            notificationService.sendEscalationNotification(
                    application.getApplicationNumber(),
                    approver.getUsername(),
                    Integer.parseInt(application.getCurrentApprovalLevel() + 1)
            );

        } catch (Exception e) {
            log.error("Failed to send escalation notifications: {}", e.getMessage());
        }
    }



    private long countApprovedApplications(List<ApplicationApproval> approvals) {
        if (approvals == null || approvals.isEmpty()) {
            return 0L;
        }
        return approvals.stream()
                .filter(approval -> approval.getDecision() == GeneralConfig.ApprovalDecision.APPROVED)
                .count();
    }


    private long countRejectedApplications(List<ApplicationApproval> approvals) {
        if (approvals == null || approvals.isEmpty()) {
            return 0L;
        }

        return approvals.stream()
                .filter(approval -> approval.getDecision() == GeneralConfig.ApprovalDecision.REJECTED)
                .count();
    }


    private long countReturnedApplications(List<ApplicationApproval> approvals) {
        if (approvals == null || approvals.isEmpty()) {
            return 0L;
        }

        return approvals.stream()
                .filter(approval -> approval.getDecision() == GeneralConfig.ApprovalDecision.RETURNED_FOR_REVISION)
                .count();
    }


    private double calculateAvgProcessingTime(List<ApplicationApproval> approvals) {
        if (approvals == null || approvals.isEmpty()) {
            return 0.0;
        }

        return approvals.stream()
                .filter(a -> a.getDecisionDate() != null && a.getCreatedAt() != null)
                .mapToLong(a -> ChronoUnit.HOURS.between(a.getCreatedAt(), a.getDecisionDate()))
                .average()
                .orElse(0.0);
    }



    private AmountMetrics calculateAmountMetrics(List<ApplicationApproval> approvals) {
        if (approvals == null || approvals.isEmpty()) {
            return new AmountMetrics(0.0, 0.0, 0.0, 0.0);
        }

        List<Double> approvedAmounts = approvals.stream()
                .filter(a -> a.getDecision() == GeneralConfig.ApprovalDecision.APPROVED)
                .filter(a -> a.getLoanApplication() != null && a.getLoanApplication().getAppliedAmount() != null)
                .map(a -> a.getLoanApplication().getAppliedAmount().doubleValue())
                .collect(Collectors.toList());

        if (approvedAmounts.isEmpty()) {
            return new AmountMetrics(0.0, 0.0, 0.0, 0.0);
        }

        double total = approvedAmounts.stream().mapToDouble(Double::doubleValue).sum();
        double average = total / approvedAmounts.size();
        double largest = approvedAmounts.stream().max(Double::compare).orElse(0.0);
        double smallest = approvedAmounts.stream().min(Double::compare).orElse(0.0);

        return new AmountMetrics(total, average, largest, smallest);
    }


}