package com.microfinance.loanapplications.repository;

import com.microfinance.loanapplications.entity.ApprovalReminder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalReminderRepository extends JpaRepository<ApprovalReminder, Long> {
    
    // Basic queries by approver
    List<ApprovalReminder> findByApproverIdOrderByDueDateAsc(Long approverId);
    
    List<ApprovalReminder> findByApproverIdAndIsDismissedFalseOrderByDueDateAsc(Long approverId);
    
    List<ApprovalReminder> findByApproverIdAndIsDismissedTrueOrderByDismissedAtDesc(Long approverId);
    

    // Active reminders - FIXED: Use proper parameter names
    @Query("SELECT r FROM ApprovalReminder r WHERE r.isDismissed = false AND r.dueDate <= :now ORDER BY r.dueDate ASC")
    List<ApprovalReminder> findOverdueReminders(@Param("now") LocalDateTime now);

    @Query("SELECT r FROM ApprovalReminder r WHERE r.isDismissed = false AND r.approverId = :approverId AND r.dueDate <= :now ORDER BY r.dueDate ASC")
    List<ApprovalReminder> findOverdueRemindersForApprover(@Param("approverId") Long approverId, @Param("now") LocalDateTime now);
    
    @Query("SELECT r FROM ApprovalReminder r WHERE r.isDismissed = false AND r.nextReminderAt <= :now ORDER BY r.nextReminderAt ASC")
    List<ApprovalReminder> findRemindersToSend(@Param("now") LocalDateTime now);
    
    // Reminders by application
    List<ApprovalReminder> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
    
    List<ApprovalReminder> findByApplicationIdAndIsDismissedFalse(Long applicationId);
    
    Optional<ApprovalReminder> findByApplicationIdAndReminderTypeAndIsDismissedFalse(
            Long applicationId, String reminderType);
    
    // Reminders by type
    List<ApprovalReminder> findByReminderTypeAndIsDismissedFalseOrderByDueDateAsc(String reminderType);
    
    List<ApprovalReminder> findByReminderTypeAndApproverIdAndIsDismissedFalse(String reminderType, Long approverId);
    
    // Priority-based queries
    List<ApprovalReminder> findByPriorityAndIsDismissedFalseOrderByDueDateAsc(String priority);
    
    List<ApprovalReminder> findByApproverIdAndPriorityAndIsDismissedFalseOrderByDueDateAsc(
            Long approverId, String priority);
    
    // Statistics queries
    @Query("SELECT COUNT(r) FROM ApprovalReminder r WHERE r.approverId = :approverId AND r.isDismissed = false")
    Long countActiveRemindersForApprover(@Param("approverId") Long approverId);
    
    @Query("SELECT COUNT(r) FROM ApprovalReminder r WHERE r.approverId = :approverId AND r.isDismissed = false AND r.dueDate < :now")
    Long countOverdueRemindersForApprover(@Param("approverId") Long approverId,
                                           @Param("now") LocalDateTime now);
    
    @Query("SELECT r.reminderType, COUNT(r) FROM ApprovalReminder r WHERE r.approverId = :approverId AND r.isDismissed = false GROUP BY r.reminderType")
    List<Object[]> countRemindersByTypeForApprover(@Param("approverId") Long approverId);
    
    @Query("SELECT r.priority, COUNT(r) FROM ApprovalReminder r WHERE r.approverId = :approverId AND r.isDismissed = false GROUP BY r.priority")
    List<Object[]> countRemindersByPriorityForApprover(@Param("approverId") Long approverId);
    
    // Dismissal operations
    @Modifying
    @Transactional
    @Query("UPDATE ApprovalReminder r SET r.isDismissed = true, r.dismissedAt = :now, r.dismissedBy = :dismissedBy, " +
           "r.dismissalReason = :reason, r.updatedAt = :now WHERE r.id = :reminderId")
    void dismissReminder(@Param("reminderId") Long reminderId,
                         @Param("now") LocalDateTime now,
                         @Param("dismissedBy") Long dismissedBy,
                         @Param("reason") String reason);
    
    @Modifying
    @Transactional
    @Query("UPDATE ApprovalReminder r SET r.isDismissed = true, r.dismissedAt = :now, r.updatedAt = :now " +
           "WHERE r.applicationId = :applicationId")
    void dismissAllRemindersForApplication(@Param("applicationId") Long applicationId,
                                            @Param("now") LocalDateTime now);
    
    @Modifying
    @Transactional
    @Query("UPDATE ApprovalReminder r SET r.isDismissed = true, r.dismissedAt = :now, r.dismissedBy = :dismissedBy " +
           "WHERE r.approverId = :approverId AND r.isDismissed = false")
    int dismissAllRemindersForApprover(@Param("approverId") Long approverId,
                                        @Param("now") LocalDateTime now,
                                        @Param("dismissedBy") Long dismissedBy);
    
    // Reminder count updates
    @Modifying
    @Transactional
    @Query("UPDATE ApprovalReminder r SET r.reminderCount = r.reminderCount + 1, r.lastSentAt = :now, " +
           "r.nextReminderAt = :nextReminderAt, r.notificationSent = true, r.updatedAt = :now WHERE r.id = :reminderId")
    void incrementReminderCount(@Param("reminderId") Long reminderId,
                                @Param("now") LocalDateTime now,
                                @Param("nextReminderAt") LocalDateTime nextReminderAt);
    
    // Expiry operations
    @Modifying
    @Transactional
    @Query("UPDATE ApprovalReminder r SET r.isDismissed = true, r.dismissedAt = :now, " +
           "r.dismissalReason = 'Auto-dismissed: Reminder expired', r.updatedAt = :now " +
           "WHERE r.isDismissed = false AND r.dueDate < :cutoffDate")
    int autoDismissExpiredReminders(@Param("cutoffDate") LocalDateTime cutoffDate,
                                     @Param("now") LocalDateTime now);
    
    // Reference-based queries
    List<ApprovalReminder> findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(Long referenceId, String referenceType);
    
    Optional<ApprovalReminder> findByReferenceIdAndReferenceTypeAndIsDismissedFalse(
            Long referenceId, String referenceType);
    
    // Date range queries
    @Query("SELECT r FROM ApprovalReminder r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    List<ApprovalReminder> findRemindersCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT r FROM ApprovalReminder r WHERE r.dueDate BETWEEN :startDate AND :endDate")
    List<ApprovalReminder> findRemindersDueBetween(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);
    
    // Batch operations for cleanup
    @Modifying
    @Transactional
    @Query("DELETE FROM ApprovalReminder r WHERE r.isDismissed = true AND r.dismissedAt < :cutoffDate")
    int deleteOldDismissedReminders(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Analytics queries
    @Query("SELECT FUNCTION('DATE', r.createdAt), COUNT(r) FROM ApprovalReminder r " +
           "WHERE r.createdAt >= :startDate GROUP BY FUNCTION('DATE', r.createdAt)")
    List<Object[]> getReminderCreationTrends(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT AVG(r.reminderCount) FROM ApprovalReminder r WHERE r.isDismissed = true")
    Double getAverageRemindersBeforeDismissal();
}