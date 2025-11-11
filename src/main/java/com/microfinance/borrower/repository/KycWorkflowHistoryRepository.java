package com.microfinance.borrower.repository;

import com.microfinance.borrower.entity.KycWorkflowHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycWorkflowHistoryRepository extends JpaRepository<KycWorkflowHistory, Long> {

    /**
     * Find all history entries for a workflow, ordered by transition date (newest first)
     */
    List<KycWorkflowHistory> findByKycWorkflowIdOrderByTransitionDateDesc(Long kycWorkflowId);

    /**
     * Find history entries for a workflow, ordered by transition date (oldest first)
     */
    List<KycWorkflowHistory> findByKycWorkflowIdOrderByTransitionDateAsc(Long kycWorkflowId);

    /**
     * Find the first 5 recent history entries for a workflow, ordered by transition date (newest first)
     */
    List<KycWorkflowHistory> findTop5ByKycWorkflowIdOrderByTransitionDateDesc(Long kycWorkflowId);

    /**
     * Find history entries by from state
     */
    List<KycWorkflowHistory> findByFromState(String fromState);

    /**
     * Find history entries by to state
     */
    List<KycWorkflowHistory> findByToState(String toState);

    /**
     * Find history entries by performer
     */
    List<KycWorkflowHistory> findByPerformedBy(Long performedBy);

    /**
     * Find the last transition for a workflow
     */
    @Query("SELECT h FROM KycWorkflowHistory h WHERE h.kycWorkflow.id = :workflowId ORDER BY h.transitionDate DESC LIMIT 1")
    Optional<KycWorkflowHistory> findLatestTransitionByWorkflowId(@Param("workflowId") Long workflowId);

    /**
     * Count transitions by action type
     */
    @Query("SELECT h.actionPerformed, COUNT(h) FROM KycWorkflowHistory h GROUP BY h.actionPerformed")
    List<Object[]> countTransitionsByAction();

    /**
     * Find transitions within a date range
     */
    List<KycWorkflowHistory> findByTransitionDateBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    /**
     * Find history entries with pagination support
     */
    @Query("SELECT h FROM KycWorkflowHistory h WHERE h.kycWorkflow.id = :workflowId ORDER BY h.transitionDate DESC")
    List<KycWorkflowHistory> findByKycWorkflowIdWithPagination(@Param("workflowId") Long workflowId, org.springframework.data.domain.Pageable pageable);
}