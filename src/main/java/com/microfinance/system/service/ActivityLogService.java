// src/main/java/com/microfinance/system/service/ActivityLogService.java
package com.microfinance.system.service;

import com.microfinance.common.config.GeneralConfig;
import com.microfinance.system.dto.ActivityLogDto;
import com.microfinance.system.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityLogService {
    
    ActivityLog logBorrowerActivity(Long borrowerId, 
                                   GeneralConfig.BorrowerActivityType activityType, 
                                   String description, 
                                   Long performedBy);
    
    ActivityLog logBorrowerActivity(Long borrowerId, 
                                   GeneralConfig.BorrowerActivityType activityType, 
                                   String description, 
                                   Long performedBy,
                                   String ipAddress,
                                   String userAgent);
    
    ActivityLog logBorrowerActivity(Long borrowerId, 
                                   GeneralConfig.BorrowerActivityType activityType, 
                                   String description, 
                                   Long performedBy,
                                   String ipAddress,
                                   String userAgent,
                                   Long groupId,
                                   Long loanId,
                                   Long documentId,
                                   String oldValue,
                                   String newValue);
    
    Page<ActivityLogDto> getBorrowerActivityLogs(Long borrowerId, Pageable pageable);
    
    Page<ActivityLogDto> getUserActivityLogs(Long userId, Pageable pageable);
    
    Page<ActivityLogDto> getActivityLogsByType(GeneralConfig.BorrowerActivityType activityType, Pageable pageable);
    
    Page<ActivityLogDto> getActivityLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    Page<ActivityLogDto> getGroupActivityLogs(Long groupId, Pageable pageable);
    
    List<ActivityLogDto> getRecentBorrowerActivities(Long borrowerId, int limit);
    
    Long getBorrowerActivityCount(Long borrowerId);

    // Convenience methods for common activities
    @Transactional
    void logGroupLeaderAssignment(Long borrowerId, Long groupId, String groupName, Long performedBy);

    @Transactional
    void logGroupMemberAddition(Long borrowerId, Long groupId, String groupName, Long performedBy);

    @Transactional
    void logGroupMemberRemoval(Long borrowerId, Long groupId, String groupName, Long performedBy);

    @Transactional
    void logLoanApplication(Long borrowerId, Long loanId, String loanNumber, Long performedBy);

    // Additional convenience methods
    @Transactional
    void logBorrowerCreation(Long borrowerId, String borrowerName, Long performedBy);

    @Transactional
    void logDocumentUpload(Long borrowerId, Long documentId, String documentType, Long performedBy);

    @Transactional
    void logRepayment(Long borrowerId, Long loanId, String amount, Long performedBy);
}