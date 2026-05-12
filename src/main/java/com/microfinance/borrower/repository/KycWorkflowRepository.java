package com.microfinance.borrower.repository;

import com.microfinance.borrower.entity.KycWorkflow;
import com.microfinance.borrower.enums.KycWorkflowState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface KycWorkflowRepository extends JpaRepository<KycWorkflow, Long> {
    
    /**
     * Find KYC workflow by borrower ID
     */
    Optional<KycWorkflow> findByBorrowerId(Long borrowerId);
    
    /**
     * Find all KYC workflows by current state
     */
    List<KycWorkflow> findByCurrentState(KycWorkflowState currentState);
    
    /**
     * Find KYC workflows assigned to a specific officer
     */
    List<KycWorkflow> findByAssignedOfficerId(Long assignedOfficerId);
    
    /**
     * Find KYC workflows by multiple states
     */
    List<KycWorkflow> findByCurrentStateIn(List<KycWorkflowState> states);
    
    /**
     * Find workflows that are not completed (non-terminal states)
     */
    @Query("SELECT k FROM KycWorkflow k WHERE k.currentState NOT IN :terminalStates")
    List<KycWorkflow> findActiveWorkflows(@Param("terminalStates") List<KycWorkflowState> terminalStates);
    
    /**
     * Find workflows that are stuck (started but not completed after cutoff date)
     */
    @Query("SELECT k FROM KycWorkflow k WHERE k.startedAt IS NOT NULL AND k.completedAt IS NULL AND k.startedAt < :cutoffDate")
    List<KycWorkflow> findStuckWorkflows(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find workflows that are overdue (past estimated completion date)
     */
    @Query("SELECT k FROM KycWorkflow k WHERE k.estimatedCompletionDate IS NOT NULL AND k.estimatedCompletionDate < :currentDate AND k.completedAt IS NULL")
    List<KycWorkflow> findOverdueWorkflows(@Param("currentDate") LocalDateTime currentDate);
    
    /**
     * Get workflow statistics grouped by state
     */
    @Query("SELECT k.currentState, COUNT(k) FROM KycWorkflow k GROUP BY k.currentState")
    List<Object[]> getWorkflowStatisticsByState();
    
    /**
     * Get workflow statistics by branch
     */
    @Query("SELECT k.borrower.branch.name, k.currentState, COUNT(k) FROM KycWorkflow k GROUP BY k.borrower.branch.name, k.currentState")
    List<Object[]> getWorkflowStatisticsByBranch();
    
    /**
     * Count workflows by state
     */
    Long countByCurrentState(KycWorkflowState currentState);
    
    /**
     * Count active workflows (non-terminal states)
     */
    @Query("SELECT COUNT(k) FROM KycWorkflow k WHERE k.currentState NOT IN :terminalStates")
    Long countActiveWorkflows(@Param("terminalStates") List<KycWorkflowState> terminalStates);
    
    /**
     * Find workflows created within a date range
     */
    List<KycWorkflow> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find workflows completed within a date range
     */
    List<KycWorkflow> findByCompletedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Check if a borrower has an active KYC workflow
     */
    @Query("SELECT CASE WHEN COUNT(k) > 0 THEN true ELSE false END FROM KycWorkflow k WHERE k.borrower.id = :borrowerId AND k.currentState NOT IN :terminalStates")
    Boolean existsActiveWorkflowByBorrowerId(@Param("borrowerId") Long borrowerId, @Param("terminalStates") List<KycWorkflowState> terminalStates);
    
    /**
     * Find workflows by borrower IDs
     */
    List<KycWorkflow> findByBorrowerIdIn(List<Long> borrowerIds);
    
    /**
     * Get average completion time for completed workflows
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, k.startedAt, k.completedAt)) FROM KycWorkflow k WHERE k.completedAt IS NOT NULL")
    Double getAverageCompletionTimeInSeconds();
    
    /**
     * Find workflows that need attention (pending for too long)
     */
    @Query("SELECT k FROM KycWorkflow k WHERE k.currentState = :state AND k.updatedAt < :thresholdDate")
    List<KycWorkflow> findWorkflowsNeedingAttention(@Param("state") KycWorkflowState state, 
                                                   @Param("thresholdDate") LocalDateTime thresholdDate);

    @Query("SELECT w FROM KycWorkflow w LEFT JOIN FETCH w.stepStatuses WHERE w.borrower.id = :borrowerId")
    Optional<KycWorkflow> findByBorrowerIdWithSteps(@Param("borrowerId") Long borrowerId);

}