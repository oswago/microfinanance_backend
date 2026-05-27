package com.microfinance.loanapplications.repository;

import com.microfinance.common.config.DocumentConfig;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanapplications.entity.LoanApplication;
import com.microfinance.base.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    
    List<LoanApplication> findByBorrowerIdAndStatus(Long borrowerId, GeneralConfig.LoanApplicationStatus status);
    
    List<LoanApplication> findByStatus(GeneralConfig.LoanApplicationStatus status);
    
    List<LoanApplication> findByCreatedByAndStatus(User createdBy, GeneralConfig.LoanApplicationStatus status);
    
    Page<LoanApplication> findByStatus(GeneralConfig.LoanApplicationStatus status, Pageable pageable);
    
    Optional<LoanApplication> findByApplicationNumber(String applicationNumber);

    /**
     * Find applications by status in the given list of statuses
     */
    List<LoanApplication> findByStatusIn(List<GeneralConfig.LoanApplicationStatus> statuses);

    /**
     * Find applications by status in the given list of statuses with pagination
     */
    Page<LoanApplication> findByStatusIn(List<GeneralConfig.LoanApplicationStatus> statuses, Pageable pageable);

    /**
     * Find draft applications created by a specific user
     */
    List<LoanApplication> findByStatusAndCreatedBy(GeneralConfig.LoanApplicationStatus status, Long createdBy);

    /**
     * Find draft applications created by a specific user with pagination
     */
    Page<LoanApplication> findByStatusAndCreatedBy(GeneralConfig.LoanApplicationStatus status, Long createdBy, Pageable pageable);

    /**
     * Find all applications for a specific borrower
     */
    List<LoanApplication> findByBorrowerId(Long borrowerId);

    /**
     * Find applications for a specific borrower with pagination
     */
    Page<LoanApplication> findByBorrowerId(Long borrowerId, Pageable pageable);

    /**
     * Find applications by borrower and status in list
     */
    List<LoanApplication> findByBorrowerIdAndStatusIn(Long borrowerId, List<GeneralConfig.LoanApplicationStatus> statuses);


    @Query("SELECT COUNT(la) FROM LoanApplication la WHERE la.borrower.id = :borrowerId " +
            "AND la.status IN ('PENDING_APPROVAL', 'APPROVED', 'PROCESSING')")
    long countActiveApplicationsByBorrower(@Param("borrowerId") Long borrowerId);
    
    @Query("SELECT la FROM LoanApplication la WHERE la.status = 'PENDING_APPROVAL' " +
           "ORDER BY la.submittedDate ASC")
    List<LoanApplication> findPendingApprovals();
    
    @Query("SELECT la FROM LoanApplication la WHERE la.createdBy = :user AND la.status = 'DRAFT'")
    List<LoanApplication> findDraftsByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(la) FROM LoanApplication la WHERE la.loanProduct.id = :productId")
    long countByLoanProduct(@Param("productId") Long productId);

    // Financial aggregation methods
    // Basic count methods with date ranges
    Long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    Long countByStatusAndCreatedAtBetween(GeneralConfig.LoanApplicationStatus status,
                                          LocalDateTime startDate,
                                          LocalDateTime endDate);

    // Financial aggregation methods
    @Query("SELECT COALESCE(SUM(la.appliedAmount), 0) FROM LoanApplication la " +
            "WHERE la.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumAppliedAmountByPeriod(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(la.appliedAmount), 0) FROM LoanApplication la " +
            "WHERE la.status = 'APPROVED' AND la.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumApprovedAmountByPeriod(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(MIN(la.appliedAmount), 0) FROM LoanApplication la " +
            "WHERE la.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal findMinApplicationAmountByPeriod(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(MAX(la.appliedAmount), 0) FROM LoanApplication la " +
            "WHERE la.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal findMaxApplicationAmountByPeriod(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    // Status-based financial aggregation
    @Query("SELECT COALESCE(SUM(la.appliedAmount), 0) FROM LoanApplication la " +
            "WHERE la.status = :status AND la.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByStatusAndPeriod(@Param("status") GeneralConfig.LoanApplicationStatus status,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // NEW METHODS FOR TIME-BASED COUNTS:

    /**
     * Count applications created today
     */
    @Query("SELECT COUNT(la) FROM LoanApplication la " +
            "WHERE DATE(la.createdAt) = CURRENT_DATE")
    Long countApplicationsCreatedToday();

    /**
     * Count applications created this week (Monday to Sunday)
     */
    @Query("SELECT COUNT(la) FROM LoanApplication la " +
            "WHERE YEAR(la.createdAt) = YEAR(CURRENT_DATE) " +
            "AND WEEK(la.createdAt) = WEEK(CURRENT_DATE)")
    Long countApplicationsCreatedThisWeek();

    /**
     * Count applications created this month
     */
    @Query("SELECT COUNT(la) FROM LoanApplication la " +
            "WHERE YEAR(la.createdAt) = YEAR(CURRENT_DATE) " +
            "AND MONTH(la.createdAt) = MONTH(CURRENT_DATE)")
    Long countApplicationsCreatedThisMonth();

    /**
     * Count applications by status created today
     */
    @Query("SELECT COUNT(la) FROM LoanApplication la " +
            "WHERE la.status = :status " +
            "AND DATE(la.createdAt) = CURRENT_DATE")
    Long countByStatusAndCreatedToday(@Param("status") GeneralConfig.LoanApplicationStatus status);

    /**
     * Count applications by status created this week
     */
    @Query("SELECT COUNT(la) FROM LoanApplication la " +
            "WHERE la.status = :status " +
            "AND YEAR(la.createdAt) = YEAR(CURRENT_DATE) " +
            "AND WEEK(la.createdAt) = WEEK(CURRENT_DATE)")
    Long countByStatusAndCreatedThisWeek(@Param("status") GeneralConfig.LoanApplicationStatus status);

    /**
     * Count applications by status created this month
     */
    @Query("SELECT COUNT(la) FROM LoanApplication la " +
            "WHERE la.status = :status " +
            "AND YEAR(la.createdAt) = YEAR(CURRENT_DATE) " +
            "AND MONTH(la.createdAt) = MONTH(CURRENT_DATE)")
    Long countByStatusAndCreatedThisMonth(@Param("status") GeneralConfig.LoanApplicationStatus status);

    // Alternative methods using native queries (if needed)

    /**
     * Count today's applications using native query
     */
    @Query(value = "SELECT COUNT(*) FROM loan_applications la " +
            "WHERE DATE(la.created_at) = CURDATE()", nativeQuery = true)
    Long countApplicationsCreatedTodayNative();

    /**
     * Count this week's applications using native query
     */
    @Query(value = "SELECT COUNT(*) FROM loan_applications la " +
            "WHERE YEARWEEK(la.created_at) = YEARWEEK(CURDATE())", nativeQuery = true)
    Long countApplicationsCreatedThisWeekNative();

    /**
     * Count this month's applications using native query
     */
    @Query(value = "SELECT COUNT(*) FROM loan_applications la " +
            "WHERE YEAR(la.created_at) = YEAR(CURDATE()) " +
            "AND MONTH(la.created_at) = MONTH(CURDATE())", nativeQuery = true)
    Long countApplicationsCreatedThisMonthNative();

    // Add this method for pending approvals

    @Query("SELECT la FROM LoanApplication la WHERE la.status IN ('SUBMITTED', 'UNDER_REVIEW') AND la.branch.id = :branchId")
    List<LoanApplication> findPendingApprovalsByBranch(@Param("branchId") Long branchId);


    /**
     * Find pending loan applications that a specific user can approve
     * Based on user's role, branch, and approval limits
     */
    @Query("""
        SELECT DISTINCT la FROM LoanApplication la
        LEFT JOIN la.branch b
        LEFT JOIN la.loanProduct lp
        WHERE la.status IN :pendingStatuses
        AND la.submittedDate IS NOT NULL
        AND (:userId IS NULL OR la.createdBy = :userId
            OR EXISTS (
                SELECT u FROM User u
                WHERE u.id = :userId
                AND (
                    u.role = 'SUPER_ADMIN'
                    OR (u.role = 'BRANCH_MANAGER' AND (b.id = :branchId OR :branchId IS NULL) AND la.appliedAmount <= 1000000)
                    OR (u.role = 'CREDIT_APPROVER' AND la.appliedAmount <= 5000000)
                    OR (u.role = 'REGIONAL_MANAGER' AND la.appliedAmount <= 10000000)
                )
            )
        )
    """)
    List<LoanApplication> findPendingApprovalsForUser(
            @Param("userId") Long userId,
            @Param("branchId") Long branchId,
            @Param("pendingStatuses") List<GeneralConfig.LoanApplicationStatus> pendingStatuses);


    /**
     * Default method with pre-defined pending statuses
     */
    default List<LoanApplication> findPendingApprovalsForUser(Long userId, Long branchId) {
        return findPendingApprovalsForUser(
                userId,
                branchId,
                List.of(
                        GeneralConfig.LoanApplicationStatus.SUBMITTED,
                        GeneralConfig.LoanApplicationStatus.UNDER_REVIEW,
                        GeneralConfig.LoanApplicationStatus.PENDING_APPROVAL
                )
        );
    }

    /**
     * Find pending approvals with advanced filters
     */
    @Query("""
    SELECT la FROM LoanApplication la
    LEFT JOIN la.branch b
    LEFT JOIN la.loanProduct lp
    LEFT JOIN la.borrower br
    WHERE la.status IN ('SUBMITTED', 'UNDER_REVIEW', 'PENDING_APPROVAL')
    AND (:userId IS NULL OR la.createdBy = :userId)
    AND (:userBranchId IS NULL OR b.id = :userBranchId)
    AND (:branchId IS NULL OR b.id = :branchId)
    AND (:minAmount IS NULL OR la.appliedAmount >= :minAmount)
    AND (:maxAmount IS NULL OR la.appliedAmount <= :maxAmount)
    AND (:productType IS NULL OR lp.productType.name = :productType)
    AND (CAST(:startDate AS timestamp) IS NULL OR la.submittedDate >= :startDate)
    AND (CAST(:endDate AS timestamp) IS NULL OR la.submittedDate <= :endDate)
    AND la.submittedDate IS NOT NULL
    ORDER BY 
        CASE 
            WHEN la.appliedAmount >= 500000 THEN 0
            ELSE 1
        END,
        la.submittedDate ASC
""")
    List<LoanApplication> findPendingApprovalsWithFilters(
            @Param("userId") Long userId,
            @Param("userBranchId") Long userBranchId,
            @Param("branchId") Long branchId,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            @Param("productType") String productType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);


    @Query("""
    SELECT la FROM LoanApplication la
    LEFT JOIN la.branch b
    LEFT JOIN la.loanProduct lp
    LEFT JOIN la.borrower br
    WHERE la.status IN ('SUBMITTED', 'UNDER_REVIEW', 'PENDING_APPROVAL')
    AND la.submittedDate IS NOT NULL
    ORDER BY 
        CASE 
            WHEN la.appliedAmount >= 500000 THEN 0
            ELSE 1
        END,
        la.submittedDate ASC
""")
    List<LoanApplication> findAllPendingApprovals();

    /**
     * Count pending applications for a user within a date range
     */
    @Query("""
    SELECT COUNT(la) FROM LoanApplication la
    LEFT JOIN la.branch b
    WHERE la.status IN :statuses
      AND (:startDate IS NULL OR la.submittedDate >= :startDate)
       AND (:endDate IS NULL OR la.submittedDate <= :endDate)
    AND (:branchId IS NULL OR b.id = :branchId)
    AND (:userId IS NULL OR la.createdBy = :userId)
    AND la.submittedDate IS NOT NULL
""")
    long countPendingForUser(
            @Param("userId") Long userId,
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("statuses") List<String> statuses);




    /**
     * Count pending applications for a user within a date range
     */
    @Query("""
    SELECT COUNT(la) FROM LoanApplication la
    LEFT JOIN la.branch b
    WHERE la.submittedDate BETWEEN :startDate AND :endDate
    AND (:branchId IS NULL OR b.id = :branchId)
    AND (:userId IS NULL OR la.createdBy = :userId)
    AND la.submittedDate IS NOT NULL
""")
    long countPendingForUser(
            @Param("userId") Long userId,
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find applications pending at a specific approval level
     */
    @Query("""
        SELECT la FROM LoanApplication la
        WHERE la.status IN :statuses
        AND la.currentApprovalLevel = :approvalLevel
        AND la.submittedDate IS NOT NULL
        ORDER BY la.submittedDate ASC
    """)
    List<LoanApplication> findPendingAtApprovalLevel(
            @Param("approvalLevel") Integer approvalLevel,
            @Param("statuses") List<GeneralConfig.LoanApplicationStatus> statuses);

    /**
     * Find overdue pending approvals (past SLA)
     */
    @Query("""
        SELECT la FROM LoanApplication la
        WHERE la.status IN :statuses
        AND la.submittedDate IS NOT NULL
        AND la.submittedDate <= :cutoffDate
        ORDER BY la.submittedDate ASC
    """)
    List<LoanApplication> findOverduePendingApprovals(
            @Param("cutoffDate") LocalDateTime cutoffDate,
            @Param("statuses") List<GeneralConfig.LoanApplicationStatus> statuses);

    /**
     * Find pending approvals by product type
     */
    @Query("""
        SELECT la FROM LoanApplication la
        LEFT JOIN la.loanProduct lp
        WHERE la.status IN :statuses
        AND lp.productType = :productType
        AND la.submittedDate IS NOT NULL
        ORDER BY la.submittedDate ASC
    """)
    List<LoanApplication> findPendingByProductType(
            @Param("productType") String productType,
            @Param("statuses") List<GeneralConfig.LoanApplicationStatus> statuses);

    /**
     * Find pending approvals for a specific borrower
     */
    @Query("""
        SELECT la FROM LoanApplication la
        WHERE la.status IN :statuses
        AND la.borrower.id = :borrowerId
        AND la.submittedDate IS NOT NULL
        ORDER BY la.submittedDate DESC
    """)
    List<LoanApplication> findPendingForBorrower(
            @Param("borrowerId") Long borrowerId,
            @Param("statuses") List<GeneralConfig.LoanApplicationStatus> statuses);

    /**
     * Find applications that have been in pending status for more than X days
     */
    @Query("""
        SELECT la FROM LoanApplication la
        WHERE la.status IN :statuses
        AND la.submittedDate IS NOT NULL
        AND DATEDIFF(day, la.submittedDate, CURRENT_DATE) > :daysThreshold
        ORDER BY la.submittedDate ASC
    """)
    List<LoanApplication> findLongPendingApplications(
            @Param("daysThreshold") Integer daysThreshold,
            @Param("statuses") List<GeneralConfig.LoanApplicationStatus> statuses);



    // Add this method to LoanApplicationRepository.java
    @Query("SELECT COUNT(la) FROM LoanApplication la WHERE la.status = :status")
    Long countByStatus(@Param("status") GeneralConfig.LoanApplicationStatus status);


    @Query("SELECT COUNT(l) FROM LoanApplication l WHERE l.status = :status AND l.branch.id = :branchId")
    int countByStatusAndBranchId(@Param("status") GeneralConfig.LoanApplicationStatus status, @Param("branchId") Long branchId);

    @Query("SELECT COUNT(l) FROM LoanApplication l WHERE l.status = :status AND l.approvedDate BETWEEN :start AND :end")
    int countByStatusAndApprovedDateBetween(@Param("status") GeneralConfig.LoanApplicationStatus status,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(l) FROM LoanApplication l WHERE l.status = :status AND l.approvedDate BETWEEN :start AND :end AND l.branch.id = :branchId")
    int countByStatusAndApprovedDateBetweenAndBranchId(@Param("status") GeneralConfig.LoanApplicationStatus status,
                                                       @Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end,
                                                       @Param("branchId") Long branchId);



    /**
     * Find top N applications ordered by creation date descending (all branches)
     */
    @Query("SELECT la FROM LoanApplication la ORDER BY la.createdAt DESC")
    List<LoanApplication> findTopByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find top N applications for a specific branch ordered by creation date descending
     */
    @Query("SELECT la FROM LoanApplication la WHERE la.branch.id = :branchId ORDER BY la.createdAt DESC")
    List<LoanApplication> findTopByBranchIdOrderByCreatedAtDesc(@Param("branchId") Long branchId, Pageable pageable);

    /**
     * Find applications by status and branch with pagination
     */
    Page<LoanApplication> findByStatusAndBranchId(GeneralConfig.LoanApplicationStatus status,
                                                  @Param("branchId") Long branchId,
                                                  Pageable pageable);





}