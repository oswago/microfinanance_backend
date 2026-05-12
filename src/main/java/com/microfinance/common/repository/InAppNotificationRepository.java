package com.microfinance.common.repository;

import com.microfinance.common.entity.InAppNotification;
import org.springframework.data.domain.Page;
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
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {
    
    // ===== USER-SPECIFIC QUERIES =====
    
    /**
     * Find all notifications for a user, ordered by creation date descending
     */
    List<InAppNotification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find paginated notifications for a user
     */
    Page<InAppNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Find unread notifications for a user
     */
    List<InAppNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    
    /**
     * Count unread notifications for a user
     */
    @Query("SELECT COUNT(n) FROM InAppNotification n WHERE n.userId = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") Long userId);
    
    // ===== TYPE-SPECIFIC QUERIES =====
    
    /**
     * Find notifications by type for a user
     */
    List<InAppNotification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type);
    
    /**
     * Find notifications by reference type and reference number
     */
    List<InAppNotification> findByReferenceTypeAndReferenceNumberOrderByCreatedAtDesc(
            String referenceType, String referenceNumber);
    
    /**
     * Find notifications by reference type and reference ID
     */
    List<InAppNotification> findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
            String referenceType, Long referenceId);
    
    // ===== DATE RANGE QUERIES =====
    
    /**
     * Find notifications created between dates
     */
    List<InAppNotification> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find notifications for a user created after a specific date
     */
    List<InAppNotification> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime createdAt);
    
    // ===== MARK AS READ/UNREAD =====
    
    /**
     * Mark a single notification as read
     */
    @Modifying
    @Transactional
    @Query("UPDATE InAppNotification n SET n.isRead = true, n.readAt = :readAt, n.readBy = :readBy " +
           "WHERE n.id = :notificationId")
    void markAsRead(@Param("notificationId") Long notificationId,
                    @Param("readAt") LocalDateTime readAt,
                    @Param("readBy") Long readBy);
    
    /**
     * Mark all notifications for a user as read
     */
    @Modifying
    @Transactional
    @Query("UPDATE InAppNotification n SET n.isRead = true, n.readAt = :readAt, n.readBy = :readBy " +
           "WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadForUser(@Param("userId") Long userId,
                              @Param("readAt") LocalDateTime readAt,
                              @Param("readBy") Long readBy);
    
    /**
     * Mark notifications by type as read for a user
     */
    @Modifying
    @Transactional
    @Query("UPDATE InAppNotification n SET n.isRead = true, n.readAt = :readAt, n.readBy = :readBy " +
           "WHERE n.userId = :userId AND n.type = :type AND n.isRead = false")
    void markByTypeAsReadForUser(@Param("userId") Long userId,
                                  @Param("type") String type,
                                  @Param("readAt") LocalDateTime readAt,
                                  @Param("readBy") Long readBy);
    
    // ===== DELETE/CLEANUP QUERIES =====
    
    /**
     * Delete old read notifications
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM InAppNotification n WHERE n.isRead = true AND n.readAt < :cutoffDate")
    int deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Delete all notifications for a user (e.g., when user account is deleted)
     */
    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
    
    /**
     * Delete expired notifications
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM InAppNotification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    int deleteExpiredNotifications(@Param("now") LocalDateTime now);
    
    // ===== STATISTICS QUERIES =====
    
    /**
     * Get notification statistics for a user
     */
    @Query("SELECT COUNT(n), SUM(CASE WHEN n.isRead = false THEN 1 ELSE 0 END) " +
           "FROM InAppNotification n WHERE n.userId = :userId")
    List<Object[]> getNotificationStats(@Param("userId") Long userId);
    
    /**
     * Get notification counts by type for a user
     */
    @Query("SELECT n.type, COUNT(n) FROM InAppNotification n " +
           "WHERE n.userId = :userId AND n.createdAt >= :since " +
           "GROUP BY n.type")
    List<Object[]> getNotificationCountsByType(@Param("userId") Long userId,
                                                 @Param("since") LocalDateTime since);
    
    // ===== PRIORITY QUERIES =====
    
    /**
     * Get high priority unread notifications for a user
     */
    List<InAppNotification> findByUserIdAndIsReadFalseAndPriorityOrderByCreatedAtDesc(
            Long userId, String priority);
    
    // ===== BULK OPERATIONS =====
    
    /**
     * Delete all notifications for a reference type and reference number
     */
    @Modifying
    @Transactional
    void deleteByReferenceTypeAndReferenceNumber(String referenceType, String referenceNumber);
}