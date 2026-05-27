package com.microfinance.loanapplications.repository;

import com.microfinance.loanapplications.entity.RecoveryCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecoveryCaseRepository extends JpaRepository<RecoveryCase, Long> {

    Optional<RecoveryCase> findByCaseNumber(String caseNumber);

    boolean existsByLoanId(Long loanId);

    List<RecoveryCase> findByCurrentStage(String stage);

    List<RecoveryCase> findByAssignedAgentId(Long agentId);

    @Query("SELECT rc FROM RecoveryCase rc WHERE " +
            "(:search IS NULL OR LOWER(rc.caseNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(rc.borrower.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(rc.borrower.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(rc.loan.loanAccountNumber) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:status IS NULL OR rc.status = :status) AND " +
            "(:stage IS NULL OR rc.currentStage = :stage) AND " +
            "(:priority IS NULL OR rc.priority = :priority) AND " +
            "(:assignedTo IS NULL OR rc.assignedAgent.id = :assignedTo)")
    Page<RecoveryCase> findAllWithFiltersORG(@Param("search") String search,
                                          @Param("status") String status,
                                          @Param("stage") String stage,
                                          @Param("priority") String priority,
                                          @Param("assignedTo") Long assignedTo,
                                          Pageable pageable);



    @Query("SELECT rc FROM RecoveryCase rc " +
            "LEFT JOIN FETCH rc.borrower b " +
            "LEFT JOIN FETCH rc.loan l " +
            "WHERE (:search IS NULL OR :search = '' OR " +
            "      LOWER(rc.caseNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "      LOWER(b.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "      LOWER(b.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "      LOWER(l.loanAccountNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR :status = '' OR rc.status = :status) " +
            "AND (:stage IS NULL OR :stage = '' OR rc.currentStage = :stage) " +
            "AND (:priority IS NULL OR :priority = '' OR rc.priority = :priority) " +
            "AND (:assignedTo IS NULL OR rc.assignedAgent.id = :assignedTo)")
    Page<RecoveryCase> findAllWithFilters(@Param("search") String search,
                                          @Param("status") String status,
                                          @Param("stage") String stage,
                                          @Param("priority") String priority,
                                          @Param("assignedTo") Long assignedTo,
                                          Pageable pageable);





    @Query("SELECT rc.loan.id FROM RecoveryCase rc WHERE rc.status != 'CLOSED'")
    List<Long> findAllLoanIdsWithActiveCases();

    @Query("SELECT COUNT(rc) FROM RecoveryCase rc WHERE rc.loan.id = :loanId")
    int countByLoanId(@Param("loanId") Long loanId);

    // Add to RecoveryCaseRepository
    Optional<RecoveryCase> findByLoanId(Long loanId);


// Add these methods to RecoveryCaseRepository.java

// ==================== LEGAL STATISTICS METHODS ====================

    /**
     * Count recovery cases by status
     *
     * @param status Case status (ACTIVE, CLOSED, ESCALATED, etc.)
     * @return Count of cases with given status
     */
    @Query("SELECT COUNT(rc) FROM RecoveryCase rc WHERE rc.status = :status")
    Long countByStatus(@Param("status") String status);

    /**
     * Count recovery cases by current stage
     *
     * @param stage Current stage (INITIAL_CONTACT, LEGAL_NOTICE, COURT_CASE, etc.)
     * @return Count of cases at given stage
     */
    @Query("SELECT COUNT(rc) FROM RecoveryCase rc WHERE rc.currentStage = :stage")
    Long countByCurrentStage(@Param("stage") String stage);

    /**
     * Count recovery cases by priority
     *
     * @param priority Case priority (LOW, MEDIUM, HIGH, CRITICAL)
     * @return Count of cases with given priority
     */
    @Query("SELECT COUNT(rc) FROM RecoveryCase rc WHERE rc.priority = :priority")
    Long countByPriority(@Param("priority") String priority);

    /**
     * Get recovery case statistics by stage
     *
     * @return List of objects [stage, count, totalOutstandingAmount, averageRecoveryRate]
     */
    @Query("SELECT rc.currentStage, " +
            "COUNT(rc), " +
            "COALESCE(SUM(rc.outstandingAmount), 0), " +
            "COALESCE(AVG(rc.recoveryRate), 0) " +
            "FROM RecoveryCase rc " +
            "GROUP BY rc.currentStage")
    List<Object[]> getStatisticsByStage();

    /**
     * Get recovery case statistics by status
     *
     * @return List of objects [status, count, totalOutstandingAmount, totalRecoveredAmount]
     */
    @Query("SELECT rc.status, " +
            "COUNT(rc), " +
            "COALESCE(SUM(rc.outstandingAmount), 0), " +
            "COALESCE(SUM(rc.recoveredAmount), 0) " +
            "FROM RecoveryCase rc " +
            "GROUP BY rc.status")
    List<Object[]> getStatisticsByStatus();

    /**
     * Get recovery case statistics by priority
     *
     * @return List of objects [priority, count, totalOutstandingAmount]
     */
    @Query("SELECT rc.priority, " +
            "COUNT(rc), " +
            "COALESCE(SUM(rc.outstandingAmount), 0) " +
            "FROM RecoveryCase rc " +
            "GROUP BY rc.priority")
    List<Object[]> getStatisticsByPriority();

    /**
     * Get total outstanding amount for all active recovery cases
     *
     * @return Total outstanding amount
     */
    @Query("SELECT COALESCE(SUM(rc.outstandingAmount), 0) FROM RecoveryCase rc WHERE rc.status != 'CLOSED'")
    BigDecimal getTotalOutstandingAmount();

    /**
     * Get total recovered amount for all recovery cases
     *
     * @return Total recovered amount
     */
    @Query("SELECT COALESCE(SUM(rc.recoveredAmount), 0) FROM RecoveryCase rc")
    BigDecimal getTotalRecoveredAmount();

    /**
     * Get average recovery rate across all cases
     *
     * @return Average recovery rate percentage
     */
    @Query("SELECT COALESCE(AVG(rc.recoveryRate), 0) FROM RecoveryCase rc")
    Double getAverageRecoveryRate();

    /**
     * Count cases that have reached legal notice stage or beyond
     *
     * @return Count of cases in legal stages
     */
    @Query("SELECT COUNT(rc) FROM RecoveryCase rc WHERE rc.currentStage IN ('LEGAL_NOTICE', 'COURT_CASE', 'ASSET_SEIZURE')")
    Long countCasesInLegalStages();

    /**
     * Count cases that are overdue for action (no activity in last X days)
     *
     *
     // @param daysThreshold Number of days without activity
     * @return Count of stagnant cases
     */
    @Query("SELECT COUNT(rc) FROM RecoveryCase rc WHERE rc.updatedAt <= :cutoffDate AND rc.status != 'CLOSED'")
    Long countStagnantCases(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /**
     * Get recovery cases by agent with statistics
     *
     * @param agentId Agent ID
     * @return List of objects [agentId, agentName, caseCount, totalOutstanding, totalRecovered]
     */
    @Query("SELECT rc.assignedAgent.id, " +
            "CONCAT(rc.assignedAgent.firstName, ' ', rc.assignedAgent.lastName), " +
            "COUNT(rc), " +
            "COALESCE(SUM(rc.outstandingAmount), 0), " +
            "COALESCE(SUM(rc.recoveredAmount), 0) " +
            "FROM RecoveryCase rc " +
            "WHERE rc.assignedAgent.id = :agentId " +
            "GROUP BY rc.assignedAgent.id, rc.assignedAgent.firstName, rc.assignedAgent.lastName")
    List<Object[]> getStatisticsByAgent(@Param("agentId") Long agentId);

    /**
     * Get recovery case trend by month
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of objects [year, month, caseCount, amountRecovered]
     */
    @Query("SELECT YEAR(rc.createdAt), MONTH(rc.createdAt), " +
            "COUNT(rc), " +
            "COALESCE(SUM(rc.recoveredAmount), 0) " +
            "FROM RecoveryCase rc " +
            "WHERE rc.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY YEAR(rc.createdAt), MONTH(rc.createdAt) " +
            "ORDER BY YEAR(rc.createdAt), MONTH(rc.createdAt)")
    List<Object[]> getRecoveryTrendByMonth(@Param("startDate") java.time.LocalDateTime startDate,
                                           @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * Sum amount under litigation (cases in COURT_CASE or LEGAL_NOTICE stage)
     *
     * @return Total amount under litigation
     */
    @Query("SELECT COALESCE(SUM(rc.outstandingAmount), 0) FROM RecoveryCase rc " +
            "WHERE rc.currentStage IN ('COURT_CASE', 'LEGAL_NOTICE') AND rc.status != 'CLOSED'")
    BigDecimal sumAmountUnderLitigation();

    /**
     * Get cases that have been escalated
     *
     * @return List of escalated cases
     */
    @Query("SELECT rc FROM RecoveryCase rc WHERE rc.status = 'ESCALATED' ORDER BY rc.updatedAt DESC")
    List<RecoveryCase> findEscalatedCases();

    /**
     * Count escalated cases
     *
     * @return Count of escalated cases
     */
    @Query("SELECT COUNT(rc) FROM RecoveryCase rc WHERE rc.status = 'ESCALATED'")
    Long countEscalatedCases();

    /**
     * Get cases by priority and stage for prioritization
     *
     * @param priority Priority level
     * @param stage Stage
     * @return List of cases
     */
    @Query("SELECT rc FROM RecoveryCase rc WHERE rc.priority = :priority AND rc.currentStage = :stage AND rc.status != 'CLOSED'")
    List<RecoveryCase> findByPriorityAndStage(@Param("priority") String priority,
                                              @Param("stage") String stage);

    /**
     * Get total recovery amount by agent (for performance tracking)
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of objects [agentId, agentName, totalRecovered, caseCount]
     */
    @Query("SELECT rc.assignedAgent.id, " +
            "CONCAT(rc.assignedAgent.firstName, ' ', rc.assignedAgent.lastName), " +
            "COALESCE(SUM(rc.recoveredAmount), 0), " +
            "COUNT(rc) " +
            "FROM RecoveryCase rc " +
            "WHERE rc.closedDate BETWEEN :startDate AND :endDate " +
            "AND rc.status = 'CLOSED' " +
            "GROUP BY rc.assignedAgent.id, rc.assignedAgent.firstName, rc.assignedAgent.lastName " +
            "ORDER BY SUM(rc.recoveredAmount) DESC")
    List<Object[]> getAgentRecoveryPerformance(@Param("startDate") java.time.LocalDate startDate,
                                               @Param("endDate") java.time.LocalDate endDate);

    /**
     * Get recovery success rate by stage
     *
     * @return List of objects [stage, totalCases, closedCases, successRate]
     */
    @Query("SELECT rc.currentStage, " +
            "COUNT(rc), " +
            "SUM(CASE WHEN rc.status = 'CLOSED' THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(CASE WHEN rc.status = 'CLOSED' THEN rc.recoveredAmount ELSE 0 END), 0) " +
            "FROM RecoveryCase rc " +
            "GROUP BY rc.currentStage")
    List<Object[]> getSuccessRateByStage();

    /**
     * Find cases that are overdue for legal action
     */
     // @param daysThreshold Days without legal action
    /* * @return List of cases needing legal attention
     */
    @Query("SELECT rc FROM RecoveryCase rc WHERE rc.currentStage = 'LEGAL_NOTICE' " +
            "AND rc.updatedAt <= :cutoffDate AND rc.status != 'CLOSED'")
    List<RecoveryCase> findCasesNeedingLegalAction(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);


    // In RecoveryCaseRepository.java - Add these methods

    /**
     * Sum recovered amount from all recovery cases
     * This is the total amount recovered through the recovery process
     */
    @Query("SELECT COALESCE(SUM(rc.recoveredAmount), 0) FROM RecoveryCase rc")
    BigDecimal sumTotalRecoveredAmount();

    /**
     * Sum recovered amount from closed recovery cases
     */
    @Query("SELECT COALESCE(SUM(rc.recoveredAmount), 0) FROM RecoveryCase rc WHERE rc.status = 'CLOSED'")
    BigDecimal sumRecoveredAmountFromClosedCases();

    /**
     * Sum recovered amount from active recovery cases
     */
    @Query("SELECT COALESCE(SUM(rc.recoveredAmount), 0) FROM RecoveryCase rc WHERE rc.status != 'CLOSED'")
    BigDecimal sumRecoveredAmountFromActiveCases();

    /**
     * Sum recovered amount by date range (when case was closed)
     */
    @Query("SELECT COALESCE(SUM(rc.recoveredAmount), 0) FROM RecoveryCase rc " +
            "WHERE rc.closedDate BETWEEN :startDate AND :endDate")
    BigDecimal sumRecoveredAmountByDateRange(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    /**
     * Sum recovered amount by agent
     */
    @Query("SELECT rc.assignedAgent.id, CONCAT(rc.assignedAgent.firstName, ' ', rc.assignedAgent.lastName), " +
            "COALESCE(SUM(rc.recoveredAmount), 0) " +
            "FROM RecoveryCase rc " +
            "WHERE rc.assignedAgent IS NOT NULL " +
            "GROUP BY rc.assignedAgent.id, rc.assignedAgent.firstName, rc.assignedAgent.lastName")
    List<Object[]> sumRecoveredAmountByAgent();

    /**
     * Sum recovered amount by stage
     */
    @Query("SELECT rc.currentStage, COALESCE(SUM(rc.recoveredAmount), 0) " +
            "FROM RecoveryCase rc " +
            "GROUP BY rc.currentStage")
    List<Object[]> sumRecoveredAmountByStage();

    /**
     * Sum recovered amount by priority
     */
    @Query("SELECT rc.priority, COALESCE(SUM(rc.recoveredAmount), 0) " +
            "FROM RecoveryCase rc " +
            "GROUP BY rc.priority")
    List<Object[]> sumRecoveredAmountByPriority();

    /**
     * Get recovery rate summary (total recovered vs total outstanding)
     */
    @Query("SELECT COALESCE(SUM(rc.recoveredAmount), 0), COALESCE(SUM(rc.outstandingAmount), 0) " +
            "FROM RecoveryCase rc")
    List<Object[]> getRecoverySummary();


    /**
     * Get recovery performance by month
     */
    @Query("SELECT YEAR(rc.closedDate), MONTH(rc.closedDate), " +
            "COUNT(rc), COALESCE(SUM(rc.recoveredAmount), 0) " +
            "FROM RecoveryCase rc " +
            "WHERE rc.status = 'CLOSED' " +
            "AND rc.closedDate BETWEEN :startDate AND :endDate " +
            "GROUP BY YEAR(rc.closedDate), MONTH(rc.closedDate) " +
            "ORDER BY YEAR(rc.closedDate), MONTH(rc.closedDate)")
    List<Object[]> getRecoveryPerformanceByMonth(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * Calculate overall recovery rate percentage
     */
    @Query("SELECT CASE WHEN SUM(rc.originalLoanAmount) > 0 THEN " +
            "(COALESCE(SUM(rc.recoveredAmount), 0) * 100 / COALESCE(SUM(rc.originalLoanAmount), 1)) " +
            "ELSE 0 END FROM RecoveryCase rc")
    BigDecimal calculateOverallRecoveryRate();



}