package com.microfinance.audit.service;

import com.microfinance.audit.dto.AuditLogDto;
import com.microfinance.audit.dto.AuditLogFilterDto;
import com.microfinance.borrower.dto.BorrowerActivityDto;
import com.microfinance.common.config.GeneralConfig;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface AuditService {
    
    @Async
    void logApprovalAction(Long applicationId, String action, Long userId, String comments);
    
    @Async
    void logRejectionAction(Long applicationId, Long userId, String reason);
    
    @Async
    void logDisbursementAction(Long loanId, Long userId, BigDecimal amount);

    @Async
    void logRepaymentAction(Long loanId, Long userId, BigDecimal amount);

    @Async
    void logEntityAction(Long borrowerId, Long userId,
                         String entityType,//BORROWER
                         String action,//BORROWER CREATION
                         String details // Borrower with id created
    );

    @Async
    void logWaivedRepaymentAction(Long loanId, Long userId, BigDecimal amount);

    @Async
    void logReverseRepaymentAction(Long loanId, Long userId, BigDecimal amount);

    @Async
    void logWriteOffAction(Long loanId, Long userId, BigDecimal amount);

    @Async
    void logRejectWriteOffAction(Long loanId, Long userId, BigDecimal amount);

    @Async
    void logSystemAction(String action, String entityType, Long entityId, String details);

    @Async
    void logUserAction(String action, String entityType, Long entityId, String details, String severity);

    @Async
    void logReportGeneration(String reportType, String format, Long durationMs);

    @Async
    void logReportExport(String reportType, String format);

    @Async
    void logChartOfAccountAction(Long applicationId, String action, Long userId, String comments);

    @Transactional(readOnly = true)
    Page<AuditLogDto> getAuditLogs(AuditLogFilterDto filter);

    @Transactional(readOnly = true)
    List<AuditLogDto> getRecentSecurityEvents(int limit);

    @Async
    BorrowerActivityDto logBorrowerStandardActivity(Long borrowerId, GeneralConfig.BorrowerActivityType activityType,
                                                    String description, Long performedBy, String referenceType, Long referenceId);

    @Async
    void logLoginAction(Long userId, String username, String ipAddress, String userAgent, boolean success, String failureReason);

    @Async
    void logLogoutAction(Long userId, String username, String ipAddress, String sessionId);

    @Async
    void logLoginAttempt(String username, String ipAddress, String userAgent, boolean success, String failureReason);

    //Genaral Log Method that logs to acivity logs, borrower activity logs and audit Logs
    @Async
    void masterAuditLogs(
            Long entityId,
            GeneralConfig.BorrowerActivityType borrowerActivityType,
            String entityType,
            String details
    );
}