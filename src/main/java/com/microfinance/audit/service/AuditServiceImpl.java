// service/AuditServiceImpl.java
package com.microfinance.audit.service;

import com.microfinance.audit.dto.AuditLogDto;
import com.microfinance.audit.dto.AuditLogFilterDto;
import com.microfinance.audit.entity.AuditLog;
import com.microfinance.audit.repository.AuditLogRepository;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.borrower.dto.BorrowerActivityDto;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.entity.BorrowerActivity;
import com.microfinance.borrower.repository.BorrowerActivityRepository;
import com.microfinance.borrower.repository.BorrowerRepository;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.system.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final BorrowerRepository borrowerRepository;
    private final BorrowerActivityRepository borrowerActivityRepository;
    private final SecurityUtils securityUtils;

    @Autowired
    private final ActivityLogService activityLogService;


    @Override
    @Async
    public void logApprovalAction(Long applicationId, String action, Long userId, String comments) {
        try {
            log.info("Audit Log - Application {}: Action '{}' by user {} with comments: {}", 
                    applicationId, action, userId, comments);
            
            AuditLog auditLog = AuditLog.builder()
                    .entityType("LOAN_APPLICATION")
                    .entityId(applicationId)
                    .action(action)
                    .userId(userId)
                    .username(getUsername(userId))
                    .details(comments)
                    .severity("INFO")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Failed to log approval action: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    public void logRejectionAction(Long applicationId, Long userId, String reason) {
        try {
            log.info("Audit Log - Application {}: REJECTED by user {} with reason: {}", 
                    applicationId, userId, reason);
            
            AuditLog auditLog = AuditLog.builder()
                    .entityType("LOAN_APPLICATION")
                    .entityId(applicationId)
                    .action("REJECT")
                    .userId(userId)
                    .username(getUsername(userId))
                    .details(reason)
                    .severity("WARNING")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Failed to log rejection action: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    public void logDisbursementAction(Long loanId, Long userId, BigDecimal amount) {
        try {
            log.info("Audit Log - Loan {}: DISBURSED by user {} for amount {}", 
                    loanId, userId, amount);
            
            AuditLog auditLog = AuditLog.builder()
                    .entityType("LOAN")
                    .entityId(loanId)
                    .action("DISBURSE")
                    .userId(userId)
                    .username(getUsername(userId))
                    .details("Amount: " + amount)
                    .severity("INFO")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Failed to log disbursement action: {}", e.getMessage(), e);
        }
    }

    @Async
    @Override
    public void logRepaymentAction(Long loanId, Long userId, BigDecimal amount) {
        try {
            log.info("Audit Log - Loan {}: Repayment by user {} for amount {}",
                    loanId, userId, amount);

            AuditLog auditLog = AuditLog.builder()
                    .entityType("LOAN")
                    .entityId(loanId)
                    .action("REPAYMENT")
                    .userId(userId)
                    .username(getUsername(userId))
                    .details("Amount: " + amount)
                    .severity("INFO")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Failed to log repayment action: {}", e.getMessage(), e);
        }
    }


    @Async
    @Override
    public void logEntityAction(Long borrowerId, Long userId,
                                String entityType,//BORROWER
                                String action,//BORROWER CREATION
                                String details // Borrower with id created
    ) {
        try {

            log.info("Audit Log - entityType {} ID: {}",entityType,borrowerId);

            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(borrowerId)
                    .action(action)
                    .userId(userId)
                    .username(getUsername(userId))
                    .details(details)
                    .severity("INFO")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Failed to log borrower action: {}", e.getMessage(), e);
        }
    }



    @Async
    @Override
    public void logWaivedRepaymentAction(Long loanId, Long userId, BigDecimal amount) {
        try {
            log.info("Audit Log - Loan Waived {}: Repayment by user {} for amount {}",
                    loanId, userId, amount);

            AuditLog auditLog = AuditLog.builder()
                    .entityType("LOAN")
                    .entityId(loanId)
                    .action("REPAYMENT-WAIVER")
                    .userId(userId)
                    .username(getUsername(userId))
                    .details("Amount: " + amount)
                    .severity("WARNING")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Failed to log repayment waiver action: {}", e.getMessage(), e);
        }
    }



    @Async
    @Override
    public void logReverseRepaymentAction(Long loanId, Long userId, BigDecimal amount) {
        try {
            log.info("Audit Log - Loan {}: Reverse Repayment by user {} for amount {}",
                    loanId, userId, amount);

            AuditLog auditLog = AuditLog.builder()
                    .entityType("LOAN")
                    .entityId(loanId)
                    .action("REPAYMENT-REVERSAL")
                    .userId(userId)
                    .username(getUsername(userId))
                    .details("Amount: " + amount)
                    .severity("WARNING")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Failed to log reverse repayment action: {}", e.getMessage(), e);
        }
    }




    @Async
    @Override
    public void logWriteOffAction(Long loanId, Long userId, BigDecimal amount) {
        try {
            log.info("Audit Log - Loan Write Off {}:  by user {} for amount {}",
                    loanId, userId, amount);

            AuditLog auditLog = AuditLog.builder()
                    .entityType("LOAN")
                    .entityId(loanId)
                    .action("WRITE-OFF")
                    .userId(userId)
                    .username(getUsername(userId))
                    .details("Amount: " + amount)
                    .severity("INFO")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Failed to log loan write off action: {}", e.getMessage(), e);
        }
    }



    @Async
    @Override
    public void logRejectWriteOffAction(Long loanId, Long userId, BigDecimal amount) {
        try {
            log.info("Audit Log - Reject Loan Write Off {}:  by user {} for amount {}",
                    loanId, userId, amount);

            AuditLog auditLog = AuditLog.builder()
                    .entityType("LOAN")
                    .entityId(loanId)
                    .action("REJECT-WRITE-OFF")
                    .userId(userId)
                    .username(getUsername(userId))
                    .details("Amount: " + amount)
                    .severity("WARNING")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Failed to log reject loan write off action: {}", e.getMessage(), e);
        }
    }

    
    @Override
    @Async
    public void logSystemAction(String action, String entityType, Long entityId, String details) {
        try {
            log.info("Audit Log - System: {} on {} {} - Details: {}", 
                    action, entityType, entityId, details);
            
            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .userId(null)
                    .username("SYSTEM")
                    .details(details)
                    .severity("INFO")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Failed to log system action: {}", e.getMessage(), e);
        }
    }

    @Async
    @Override
    public void logUserAction(String action, String entityType, Long entityId, String details, String severity) {
        try {
            Long currentUserId = securityUtils.getCurrentUserId();
            String currentUsername = securityUtils.getCurrentUsername();
            String ipAddress = securityUtils.getCurrentIpAddress();
            String userAgent = securityUtils.getCurrentUserAgent();
            
            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .userId(currentUserId)
                    .username(currentUsername)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .details(details)
                    .severity(severity != null ? severity : "INFO")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Failed to log user action: {}", e.getMessage(), e);
        }
    }

    @Async
    @Override
    public void logReportGeneration(String reportType, String format, Long durationMs) {
        try {
            Long currentUserId = securityUtils.getCurrentUserId();
            String currentUsername = securityUtils.getCurrentUsername();
            
            AuditLog auditLog = AuditLog.builder()
                    .entityType("REPORT")
                    .action("REPORT_GENERATED")
                    .userId(currentUserId)
                    .username(currentUsername)
                    .resource(reportType)
                    .details("Format: " + format)
                    .durationMs(durationMs)
                    .severity("INFO")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Failed to log report generation: {}", e.getMessage(), e);
        }
    }

    @Async
    @Override
    public void logReportExport(String reportType, String format) {
        try {
            Long currentUserId = securityUtils.getCurrentUserId();
            String currentUsername = securityUtils.getCurrentUsername();
            
            AuditLog auditLog = AuditLog.builder()
                    .entityType("REPORT")
                    .action("REPORT_EXPORTED")
                    .userId(currentUserId)
                    .username(currentUsername)
                    .resource(reportType)
                    .details(format)
                    .severity("INFO")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Failed to log report export: {}", e.getMessage(), e);
        }
    }


    @Async
    @Override
    public void logChartOfAccountAction(Long applicationId, String action, Long userId, String comments) {
        try {
            log.info("Audit Log - Chart Of Accounts {}: Action '{}' by user {} with comments: {}",
                    applicationId, action, userId, comments);

            AuditLog auditLog = AuditLog.builder()
                    .entityType("CHART_OF_ACCOUNTS")
                    .entityId(applicationId)
                    .action(action)
                    .userId(userId)
                    .username(getUsername(userId))
                    .details(comments)
                    .severity("INFO")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Failed to log Chart Account  action: {}", e.getMessage(), e);
        }
    }


    @Transactional(readOnly = true)
    @Override
    public Page<AuditLogDto> getAuditLogs(AuditLogFilterDto filter) {
        Pageable pageable = PageRequest.of(
            filter.getPage() != null ? filter.getPage() : 0,
            filter.getSize() != null ? filter.getSize() : 20,
            Sort.by(Sort.Direction.DESC, filter.getSortBy() != null ? filter.getSortBy() : "timestamp")
        );
        
        Page<AuditLog> auditLogs = auditLogRepository.findAuditLogsByFilters(
            filter.getEntityType(),
            filter.getEntityId(),
            filter.getAction(),
            filter.getUserId(),
            filter.getSeverity(),
            filter.getStartDate(),
            filter.getEndDate(),
            filter.getSearchTerm(),
            pageable
        );
        
        return auditLogs.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    @Override
    public List<AuditLogDto> getRecentSecurityEvents(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<AuditLog> events = auditLogRepository.findRecentSecurityEvents(pageable);
        return events.stream().map(this::convertToDto).collect(Collectors.toList());
    }


    private String getUsername(Long userId) {
        String  uname=userRepository.getUserNameById(userId);
        return userId != null ? "User_" + uname : "UNKNOWN";
    }


    private AuditLogDto convertToDto(AuditLog auditLog) {
        return AuditLogDto.builder()
                .id(auditLog.getId())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .action(auditLog.getAction())
                .userId(auditLog.getUserId())
                .username(auditLog.getUsername())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .sessionId(auditLog.getSessionId())
                .details(auditLog.getDetails())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .severity(auditLog.getSeverity())
                .durationMs(auditLog.getDurationMs())
                .resource(auditLog.getResource())
                .status(auditLog.getStatus())
                .timestamp(auditLog.getTimestamp())
                .build();
    }


    //Mainly Borrower related activities logs

    @Async
    @Override
    public BorrowerActivityDto logBorrowerStandardActivity(Long borrowerId, GeneralConfig.BorrowerActivityType activityType,
                                                           String description, Long performedBy, String referenceType, Long referenceId) {
        BorrowerActivityDto activity = new BorrowerActivityDto();
        activity.setBorrowerId(borrowerId);
        activity.setActivityType(activityType);
        activity.setDescription(description);
        activity.setActivityDate(LocalDateTime.now());
        activity.setPerformedBy(performedBy);
        activity.setReferenceType(referenceType);
        activity.setReferenceId(referenceId);
        return logActivity(activity);
    }

    @Transactional
    public BorrowerActivityDto logActivity(BorrowerActivityDto activityDto) {
        log.info("Logging activity for borrower: {}, type: {}",
                activityDto.getBorrowerId(), activityDto.getActivityType());

        try {
            Borrower borrower = borrowerRepository.findById(activityDto.getBorrowerId())
                    .orElseThrow(() -> new RuntimeException("Borrower not found with id: " + activityDto.getBorrowerId()));

            BorrowerActivity activity = new BorrowerActivity();
            activity.setBorrower(borrower);
            activity.setActivityType(activityDto.getActivityType());
            activity.setDescription(activityDto.getDescription());
            activity.setDetails(activityDto.getDetails());
            activity.setActivityDate(activityDto.getActivityDate() != null ?
                    activityDto.getActivityDate() : LocalDateTime.now());
            activity.setPerformedBy(activityDto.getPerformedBy());

            // Get performer name if performedBy is provided but performedByName is not
            if (activityDto.getPerformedBy() != null && activityDto.getPerformedByName() == null) {
                try {
                    User performer = userRepository.findById(activityDto.getPerformedBy()).orElse(null);
                    if (performer != null) {
                        activity.setPerformedByName(performer.getFirstName() + " " +
                                (performer.getLastName() != null ? performer.getLastName() : ""));
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch performer name for user ID: {}", activityDto.getPerformedBy());
                }
            } else {
                activity.setPerformedByName(activityDto.getPerformedByName());
            }

            activity.setReferenceType(activityDto.getReferenceType());
            activity.setReferenceId(activityDto.getReferenceId());
            activity.setReferenceNumber(activityDto.getReferenceNumber());
            activity.setBranchName(activityDto.getBranchName());
            activity.setIpAddress(activityDto.getIpAddress());
            activity.setUserAgent(activityDto.getUserAgent());
            activity.setSessionId(activityDto.getSessionId());

            BorrowerActivity savedActivity = borrowerActivityRepository.save(activity);
            log.info("Activity logged successfully with id: {}", savedActivity.getId());

            return BorrowerActivityDto.fromEntity(savedActivity);

        } catch (Exception e) {
            log.error("Error logging activity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to log activity", e);
        }
    }


    //Genaral Log Method that logs to acivity logs, borrower activity logs and audit Logs
    @Async
    @Override
    public void masterAuditLogs(
            Long entityId,
            GeneralConfig.BorrowerActivityType borrowerActivityType,
            String entityType,
            String details
    ){
        Optional<User> currentUser = userRepository.findById(securityUtils.getCurrentUserId());
        String createdByName;
        Long createdById=null;

        if(currentUser.isPresent()){
            createdByName=currentUser.get().getFullName();
            createdById=currentUser.get().getId();
        }

        try{
        activityLogService.logBorrowerActivity(
                entityId,// updatedBorrower.getId()
                borrowerActivityType,//GeneralConfig.BorrowerActivityType.BORROWER_UPDATED,
                details ,//"Borrower Created by name: " + updatedBorrower.getFullName(),
                createdById
        );

        //audit log as well
        logEntityAction(
                entityId,//updatedBorrower.getId(),
                createdById,
                entityType,//"BORROWER",
                String.valueOf(borrowerActivityType),//"BORROWER UPDATED",
                details//"Borrower with ID: "+updatedBorrower.getFullName()+" Created"
        );

        logBorrowerStandardActivity(
                entityId,
                borrowerActivityType, //GeneralConfig.BorrowerActivityType.BORROWER_UPDATED,
                details ,//"Borrower Created by name: " + updatedBorrower.getFullName(),
                createdById,
                entityType,//"BORROWER",
                entityId //updatedBorrower.getId()
        );

        } catch (Exception e) {
            log.error("Failed to log  activity: {}", e.getMessage());
            // Don't throw - just log the error
        }


    }








}