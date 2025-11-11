package com.microfinance.borrower.repository;

import com.microfinance.borrower.entity.KycWorkflowStepStatus;
import com.microfinance.borrower.enums.KycWorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycWorkflowStepStatusRepository extends JpaRepository<KycWorkflowStepStatus, Long> {
    
    /**
     * Find all step statuses for a workflow
     */
    List<KycWorkflowStepStatus> findByKycWorkflowId(Long kycWorkflowId);
    
    /**
     * Find step status by workflow and step
     */
    Optional<KycWorkflowStepStatus> findByKycWorkflowIdAndStep(Long kycWorkflowId, KycWorkflowStep step);
    
    /**
     * Find step statuses by status
     */
    List<KycWorkflowStepStatus> findByKycWorkflowIdAndStatus(Long kycWorkflowId, KycWorkflowStepStatus.StepStatus status);
    
    /**
     * Find overdue step statuses
     */
    @Query("SELECT s FROM KycWorkflowStepStatus s WHERE s.dueDate < CURRENT_TIMESTAMP AND s.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<KycWorkflowStepStatus> findOverdueSteps();
    
    /**
     * Find step statuses by status
     */
    List<KycWorkflowStepStatus> findByStatus(KycWorkflowStepStatus.StepStatus status);
    
    /**
     * Count steps by status for a workflow
     */
    @Query("SELECT s.status, COUNT(s) FROM KycWorkflowStepStatus s WHERE s.kycWorkflow.id = :workflowId GROUP BY s.status")
    List<Object[]> countStepsByStatus(@Param("workflowId") Long workflowId);
    
    /**
     * Find completed steps for a workflow
     */
    @Query("SELECT s FROM KycWorkflowStepStatus s WHERE s.kycWorkflow.id = :workflowId AND s.status = 'COMPLETED' ORDER BY s.completedAt DESC")
    List<KycWorkflowStepStatus> findCompletedStepsByWorkflowId(@Param("workflowId") Long workflowId);
    
    /**
     * Find pending steps for a workflow - FIXED QUERY
     */
    @Query("SELECT s FROM KycWorkflowStepStatus s WHERE s.kycWorkflow.id = :workflowId AND s.status = 'PENDING'")
    List<KycWorkflowStepStatus> findPendingStepsByWorkflowId(@Param("workflowId") Long workflowId);
    
    /**
     * Check if all required steps are completed for a workflow
     */
    @Query("SELECT CASE WHEN COUNT(s) = 0 THEN true ELSE false END FROM KycWorkflowStepStatus s WHERE s.kycWorkflow.id = :workflowId AND s.isRequired = true AND s.status != 'COMPLETED'")
    Boolean areAllRequiredStepsCompleted(@Param("workflowId") Long workflowId);
    
    /**
     * Find steps by due date range
     */
    List<KycWorkflowStepStatus> findByDueDateBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);
    
    /**
     * Find steps completed by a specific user
     */
    List<KycWorkflowStepStatus> findByCompletedBy(Long completedBy);
    
    /**
     * Find steps by workflow ID and step status - simplified version
     */
    List<KycWorkflowStepStatus> findByKycWorkflowIdAndStatusOrderById(Long kycWorkflowId, KycWorkflowStepStatus.StepStatus status);
}