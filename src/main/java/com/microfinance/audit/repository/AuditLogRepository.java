// repository/AuditLogRepository.java
package com.microfinance.audit.repository;

import com.microfinance.audit.entity.AuditLog;
import com.microfinance.reports.dto.DataChangeDto;
import com.microfinance.reports.dto.SecurityEventDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    // Basic queries
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
    
    List<AuditLog> findByUserIdOrderByTimestampDesc(Long userId);
    
    List<AuditLog> findByAction(String action);
    
    List<AuditLog> findBySeverity(String severity);
    
    Page<AuditLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Count queries for reports
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate")
    Long countByTimestampBetween(@Param("startDate") LocalDateTime startDate, 
                                 @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :action AND a.timestamp BETWEEN :startDate AND :endDate")
    Long countByActionAndTimestampBetween(@Param("action") String action,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.severity = :severity AND a.timestamp BETWEEN :startDate AND :endDate")
    Long countBySeverityAndTimestampBetween(@Param("severity") String severity,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.status = 'FAILURE' AND a.timestamp BETWEEN :startDate AND :endDate")
    Long countFailuresInPeriod(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action LIKE '%LOGIN%' AND a.timestamp BETWEEN :startDate AND :endDate")
    Integer countLoginsInPeriod(@Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action LIKE '%LOGIN%' AND a.status = 'FAILURE' AND a.timestamp BETWEEN :startDate AND :endDate")
    Integer countFailedLoginsInPeriod(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
    
    // Transaction counts for reports
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action IN ('CREATE', 'UPDATE', 'DELETE', 'APPROVE', 'REJECT') " +
           "AND a.timestamp BETWEEN :startDate AND :endDate")
    Integer countTransactionsInPeriod(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = 'REPORT_GENERATED' AND a.timestamp BETWEEN :startDate AND :endDate")
    Long countReportGenerations(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);
    
    // Security events
    @Query("SELECT a FROM AuditLog a WHERE a.severity IN ('WARNING', 'ERROR', 'CRITICAL') " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentSecurityEvents(Pageable pageable);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.severity IN ('WARNING', 'ERROR', 'CRITICAL') " +
           "AND a.timestamp BETWEEN :startDate AND :endDate")
    Integer countSecurityEventsInPeriod(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.severity = 'CRITICAL' " +
           "AND a.timestamp BETWEEN :startDate AND :endDate")
    Integer countCriticalSecurityEventsInPeriod(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
    
    // User activity statistics
    @Query("SELECT a.userId, a.username, COUNT(a) FROM AuditLog a " +
           "WHERE a.timestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY a.userId, a.username " +
           "ORDER BY COUNT(a) DESC")
    List<Object[]> getUserActivityStats(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        Pageable pageable);


@Query("SELECT a.userId, a.username, " +
        "CASE " +
        "  WHEN u.firstName IS NOT NULL AND u.lastName IS NOT NULL THEN CONCAT(u.firstName, ' ', u.lastName) " +
        "  WHEN u.firstName IS NOT NULL THEN u.firstName " +
        "  WHEN u.lastName IS NOT NULL THEN u.lastName " +
        "  ELSE u.username " +
        "END, " +
        "u.email, " +
        "CAST(u.role AS string), " +
        "COUNT(a), " +
        "MAX(a.timestamp), " +
        "MIN(a.timestamp), " +
        "MAX(a.action), " +
        "MAX(a.ipAddress), " +
        "COUNT(DISTINCT a.sessionId) " +
        "FROM AuditLog a " +
        "LEFT JOIN User u ON a.userId = u.id " +
        "WHERE a.timestamp BETWEEN :startDate AND :endDate " +
        "GROUP BY a.userId, a.username, u.firstName, u.lastName, u.username, u.email, u.role " +
        "ORDER BY COUNT(a) DESC")
List<Object[]> getUserActivityStatsWithUserDetails(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate,
                                                   Pageable pageable);


    /**
     * Get user activity statistics without pagination (all users)
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of objects [userId, username, actionCount]
     */
    @Query("SELECT a.userId, a.username, COUNT(a) FROM AuditLog a " +
            "WHERE a.timestamp BETWEEN :startDate AND :endDate " +
            "GROUP BY a.userId, a.username " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> getUserActivityStatsAll(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    
    // Most viewed report
    @Query("SELECT a.resource, COUNT(a) FROM AuditLog a " +
           "WHERE a.action = 'REPORT_VIEWED' " +
           "GROUP BY a.resource " +
           "ORDER BY COUNT(a) DESC")
    List<Object[]> getMostViewedReport();
    
    // Average report generation time
    @Query("SELECT AVG(a.durationMs) FROM AuditLog a " +
           "WHERE a.action = 'REPORT_GENERATED' " +
           "AND a.timestamp BETWEEN :startDate AND :endDate")
    Double getAverageReportGenerationTime(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
    
    // Reports by type
    @Query("SELECT a.resource, COUNT(a) FROM AuditLog a " +
           "WHERE a.action = 'REPORT_GENERATED' " +
           "AND a.timestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY a.resource")
    List<Object[]> getReportsByType(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);
    
    // Exports by format
    @Query("SELECT a.details, COUNT(a) FROM AuditLog a " +
           "WHERE a.action = 'REPORT_EXPORTED' " +
           "AND a.timestamp BETWEEN :startDate AND :endDate " +
           "GROUP BY a.details")
    List<Object[]> getExportsByFormat(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);
    
    // Data changes tracking
    @Query("SELECT a FROM AuditLog a WHERE a.action IN ('CREATE', 'UPDATE', 'DELETE') " +
           "AND a.timestamp >= :since " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentDataChanges(@Param("since") LocalDateTime since, Pageable pageable);
  /*
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action IN ('CREATE', 'UPDATE', 'DELETE') " +
           "AND a.timestamp BETWEEN :startDate AND :endDate")
    Integer countDataChangesInPeriod(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);
*/
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE " +
            "(UPPER(a.action) LIKE UPPER('%CREATE%') OR " +
            "UPPER(a.action) LIKE UPPER('%UPDATE%') OR " +
            "UPPER(a.action) LIKE UPPER('%DELETE%')) " +
            "AND a.timestamp BETWEEN :startDate AND :endDate")
    Integer countDataChangesInPeriod(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);
    
    // Filtered queries
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:entityId IS NULL OR a.entityId = :entityId) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:severity IS NULL OR a.severity = :severity) AND " +
           "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR a.timestamp <= :endDate) AND " +
           "(:searchTerm IS NULL OR " +
           "LOWER(a.details) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<AuditLog> findAuditLogsByFilters(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("action") String action,
            @Param("userId") Long userId,
            @Param("severity") String severity,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);


    // Add these methods to AuditLogRepository.java

// ==================== DTO RETURNING METHODS ====================

    /**
     * Get recent security events as DTOs
     * Returns security events with severity WARNING, ERROR, or CRITICAL from the last 30 days
     *
     * @return List of SecurityEventDto
     */
    @Query("SELECT NEW com.microfinance.reports.dto.SecurityEventDto(" +
            "a.id, " +
            "a.action, " +
            "a.severity, " +
            "a.details, " +
            "a.timestamp, " +
            "a.userId, " +
            "a.username, " +
            "a.ipAddress, " +
            "a.userAgent, " +
            "a.resource, " +
            "a.details) " +
            "FROM AuditLog a " +
            "WHERE a.severity IN ('WARNING', 'ERROR', 'CRITICAL') " +
            "AND a.timestamp >= :since " +
            "ORDER BY a.timestamp DESC")
    List<SecurityEventDto> getRecentSecurityEvents(@Param("since") LocalDateTime since);

    /**
     * Get recent security events with limit
     *
     * @param since Date to start from
     */
     //          @param limit Maximum number of results
    /*
     * @return List of SecurityEventDto
     */
    @Query("SELECT NEW com.microfinance.reports.dto.SecurityEventDto(" +
            "a.id, " +
            "a.action, " +
            "a.severity, " +
            "a.details, " +
            "a.timestamp, " +
            "a.userId, " +
            "a.username, " +
            "a.ipAddress, " +
            "a.userAgent, " +
            "a.resource, " +
            "a.details) " +
            "FROM AuditLog a " +
            "WHERE a.severity IN ('WARNING', 'ERROR', 'CRITICAL') " +
            "AND a.timestamp >= :since " +
            "ORDER BY a.timestamp DESC")
    List<SecurityEventDto> getRecentSecurityEventsWithLimit(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Get recent data changes as DTOs
     * Returns CREATE, UPDATE, DELETE actions from the last 30 days
     *
     * @return List of DataChangeDto
     */
    /*
    @Query("SELECT NEW com.microfinance.reports.dto.DataChangeDto(" +
            "a.id, " +
            "a.entityType, " +
            "a.entityId, " +
            "CASE WHEN a.action IN ('CREATE', 'UPDATE', 'DELETE') THEN a.action ELSE 'UPDATE' END, " +
            "NULL, " + // fieldName - extract from details if needed
            "a.oldValue, " +
            "a.newValue, " +
            "a.username, " +
            "a.userId, " +
            "a.timestamp, " +
            "a.details) " +
            "FROM AuditLog a " +
            "WHERE a.action IN ('CREATE', 'UPDATE', 'DELETE') " +
            "AND a.timestamp >= :since " +
            "ORDER BY a.timestamp DESC")
    List<DataChangeDto> getRecentDataChanges(@Param("since") LocalDateTime since);

*/

    @Query("SELECT NEW com.microfinance.reports.dto.DataChangeDto(" +
            "a.id, " +
            "a.entityType, " +
            "a.entityId, " +
            "CASE " +
            "  WHEN a.action LIKE '%CREATE%' THEN 'CREATE' " +
            "  WHEN a.action LIKE '%UPDATE%' OR a.action LIKE '%UPDATED%' THEN 'UPDATE' " +
            "  WHEN a.action LIKE '%DELETE%' OR a.action LIKE '%DELETED%' THEN 'DELETE' " +
            "  WHEN a.action LIKE '%APPROVE%' THEN 'APPROVE' " +
            "  ELSE 'UPDATE' " +
            "END, " +
            "CASE " +
            "  WHEN a.details LIKE '%kycStatus%' THEN 'kycStatus' " +
            "  WHEN a.details LIKE '%status%' THEN 'status' " +
            "  WHEN a.details LIKE '%password%' THEN 'password' " +
            "  ELSE NULL " +
            "END, " + // fieldName extracted from details
            "a.oldValue, " +
            "a.newValue, " +
            "a.username, " +
            "a.userId, " +
            "a.timestamp, " +
            "a.details) " +
            "FROM AuditLog a " +
            "WHERE (a.action LIKE '%CREATE%' OR " +
            "       a.action LIKE '%UPDATE%' OR " +
            "       a.action LIKE '%UPDATED%' OR " +
            "       a.action LIKE '%DELETE%' OR " +
            "       a.action LIKE '%DELETED%' OR " +
            "       a.details LIKE '%created%' OR " +
            "       a.details LIKE '%updated%' OR " +
            "       a.details LIKE '%deleted%') " +
            "AND a.timestamp >= :since " +
            "ORDER BY a.timestamp DESC")
    List<DataChangeDto> getRecentDataChanges(@Param("since") LocalDateTime since);

    /**
     * Get recent data changes with limit
     *
     * @param since Date to start from
     *
     // @param limit Maximum number of results
     *
     * @return List of DataChangeDto
     */
    @Query("SELECT NEW com.microfinance.reports.dto.DataChangeDto(" +
            "a.id, " +
            "a.entityType, " +
            "a.entityId, " +
            "CASE WHEN a.action IN ('CREATE', 'UPDATE', 'DELETE') THEN a.action ELSE 'UPDATE' END, " +
            "NULL, " +
            "a.oldValue, " +
            "a.newValue, " +
            "a.username, " +
            "a.userId, " +
            "a.timestamp, " +
            "a.details) " +
            "FROM AuditLog a " +
            "WHERE a.action IN ('CREATE', 'UPDATE', 'DELETE') " +
            "AND a.timestamp >= :since " +
            "ORDER BY a.timestamp DESC")
    List<DataChangeDto> getRecentDataChangesWithLimit(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Get recent data changes for a specific entity
     *
     * @param entityType Type of entity (LOAN, BORROWER, etc.)
     * @param entityId Entity ID
     * @param since Date to start from
     * @return List of DataChangeDto for specific entity
     */
    @Query("SELECT NEW com.microfinance.reports.dto.DataChangeDto(" +
            "a.id, " +
            "a.entityType, " +
            "a.entityId, " +
            "CASE WHEN a.action IN ('CREATE', 'UPDATE', 'DELETE') THEN a.action ELSE 'UPDATE' END, " +
            "NULL, " +
            "a.oldValue, " +
            "a.newValue, " +
            "a.username, " +
            "a.userId, " +
            "a.timestamp, " +
            "a.details) " +
            "FROM AuditLog a " +
            "WHERE a.entityType = :entityType " +
            "AND a.entityId = :entityId " +
            "AND a.action IN ('CREATE', 'UPDATE', 'DELETE') " +
            "AND a.timestamp >= :since " +
            "ORDER BY a.timestamp DESC")
    List<DataChangeDto> getEntityDataChanges(@Param("entityType") String entityType,
                                             @Param("entityId") Long entityId,
                                             @Param("since") LocalDateTime since);

    /**
     * Get security events by severity
     *
     * @param severity Severity level (INFO, WARNING, ERROR, CRITICAL)
     * @param since Date to start from
     * @return List of SecurityEventDto
     */
    @Query("SELECT NEW com.microfinance.reports.dto.SecurityEventDto(" +
            "a.id, " +
            "a.action, " +
            "a.severity, " +
            "a.details, " +
            "a.timestamp, " +
            "a.userId, " +
            "a.username, " +
            "a.ipAddress, " +
            "a.userAgent, " +
            "a.resource, " +
            "a.details) " +
            "FROM AuditLog a " +
            "WHERE a.severity = :severity " +
            "AND a.timestamp >= :since " +
            "ORDER BY a.timestamp DESC")
    List<SecurityEventDto> getSecurityEventsBySeverity(@Param("severity") String severity,
                                                       @Param("since") LocalDateTime since);

    /**
     * Get last active timestamp for a user in a date range
     *
     * @param userId User ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Last active timestamp
     */
    @Query("SELECT MAX(a.timestamp) FROM AuditLog a " +
            "WHERE a.userId = :userId " +
            "AND a.timestamp BETWEEN :startDate AND :endDate")
    LocalDateTime findMaxTimestampByUserAndPeriod(@Param("userId") Long userId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);


}