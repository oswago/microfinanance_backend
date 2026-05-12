package com.microfinance.loanapplications.repository;

import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanapplications.entity.ApplicationApproval;
import com.microfinance.loanapplications.entity.ApprovalComment;
import com.microfinance.loanapplications.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApplicationApprovalRepository extends JpaRepository<ApplicationApproval, Long> {
    
    List<ApplicationApproval> findByLoanApplicationIdOrderByApprovalLevelAsc(Long applicationId);
    List<ApplicationApproval> findByApproverIdAndDecisionDateBetween(Long approverId, LocalDateTime start, LocalDateTime end);
    List<ApplicationApproval> findByLoanApplicationId(Long applicationId);
    
    @Query("SELECT aa FROM ApplicationApproval aa WHERE aa.loanApplication.id = :applicationId ORDER BY aa.decisionDate DESC")
    List<ApplicationApproval> findApprovalHistory(@Param("applicationId") Long applicationId);
    
    @Query("SELECT COUNT(aa) FROM ApplicationApproval aa WHERE aa.approver.id = :approverId AND aa.decision = 'APPROVED'")
    Long countApprovedByApprover(@Param("approverId") Long approverId);
    
    Optional<ApplicationApproval> findTopByLoanApplicationIdOrderByApprovalLevelDesc(Long applicationId);

    List<ApplicationApproval> findByLoanApplicationIdOrderByCreatedAtDesc(Long applicationId);


    List<ApplicationApproval> findByDecisionAndDecisionDateBetween(GeneralConfig.ApprovalDecision decision,
                                                                   LocalDateTime startDate,
                                                                   LocalDateTime endDate);

    long countByDecisionAndDecisionDateBetween(GeneralConfig.ApprovalDecision decision,
                                               LocalDateTime startDate,
                                               LocalDateTime endDate);



    @Query("SELECT COUNT(a) FROM ApplicationApproval a WHERE a.approver.id = :approverId " +
            "AND a.decision = :decision AND a.decisionDate >= :startDate AND a.decisionDate < :endDate")
    long countByApproverAndDecisionBetweenDates(@Param("approverId") Long approverId,
                                                @Param("decision") GeneralConfig.ApprovalDecision decision,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);


    @Query("SELECT COUNT(a) FROM ApplicationApproval a WHERE a.approver.id = :approverId " +
            "AND a.decision = :decision AND DATE(a.decisionDate) = CURRENT_DATE")
    long countByApproverAndDecisionToday(@Param("approverId") Long approverId,
                                         @Param("decision") GeneralConfig.ApprovalDecision decision);

    List<ApplicationApproval> findByLoanApplicationIdOrderByCreatedAtAsc(Long id);


    /**
     * Check if a user has already approved/rejected/returned an application
     */
    boolean existsByLoanApplicationIdAndApproverId(Long loanApplicationId, Long approverId);

    // Additional variations you might need:

    /**
     * Check if a user has already approved (only approved) an application
     */
    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END 
        FROM ApplicationApproval a 
        WHERE a.loanApplication.id = :applicationId 
        AND a.approver.id = :approverId 
        AND a.decision = 'APPROVED'
    """)
    boolean existsApprovedByLoanApplicationIdAndApproverId(
            @Param("applicationId") Long applicationId,
            @Param("approverId") Long approverId);

    /**
     * Check if a user has made any decision on an application
     */
    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END 
        FROM ApplicationApproval a 
        WHERE a.loanApplication.id = :applicationId 
        AND a.approver.id = :approverId 
        AND a.decision IS NOT NULL
    """)
    boolean hasMadeDecisionOnApplication(
            @Param("applicationId") Long applicationId,
            @Param("approverId") Long approverId);

    /**
     * Check if a user has already processed (approved/rejected) an application
     */
    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END 
        FROM ApplicationApproval a 
        WHERE a.loanApplication.id = :applicationId 
        AND a.approver.id = :approverId 
        AND a.decision IN ('APPROVED', 'REJECTED', 'RETURNED_FOR_REVISION')
    """)
    boolean hasProcessedApplication(
            @Param("applicationId") Long applicationId,
            @Param("approverId") Long approverId);

    /**
     * Find the latest decision by a user on an application
     */
    @Query("""
        SELECT a FROM ApplicationApproval a 
        WHERE a.loanApplication.id = :applicationId 
        AND a.approver.id = :approverId 
        ORDER BY a.decisionDate DESC, a.createdAt DESC
    """)
    List<ApplicationApproval> findUserDecisionsOnApplication(
            @Param("applicationId") Long applicationId,
            @Param("approverId") Long approverId);


    @Query(value = """
    SELECT
        u.id,
        CONCAT(u.first_name, ' ', u.last_name) as approverName,
        u.role,
        b.name as branchName,
        COUNT(aa.id) as totalDecisions,
        SUM(CASE WHEN aa.decision = 'APPROVED' THEN 1 ELSE 0 END) as approvedCount,
        SUM(CASE WHEN aa.decision = 'REJECTED' THEN 1 ELSE 0 END) as rejectedCount,
        SUM(CASE WHEN aa.decision = 'RETURNED_FOR_REVISION' THEN 1 ELSE 0 END) as returnedCount
    FROM users u
    LEFT JOIN application_approvals aa ON aa.approver_id = u.id
    LEFT JOIN loan_applications la ON aa.loan_application_id = la.id
    LEFT JOIN branches b ON la.branch_id = b.id
    WHERE aa.decision_date BETWEEN :startDate AND :endDate
    AND (:branchId IS NULL OR la.branch_id = :branchId)
    AND aa.decision_date IS NOT NULL
    GROUP BY u.id, u.first_name, u.last_name, u.role, b.name
    ORDER BY approvedCount DESC
    LIMIT :limit
""", nativeQuery = true)
    List<Object[]> findApproverPerformance(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("branchId") Long branchId,
            @Param("limit") int limit);




    // Comments
   /* @Query("SELECT c FROM ApprovalComment c WHERE c.loanApplication.id = :applicationId ORDER BY c.createdAt DESC")
    List<ApprovalComment> findByLoanApplicationIdOrderByCreatedAtDesc(@Param("applicationId") Long applicationId);
*/
    @Query("SELECT c FROM ApprovalComment c WHERE c.loanApplication.id = :applicationId AND c.parentCommentId IS NULL ORDER BY c.createdAt DESC")
    List<ApprovalComment> findRootCommentsByApplicationId(@Param("applicationId") Long applicationId);

    // Queue Position
    @Query("SELECT COUNT(a) FROM LoanApplication a WHERE a.status IN :statuses AND a.submittedDate < :submittedDate")
    Long countApplicationsInQueueBefore(@Param("statuses") List<GeneralConfig.LoanApplicationStatus> statuses,
                                        @Param("submittedDate") LocalDateTime submittedDate);

    @Query(value = "SELECT a.* FROM loan_applications a " +
            "WHERE a.status IN ('SUBMITTED', 'UNDER_REVIEW', 'PENDING_APPROVAL') " +
            "AND a.branch_id = :branchId " +
            "ORDER BY CASE WHEN a.applied_amount >= 500000 THEN 0 ELSE 1 END, " +
            "a.submitted_date ASC", nativeQuery = true)
    List<LoanApplication> findQueueOrderByBranch(@Param("branchId") Long branchId);


    // In ApplicationApprovalRepository.java - simpler query
    @Query("SELECT a FROM ApplicationApproval a " +
            "WHERE a.approver.id = :approverId " +
            "AND a.decisionDate IS NOT NULL " +
            "AND a.createdAt IS NOT NULL")
    List<ApplicationApproval> findApprovalsWithProcessingTime(@Param("approverId") Long approverId);


    // Performance
    @Query("SELECT COUNT(a) FROM ApplicationApproval a WHERE a.approver.id = :approverId " +
            "AND a.decisionDate BETWEEN :startDate AND :endDate")
    Long countDecisionsByApproverAndDateRange(@Param("approverId") Long approverId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM ApplicationApproval a WHERE a.approver.id = :approverId " +
            "AND a.decisionDate BETWEEN :startDate AND :endDate " +
            "AND a.decision = :decision")
    List<ApplicationApproval> findByApproverAndDecisionAndDateRange(@Param("approverId") Long approverId,
                                                                    @Param("decision") GeneralConfig.ApprovalDecision decision,
                                                                    @Param("startDate") LocalDateTime startDate,
                                                                    @Param("endDate") LocalDateTime endDate);

    // SLA Monitoring
    @Query("SELECT a FROM LoanApplication a WHERE a.status IN :statuses " +
            "AND a.submittedDate <= :cutoffDate " +
            "AND a.submittedDate IS NOT NULL")
    List<LoanApplication> findPendingApprovalsExceedingSLA(@Param("statuses") List<GeneralConfig.LoanApplicationStatus> statuses,
                                                           @Param("cutoffDate") LocalDateTime cutoffDate);



}

