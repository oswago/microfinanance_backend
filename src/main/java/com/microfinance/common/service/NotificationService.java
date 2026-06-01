package com.microfinance.common.service;

import com.microfinance.common.dto.InAppNotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface NotificationService {
    
    @Async
    void sendApprovalNotification(Long userId, String applicationNumber, String action, 
                                  String comments, String approverName);
    
    @Async
    void sendBorrowerNotification(String email, String applicationNumber, String action, String comments);
    
    @Async
    void sendEscalationNotification(String applicationNumber, String escalatedBy, int newLevel);
    
    @Async
    void sendRejectionNotification(String email, String applicationNumber, String reason);
    
    @Async
    void sendDisbursementNotification(String email, String applicationNumber, BigDecimal amount);
    
    @Async
    void sendOverdueNotification(String applicationNumber, int daysOverdue, String assignedTo);
    
    @Async
    void sendSlaBreachNotification(String applicationNumber, String role, int hoursOverdue);

   // void sendApprovalRequestNotification(Long id, String applicationNumber, String format, String format1);

    @Async
    void sendApprovalRequestNotification(Long approverId, String applicationNumber,
                                         String subject, String message,
                                         Integer currentLevel, Integer totalLevels,
                                         String previousApprover);


    /*void sendApprovalRequestNotification(Long approverId, String applicationNumber,
                                         String subject, String message);*/

    @Async
    void sendToNextApprover(Long nextApproverId, String applicationNumber,
                            int currentLevel, int totalLevels,
                            String previousApprover, String comments);

    // Implementation
    Page<InAppNotificationDto> getUserNotifications(Long userId, Pageable pageable);

    long getUnreadNotificationCount(Long userId);

    @Transactional
    void markNotificationAsRead(Long notificationId, Long userId);

    void markAllNotificationsAsRead(Long userId);

    void createLoanApplicationSubmittedNotification(Long applicationId, String applicationNumber, Long borrowerId);

    void createLoanApprovedNotification(Long applicationId, String applicationNumber, Long approverId, Long borrowerId);

    void createLoanRejectedNotification(Long applicationId, String applicationNumber, String reason, Long borrowerId);

    void createLoanDisbursedNotification(Long loanId, String loanAccountNumber, Long borrowerId);

    void createRepaymentReceivedNotification(Long loanId, String loanAccountNumber, BigDecimal amount, Long borrowerId);

    void createDocumentVerifiedNotification(Long documentId, String documentName, Long borrowerId);

    void createDocumentRejectedNotification(Long documentId, String documentName, String reason, Long borrowerId);

    void createKycCompletedNotification(Long borrowerId);

    void createKycExpiringNotification(Long borrowerId, int daysUntilExpiry);

    void createOverdueNotification(Long loanId, String loanAccountNumber, int daysOverdue, Long borrowerId);

    void createPaymentReminderNotification(Long loanId, String loanAccountNumber, LocalDate dueDate, BigDecimal amount, Long borrowerId);

    void createGroupActivityNotification(Long groupId, String groupName, String activityType, Long memberId);
}