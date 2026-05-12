// src/main/java/com/microfinance/system/service/impl/ActivityLogServiceImpl.java
package com.microfinance.system.service;

import com.microfinance.common.config.GeneralConfig;
import com.microfinance.system.dto.ActivityLogDto;
import com.microfinance.system.entity.ActivityLog;
import com.microfinance.system.mapper.ActivityLogServiceMapper;
import com.microfinance.system.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogServiceMapper activityLogMapper; // Changed from activityLogServiceMapper
    private final HttpServletRequest httpServletRequest;

    @Override
    @Transactional
    public ActivityLog logBorrowerActivity(Long borrowerId, 
                                         GeneralConfig.BorrowerActivityType activityType, 
                                         String description, 
                                         Long performedBy) {
        return logBorrowerActivity(borrowerId, activityType, description, performedBy, null, null, null, null, null, null, null);
    }

    @Override
    @Transactional
    public ActivityLog logBorrowerActivity(Long borrowerId, 
                                         GeneralConfig.BorrowerActivityType activityType, 
                                         String description, 
                                         Long performedBy,
                                         String ipAddress,
                                         String userAgent) {
        return logBorrowerActivity(borrowerId, activityType, description, performedBy, ipAddress, userAgent, null, null, null, null, null);
    }

    @Override
    @Transactional
    public ActivityLog logBorrowerActivity(Long borrowerId, 
                                         GeneralConfig.BorrowerActivityType activityType, 
                                         String description, 
                                         Long performedBy,
                                         String ipAddress,
                                         String userAgent,
                                         Long groupId,
                                         Long loanId,
                                         Long documentId,
                                         String oldValue,
                                         String newValue) {
        
        ActivityLog activityLog = new ActivityLog();
        activityLog.setBorrowerId(borrowerId);
        activityLog.setActivityType(activityType);
        activityLog.setDescription(description);
        activityLog.setPerformedBy(performedBy);
        activityLog.setActivityDate(LocalDateTime.now());
        
        // Get IP address and user agent from request if not provided
        if (ipAddress == null && httpServletRequest != null) {
            ipAddress = getClientIpAddress();
        }
        if (userAgent == null && httpServletRequest != null) {
            userAgent = httpServletRequest.getHeader("User-Agent");
        }
        
        activityLog.setIpAddress(ipAddress);
        activityLog.setUserAgent(userAgent);
        activityLog.setGroupId(groupId);
        activityLog.setLoanId(loanId);
        activityLog.setDocumentId(documentId);
        activityLog.setOldValue(oldValue);
        activityLog.setNewValue(newValue);

        ActivityLog savedLog = activityLogRepository.save(activityLog);
        
        log.info("Activity logged: {} for borrower {} by user {}", 
                 activityType, borrowerId, performedBy);
        return savedLog;
    }



    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLogDto> getBorrowerActivityLogs(Long borrowerId, Pageable pageable) {
        Page<ActivityLog> activityLogs = activityLogRepository.findByBorrowerId(borrowerId, pageable);
        List<ActivityLogDto> dtoList = activityLogMapper.toDtoList(activityLogs.getContent()); // Updated method name
        return new PageImpl<>(dtoList, pageable, activityLogs.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLogDto> getUserActivityLogs(Long userId, Pageable pageable) {
        Page<ActivityLog> activityLogs = activityLogRepository.findByPerformedBy(userId, pageable);
        List<ActivityLogDto> dtoList = activityLogMapper.toDtoList(activityLogs.getContent()); // Updated method name
        return new PageImpl<>(dtoList, pageable, activityLogs.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLogDto> getActivityLogsByType(GeneralConfig.BorrowerActivityType activityType, Pageable pageable) {
        Page<ActivityLog> activityLogs = activityLogRepository.findByActivityType(activityType.name(), pageable);
        List<ActivityLogDto> dtoList = activityLogMapper.toDtoList(activityLogs.getContent()); // Updated method name
        return new PageImpl<>(dtoList, pageable, activityLogs.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLogDto> getActivityLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<ActivityLog> activityLogs = activityLogRepository.findByActivityDateBetween(startDate, endDate, pageable);
        List<ActivityLogDto> dtoList = activityLogMapper.toDtoList(activityLogs.getContent()); // Updated method name
        return new PageImpl<>(dtoList, pageable, activityLogs.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLogDto> getGroupActivityLogs(Long groupId, Pageable pageable) {
        Page<ActivityLog> activityLogs = activityLogRepository.findByGroupId(groupId, pageable);
        List<ActivityLogDto> dtoList = activityLogMapper.toDtoList(activityLogs.getContent()); // Updated method name
        return new PageImpl<>(dtoList, pageable, activityLogs.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLogDto> getRecentBorrowerActivities(Long borrowerId, int limit) {
        List<ActivityLog> activityLogs = activityLogRepository.findByBorrowerIdOrderByActivityDateDesc(borrowerId);
        return activityLogMapper.toDtoList( // Updated method name
            activityLogs.stream().limit(limit).collect(Collectors.toList())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Long getBorrowerActivityCount(Long borrowerId) {
        return activityLogRepository.countByBorrowerId(borrowerId);
    }

    // Helper method to get client IP address
    private String getClientIpAddress() {
        if (httpServletRequest == null) {
            return "127.0.0.1";
        }
        
        String xForwardedFor = httpServletRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }
        
        String xRealIp = httpServletRequest.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return httpServletRequest.getRemoteAddr();
    }

    // Convenience methods for common activities
    @Transactional
    @Override
    public void logGroupLeaderAssignment(Long borrowerId, Long groupId, String groupName, Long performedBy) {
        logBorrowerActivity(
            borrowerId,
            GeneralConfig.BorrowerActivityType.GROUP_LEADER_ASSIGNED,
            "Assigned as group leader for group: " + groupName,
            performedBy,
            null, null,
            groupId, null, null, null, null
        );
    }

    @Transactional
    @Override
    public void logGroupMemberAddition(Long borrowerId, Long groupId, String groupName, Long performedBy) {
        logBorrowerActivity(
            borrowerId,
            GeneralConfig.BorrowerActivityType.GROUP_ASSIGNED,
            "Added to group: " + groupName,
            performedBy,
            null, null,
            groupId, null, null, null, null
        );
    }

    @Transactional
    @Override
    public void logGroupMemberRemoval(Long borrowerId, Long groupId, String groupName, Long performedBy) {
        logBorrowerActivity(
            borrowerId,
            GeneralConfig.BorrowerActivityType.GROUP_REMOVED,
            "Removed from group: " + groupName,
            performedBy,
            null, null,
            groupId, null, null, null, null
        );
    }

    @Transactional
    @Override
    public void logLoanApplication(Long borrowerId, Long loanId, String loanNumber, Long performedBy) {
        logBorrowerActivity(
            borrowerId,
            GeneralConfig.BorrowerActivityType.LOAN_APPLICATION_SUBMITTED,
            "Loan application submitted: " + loanNumber,
            performedBy,
            null, null,
            null, loanId, null, null, null
        );
    }


    @Transactional
    public void logKycStatusChange(Long borrowerId, GeneralConfig.KycStatus oldStatus, GeneralConfig.KycStatus newStatus, Long performedBy) {
        GeneralConfig.BorrowerActivityType activityType = (newStatus == GeneralConfig.KycStatus.VERIFIED) ?
            GeneralConfig.BorrowerActivityType.BORROWER_KYC_VERIFIED :
            GeneralConfig.BorrowerActivityType.BORROWER_KYC_REJECTED;
            
        logBorrowerActivity(
            borrowerId,
            activityType,
            "KYC status changed from " + oldStatus + " to " + newStatus,
            performedBy,
            null, null,
            null, null, null,
            oldStatus.name(),
            newStatus.name()
        );
    }

    // Additional convenience methods
    @Transactional
    @Override
    public void logBorrowerCreation(Long borrowerId, String borrowerName, Long performedBy) {
        logBorrowerActivity(
            borrowerId,
            GeneralConfig.BorrowerActivityType.BORROWER_CREATED,
            "Borrower created: " + borrowerName,
            performedBy,
            null, null,
            null, null, null, null, null
        );
    }

    @Transactional
    @Override
    public void logDocumentUpload(Long borrowerId, Long documentId, String documentType, Long performedBy) {
        logBorrowerActivity(
            borrowerId,
            GeneralConfig.BorrowerActivityType.DOCUMENT_UPLOADED,
            "Document uploaded: " + documentType,
            performedBy,
            null, null,
            null, null, documentId, null, null
        );
    }

    @Transactional
    @Override
    public void logRepayment(Long borrowerId, Long loanId, String amount, Long performedBy) {
        logBorrowerActivity(
            borrowerId,
            GeneralConfig.BorrowerActivityType.REPAYMENT_MADE,
            "Repayment made: " + amount,
            performedBy,
            null, null,
            null, loanId, null, null, null
        );
    }
}