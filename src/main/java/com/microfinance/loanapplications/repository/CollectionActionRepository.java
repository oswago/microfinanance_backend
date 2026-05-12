package com.microfinance.loanapplications.repository;

import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanapplications.entity.CollectionAction;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.entity.RecoveryCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionActionRepository extends JpaRepository<CollectionAction, Long> {
    
    List<CollectionAction> findByLoanIdOrderByActionDateDesc(Long loanId);
    
    Page<CollectionAction> findByLoanId(Long loanId, Pageable pageable);

    List<CollectionAction> findByLoan(Loan loan);

    List<CollectionAction> findByRecoveryCase(RecoveryCase recoveryCase);

    List<CollectionAction> findByActionType(GeneralConfig.ActionType actionType);

    List<CollectionAction> findByActionStatus(GeneralConfig.ActionStatus actionStatus);

    List<CollectionAction> findByAssignedToId(Long userId);

    List<CollectionAction> findByPerformedById(Long userId);

    @Query("SELECT DISTINCT ca.performedBy.id FROM CollectionAction ca WHERE ca.loan.id = :loanId")
    List<Long> findDistinctAgentIdsByLoanId(@Param("loanId") Long loanId);

    @Query("SELECT ca FROM CollectionAction ca WHERE ca.actionStatus = 'SCHEDULED' " +
            "AND ca.actionDate >= :today " +
            "ORDER BY ca.actionDate ASC, ca.actionTime ASC")
    List<CollectionAction> findScheduledActions(@Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT ca FROM CollectionAction ca WHERE ca.assignedTo.id = :userId " +
            "AND ca.actionStatus = 'SCHEDULED' " +
            "AND ca.actionDate >= :today " +
            "ORDER BY ca.actionDate ASC, ca.actionTime ASC")
    List<CollectionAction> findScheduledActionsForUser(@Param("userId") Long userId,
                                                       @Param("today") LocalDate today,
                                                       Pageable pageable);

    @Query("SELECT ca FROM CollectionAction ca WHERE ca.loan.branch.id = :branchId " +
            "AND ca.actionStatus = 'SCHEDULED' " +
            "AND ca.actionDate >= :today " +
            "ORDER BY ca.actionDate ASC, ca.actionTime ASC")
    List<CollectionAction> findScheduledActionsForBranch(@Param("branchId") Long branchId,
                                                         @Param("today") LocalDate today,
                                                         Pageable pageable);

        @Query("SELECT ca FROM CollectionAction ca WHERE ca.followUpDate <= :date " +
                "AND ca.actionStatus != 'COMPLETED' " +
                "ORDER BY ca.followUpDate ASC")
        List<CollectionAction> findOverdueFollowUps(@Param("date") LocalDate date);

        @Query("SELECT ca FROM CollectionAction ca WHERE ca.loan.id = :loanId " +
                "AND ca.promiseDate IS NOT NULL " +
                "AND ca.paymentConfirmed = false " +
                "ORDER BY ca.promiseDate ASC")
        List<CollectionAction> findPendingPromises(@Param("loanId") Long loanId);

        @Query("SELECT ca FROM CollectionAction ca WHERE ca.loan.id = :loanId " +
                "AND ca.actionType = 'PROMISE_TO_PAY' " +
                "AND ca.paymentConfirmed = false " +
                "AND ca.promiseDate < :currentDate")
        List<CollectionAction> findBrokenPromises(@Param("loanId") Long loanId,
                                                  @Param("currentDate") LocalDate currentDate);

        @Query("SELECT COUNT(ca) FROM CollectionAction ca WHERE ca.loan.id = :loanId " +
                "AND ca.actionType = :actionType")
        Long countActionsByType(@Param("loanId") Long loanId,
                                @Param("actionType") GeneralConfig.ActionType actionType);

        @Query("SELECT ca FROM CollectionAction ca WHERE ca.assignedTo.id = :userId " +
                "AND ca.actionDate BETWEEN :startDate AND :endDate")
        List<CollectionAction> findByUserAndDateRange(@Param("userId") Long userId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

        @Query("SELECT ca FROM CollectionAction ca WHERE ca.loan.id IN :loanIds " +
                "AND ca.actionDate >= :sinceDate " +
                "ORDER BY ca.actionDate DESC")
        List<CollectionAction> findRecentActionsForLoans(@Param("loanIds") List<Long> loanIds,
                                                         @Param("sinceDate") LocalDate sinceDate);

        @Modifying
        @Query("UPDATE CollectionAction ca SET ca.paymentConfirmed = true " +
                "WHERE ca.loan.id = :loanId AND ca.promiseDate = :promiseDate")
        int confirmPromisePayment(@Param("loanId") Long loanId,
                                  @Param("promiseDate") LocalDate promiseDate);

        Optional<CollectionAction> findFirstByLoanIdOrderByActionDateDesc(Long loanId);



    /**
     * Find collection actions by performed by user ID and date range
     */
    @Query("SELECT ca FROM CollectionAction ca WHERE ca.performedBy.id = :userId " +
            "AND ca.actionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY ca.actionDate DESC")
    List<CollectionAction> findByPerformedByIdAndActionDateBetween(@Param("userId") Long userId,
                                                                   @Param("startDate") LocalDate startDate,
                                                                   @Param("endDate") LocalDate endDate);

    /**
     * Find collection actions by date range
     */
    @Query("SELECT ca FROM CollectionAction ca WHERE ca.actionDate BETWEEN :startDate AND :endDate")
    List<CollectionAction> findByActionDateBetween(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    /**
     * Count actions by outcome for a specific officer
     */
    @Query("SELECT ca.outcome, COUNT(ca) FROM CollectionAction ca " +
            "WHERE ca.performedBy.id = :officerId " +
            "AND ca.actionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY ca.outcome")
    List<Object[]> countActionsByOutcome(@Param("officerId") Long officerId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    /**
     * Get daily action counts for an officer
     */
    @Query("SELECT FUNCTION('DATE', ca.actionDate), COUNT(ca) FROM CollectionAction ca " +
            "WHERE ca.performedBy.id = :officerId " +
            "AND ca.actionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('DATE', ca.actionDate) " +
            "ORDER BY FUNCTION('DATE', ca.actionDate)")
    List<Object[]> getDailyActionCounts(@Param("officerId") Long officerId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);


    @Query("SELECT ca FROM CollectionAction ca WHERE ca.performedBy.id = :userId " +
            "AND ca.actionType = :actionType " +
            "AND ca.actionDate = :date")
    Integer countByPerformedByIdAndActionTypeAndActionDate(@Param("userId") Long userId,
                                                           @Param("actionType") GeneralConfig.ActionType actionType,
                                                           @Param("date") LocalDate date);

    @Query("SELECT ca FROM CollectionAction ca WHERE ca.loan.branch.id = :branchId " +
            "ORDER BY ca.createdAt DESC")
    List<CollectionAction> findRecentActionsByBranch(@Param("branchId") Long branchId,
                                                     Pageable pageable);

    List<CollectionAction> findAllByOrderByCreatedAtDesc(Pageable pageable);


    // ==================== New Methods for Recovery Workflow ====================

    /**
     * Find top N recent actions for a specific loan ordered by action date descending
     */
    @Query("SELECT ca FROM CollectionAction ca WHERE ca.loan.id = :loanId " +
            "ORDER BY ca.actionDate DESC, ca.actionTime DESC")
    List<CollectionAction> findTop5ByLoanIdOrderByActionDateDesc(@Param("loanId") Long loanId, Pageable pageable);

    /**
     * Convenience method to get top 5 recent actions for a loan
     */
    default List<CollectionAction> findTop5ByLoanIdOrderByActionDateDesc(Long loanId) {
        return findTop5ByLoanIdOrderByActionDateDesc(loanId, PageRequest.of(0, 5));
    }

    /**
     * Count total collection actions for a specific loan
     */
    @Query("SELECT COUNT(ca) FROM CollectionAction ca WHERE ca.loan.id = :loanId")
    Long countByLoanId(@Param("loanId") Long loanId);

    /**
     * Count actions by loan and action type
     */
    @Query("SELECT COUNT(ca) FROM CollectionAction ca WHERE ca.loan.id = :loanId AND ca.actionType = :actionType")
    Long countByLoanIdAndActionType(@Param("loanId") Long loanId,
                                    @Param("actionType") GeneralConfig.ActionType actionType);

    /**
     * Find actions by loan ID with pagination
     */
    @Query("SELECT ca FROM CollectionAction ca WHERE ca.loan.id = :loanId " +
            "ORDER BY ca.actionDate DESC, ca.actionTime DESC")
    List<CollectionAction> findByLoanIdWithPagination(@Param("loanId") Long loanId, Pageable pageable);


    /**
     * Count actions by outcome for a specific loan
     */
    @Query("SELECT ca.outcome, COUNT(ca) FROM CollectionAction ca " +
            "WHERE ca.loan.id = :loanId " +
            "GROUP BY ca.outcome")
    List<Object[]> countActionsByOutcomeForLoan(@Param("loanId") Long loanId);

    /**
     * Find recent actions for a loan (last 30 days)
     */
    @Query("SELECT ca FROM CollectionAction ca WHERE ca.loan.id = :loanId " +
            "AND ca.actionDate >= :sinceDate " +
            "ORDER BY ca.actionDate DESC")
    List<CollectionAction> findRecentActionsByLoanId(@Param("loanId") Long loanId,
                                                     @Param("sinceDate") LocalDate sinceDate);

    /**
     * Count successful actions (that led to payment) for a loan
     */
    @Query("SELECT COUNT(ca) FROM CollectionAction ca WHERE ca.loan.id = :loanId " +
            "AND ca.outcome IN ('PROMISED_TO_PAY', 'FULL_PAYMENT', 'PARTIAL_PAYMENT')")
    Long countSuccessfulActionsByLoanId(@Param("loanId") Long loanId);

    /**
     * Get the latest action for a loan
     */
    @Query("SELECT ca FROM CollectionAction ca WHERE ca.loan.id = :loanId " +
            "ORDER BY ca.actionDate DESC, ca.actionTime DESC")
    Optional<CollectionAction> findLatestByLoanId(@Param("loanId") Long loanId);

    /**
     * Get actions grouped by day for a loan (for trend analysis)
     */
    @Query("SELECT FUNCTION('DATE', ca.actionDate), COUNT(ca), COALESCE(SUM(ca.promiseAmount), 0) " +
            "FROM CollectionAction ca WHERE ca.loan.id = :loanId " +
            "AND ca.actionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('DATE', ca.actionDate) " +
            "ORDER BY FUNCTION('DATE', ca.actionDate)")
    List<Object[]> getDailyActionSummary(@Param("loanId") Long loanId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT MAX(ca.actionDate) FROM CollectionAction ca WHERE ca.loan.id = :loanId")
    Optional<LocalDate> findLastContactDateByLoanId(@Param("loanId") Long loanId);



    @Query("SELECT ca FROM CollectionAction ca WHERE ca.loan.id = :loanId AND ca.actionType = :actionType ORDER BY ca.actionDate DESC")
    List<CollectionAction> findLatestByLoanIdAndActionType(@Param("loanId") Long loanId,
                                                           @Param("actionType") GeneralConfig.ActionType actionType);

    @Query("SELECT ca FROM CollectionAction ca WHERE ca.notes LIKE %:searchText% OR ca.contactPerson LIKE %:searchText%")
    List<CollectionAction> searchByNotesOrContactPerson(@Param("searchText") String searchText);

    @Query("SELECT ca FROM CollectionAction ca WHERE ca.loan.id = :loanId AND ca.actionType = :actionType AND ca.actionDate >= :fromDate")
    List<CollectionAction> findByLoanIdAndActionTypeAfterDate(@Param("loanId") Long loanId,
                                                              @Param("actionType") GeneralConfig.ActionType actionType,
                                                              @Param("fromDate") LocalDate fromDate);

    Optional<CollectionAction> findTopByLoanIdAndActionTypeOrderByActionDateDesc(Long loanId,
                                                                                 GeneralConfig.ActionType actionType);


    // In CollectionActionRepository.java, add this method if you need to search by notes
    @Query("SELECT ca FROM CollectionAction ca WHERE ca.notes LIKE %:keyword%")
    List<CollectionAction> findByNotesContaining(@Param("keyword") String keyword);

    // Or for a more specific search by notice number
    @Query("SELECT ca FROM CollectionAction ca WHERE ca.notes LIKE %:noticeNumber%")
    Optional<CollectionAction> findByNoticeNumberInNotes(@Param("noticeNumber") String noticeNumber);


}