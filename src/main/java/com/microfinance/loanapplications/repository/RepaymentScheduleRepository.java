package com.microfinance.loanapplications.repository;

import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.entity.RepaymentSchedule;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {

    // Existing methods
    List<RepaymentSchedule> findByLoanIdAndStatusOrderByDueDate(Long loanId, GeneralConfig.InstallmentStatus status);

    List<RepaymentSchedule> findByLoanIdOrderByDueDate(Long loanId);

    List<RepaymentSchedule> findByDueDateBeforeAndStatus(LocalDate dueDate, GeneralConfig.InstallmentStatus status);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.loan.id = :loanId AND rs.status = 'PENDING' ORDER BY rs.dueDate ASC")
    List<RepaymentSchedule> findPendingInstallmentsByLoan(@Param("loanId") Long loanId);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate <= :date AND rs.status IN ('PENDING', 'OVERDUE')")
    List<RepaymentSchedule> findDueInstallments(@Param("date") LocalDate date);

    @Query("SELECT SUM(rs.outstandingAmount) FROM RepaymentSchedule rs WHERE rs.loan.id = :loanId")
    BigDecimal getTotalOutstandingByLoan(@Param("loanId") Long loanId);

    // NEW METHODS - Overdue Installments with various filtering options

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL')")
    Page<RepaymentSchedule> findOverdueInstallments(@Param("currentDate") LocalDate currentDate, Pageable pageable);
    /*
    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL') AND rs.loan.branch.id = :branchId")
    Page<RepaymentSchedule> findOverdueInstallments(@Param("currentDate") LocalDate currentDate,
                                                    @Param("branchId") Long branchId,
                                                    Pageable pageable);

     */

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL') AND rs.loan.borrower.id = :borrowerId")
    Page<RepaymentSchedule> findOverdueInstallmentsByBorrower(@Param("currentDate") LocalDate currentDate,
                                                              @Param("borrowerId") Long borrowerId,
                                                              Pageable pageable);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL') AND rs.loan.disbursedBy.id = :officerId")
    Page<RepaymentSchedule> findOverdueInstallmentsByOfficerBk(@Param("currentDate") LocalDate currentDate,
                                                             @Param("officerId") Long officerId,
                                                             Pageable pageable);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate " +
            "AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL') " +
            "AND rs.loan.disbursedBy.id = :officerId")
    Page<RepaymentSchedule> findOverdueInstallmentsByOfficer(@Param("currentDate") LocalDate currentDate,
                                                             @Param("officerId") Long officerId,
                                                             Pageable pageable);

    // Overdue installments with days overdue range
    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL') AND rs.daysOverdue BETWEEN :minDays AND :maxDays")
    Page<RepaymentSchedule> findOverdueInstallmentsByDaysRange(@Param("currentDate") LocalDate currentDate,
                                                               @Param("minDays") Integer minDays,
                                                               @Param("maxDays") Integer maxDays,
                                                               Pageable pageable);

    // Severely overdue (more than 30 days)
    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.daysOverdue > 30 AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL')")
    Page<RepaymentSchedule> findSeverelyOverdueInstallments(@Param("currentDate") LocalDate currentDate, Pageable pageable);

    // Moderately overdue (15-30 days)
    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.daysOverdue BETWEEN 15 AND 30 AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL')")
    Page<RepaymentSchedule> findModeratelyOverdueInstallments(@Param("currentDate") LocalDate currentDate, Pageable pageable);

    // Recently overdue (1-14 days)
    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.daysOverdue BETWEEN 1 AND 14 AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL')")
    Page<RepaymentSchedule> findRecentlyOverdueInstallments(@Param("currentDate") LocalDate currentDate, Pageable pageable);

    // Count methods for analytics
    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL')")
    Long countAllOverdueInstallments(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL') AND rs.loan.branch.id = :branchId")
    Long countOverdueInstallmentsByBranch(@Param("currentDate") LocalDate currentDate,
                                          @Param("branchId") Long branchId);

    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL') AND rs.loan.disbursedBy.id = :officerId")
    Long countOverdueInstallmentsByOfficer(@Param("currentDate") LocalDate currentDate,
                                           @Param("officerId") Long officerId);

    // Amount aggregation methods
    @Query("SELECT SUM(rs.outstandingAmount) FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL')")
    BigDecimal getTotalOverdueAmount(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT SUM(rs.outstandingAmount) FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL') AND rs.loan.branch.id = :branchId")
    BigDecimal getTotalOverdueAmountByBranch(@Param("currentDate") LocalDate currentDate,
                                             @Param("branchId") Long branchId);

    @Query("SELECT SUM(rs.outstandingAmount) FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL') AND rs.loan.disbursedBy.id = :officerId")
    BigDecimal getTotalOverdueAmountByOfficer(@Param("currentDate") LocalDate currentDate,
                                              @Param("officerId") Long officerId);

    // Overdue installments by risk level (based on amount and days overdue)
    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL') AND rs.outstandingAmount > :minAmount")
    Page<RepaymentSchedule> findHighValueOverdueInstallments(@Param("currentDate") LocalDate currentDate,
                                                             @Param("minAmount") BigDecimal minAmount,
                                                             Pageable pageable);

    // Overdue installments that are partially paid
    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status = 'PARTIAL'")
    Page<RepaymentSchedule> findPartiallyPaidOverdueInstallments(@Param("currentDate") LocalDate currentDate, Pageable pageable);

    // Overdue installments with penalty
    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL') AND rs.penaltyAccrued > 0")
    Page<RepaymentSchedule> findOverdueInstallmentsWithPenalty(@Param("currentDate") LocalDate currentDate, Pageable pageable);

    // Overdue installments by product type
    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL') AND rs.loan.loanProduct.id = :productId")
    Page<RepaymentSchedule> findOverdueInstallmentsByProduct(@Param("currentDate") LocalDate currentDate,
                                                             @Param("productId") Long productId,
                                                             Pageable pageable);

    // Overdue installments summary by branch
    @Query("SELECT rs.loan.branch.id, COUNT(rs), SUM(rs.outstandingAmount) FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL') GROUP BY rs.loan.branch.id")
    List<Object[]> getOverdueSummaryByBranch(@Param("currentDate") LocalDate currentDate);

    // Overdue installments summary by officer
    @Query("SELECT rs.loan.disbursedBy.id, COUNT(rs), SUM(rs.outstandingAmount) FROM RepaymentSchedule rs WHERE rs.dueDate < :currentDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL') GROUP BY rs.loan.disbursedBy.id")
    List<Object[]> getOverdueSummaryByOfficer(@Param("currentDate") LocalDate currentDate);

    // Overdue trend analysis (installments that became overdue in a date range)
    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate BETWEEN :startDate AND :endDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL')")
    Page<RepaymentSchedule> findInstallmentsThatBecameOverdueInPeriod(@Param("startDate") LocalDate startDate,
                                                                      @Param("endDate") LocalDate endDate,
                                                                      Pageable pageable);

    // Find next due installment for a loan
    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.loan.id = :loanId AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL') ORDER BY rs.dueDate ASC LIMIT 1")
    RepaymentSchedule findNextDueInstallment(@Param("loanId") Long loanId);

    // Find installments due in a specific date range
    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate BETWEEN :startDate AND :endDate AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL')")
    List<RepaymentSchedule> findInstallmentsDueInPeriod(@Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

    // Update days overdue for all overdue installments
    @Query(value = "UPDATE repayment_schedules rs " +
            "SET days_overdue = DATEDIFF(CURDATE(), rs.due_date) " +
            "WHERE rs.due_date < CURDATE() " +
            "AND rs.status IN ('PENDING', 'OVERDUE', 'PARTIAL')",
            nativeQuery = true)
    @Modifying
    @Transactional
    void updateDaysOverdue();



    List<RepaymentSchedule> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);

    // Helper method to get the first one
    default Optional<RepaymentSchedule> findNextDueByLoanId(Long loanId, LocalDate currentDate) {
        List<RepaymentSchedule> results = findNextDueCandidates(loanId, currentDate);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }


    /**
     * Alternative: Find the next due installment including overdue ones
     * This finds the earliest pending installment (could be overdue)
     */
    @Query("SELECT rs FROM RepaymentSchedule rs " +
            "WHERE rs.loan.id = :loanId " +
            "AND rs.status = 'PENDING' " +
            "ORDER BY rs.dueDate ASC")
    Optional<RepaymentSchedule> findNextPendingByLoanId(@Param("loanId") Long loanId);

    /**
     * Count the number of paid installments for a loan
     */
    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs " +
            "WHERE rs.loan.id = :loanId " +
            "AND rs.status = 'PAID'")
    long countPaidInstallments(@Param("loanId") Long loanId);

    /**
     * Count installments by status
     */
    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs " +
            "WHERE rs.loan.id = :loanId " +
            "AND rs.status = :status")
    long countByLoanIdAndStatus(
            @Param("loanId") Long loanId,
            @Param("status") GeneralConfig.InstallmentStatus status);

    /**
     * Calculate total arrears (overdue amount) for a loan as of a specific date
     */
    @Query("SELECT COALESCE(SUM(rs.outstandingAmount), 0) FROM RepaymentSchedule rs " +
            "WHERE rs.loan.id = :loanId " +
            "AND rs.dueDate < :currentDate " +
            "AND rs.status != 'PAID'")
    BigDecimal calculateTotalArrears(
            @Param("loanId") Long loanId,
            @Param("currentDate") LocalDate currentDate);

    /**
     * Calculate total arrears with a minimum days overdue
     */
    @Query("SELECT COALESCE(SUM(rs.outstandingAmount), 0) FROM RepaymentSchedule rs " +
            "WHERE rs.loan.id = :loanId " +
            "AND rs.dueDate < :currentDate " +
            "AND rs.status != 'PAID' " +
            "AND rs.dueDate <= :cutoffDate")
    BigDecimal calculateArrearsOlderThan(
            @Param("loanId") Long loanId,
            @Param("currentDate") LocalDate currentDate,
            @Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Find upcoming installments with a limit (using native query approach)
     */
    @Query(value = "SELECT * FROM repayment_schedules rs " +
            "WHERE rs.loan_id = :loanId " +
            "AND rs.due_date >= :currentDate " +
            "AND rs.status = 'PENDING' " +
            "ORDER BY rs.due_date ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<RepaymentSchedule> findUpcomingByLoanIdNative(
            @Param("loanId") Long loanId,
            @Param("currentDate") LocalDate currentDate,
            @Param("limit") int limit);


    /**
     * Find upcoming installments for a loan (limited number)
     */
    @Query("SELECT rs FROM RepaymentSchedule rs " +
            "WHERE rs.loan.id = :loanId " +
            "AND rs.dueDate >= :currentDate " +
            "AND rs.status = 'PENDING' " +
            "ORDER BY rs.dueDate ASC")
    List<RepaymentSchedule> findUpcomingByLoanId(
            @Param("loanId") Long loanId,
            @Param("currentDate") LocalDate currentDate,
            Pageable pageable);

    /**
     * Find overdue installments
     */
    @Query("SELECT rs FROM RepaymentSchedule rs " +
            "WHERE rs.loan.id = :loanId " +
            "AND rs.dueDate < :currentDate " +
            "AND rs.status != 'PAID' " +
            "ORDER BY rs.dueDate ASC")
    List<RepaymentSchedule> findOverdueByLoanId(
            @Param("loanId") Long loanId,
            @Param("currentDate") LocalDate currentDate);

    /**
     * Calculate total outstanding balance for a loan
     */
    @Query("SELECT COALESCE(SUM(rs.outstandingAmount), 0) FROM RepaymentSchedule rs " +
            "WHERE rs.loan.id = :loanId")
    BigDecimal calculateTotalOutstandingBalance(@Param("loanId") Long loanId);

    /**
     * Find installments due between dates
     */
    @Query("SELECT rs FROM RepaymentSchedule rs " +
            "WHERE rs.loan.id = :loanId " +
            "AND rs.dueDate BETWEEN :startDate AND :endDate " +
            "ORDER BY rs.dueDate ASC")
    List<RepaymentSchedule> findInstallmentsDueBetween(
            @Param("loanId") Long loanId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Check if loan has any overdue installments
     */
    @Query("SELECT CASE WHEN COUNT(rs) > 0 THEN true ELSE false END " +
            "FROM RepaymentSchedule rs " +
            "WHERE rs.loan.id = :loanId " +
            "AND rs.dueDate < :currentDate " +
            "AND rs.status != 'PAID'")
    boolean hasOverdueInstallments(
            @Param("loanId") Long loanId,
            @Param("currentDate") LocalDate currentDate);

    /**
     * Get the earliest due date for a loan
     */
    @Query("SELECT MIN(rs.dueDate) FROM RepaymentSchedule rs " +
            "WHERE rs.loan.id = :loanId")
    Optional<LocalDate> findEarliestDueDate(@Param("loanId") Long loanId);

    /**
     * Get the latest due date for a loan (maturity date from schedule)
     */
    @Query("SELECT MAX(rs.dueDate) FROM RepaymentSchedule rs " +
            "WHERE rs.loan.id = :loanId")
    Optional<LocalDate> findLatestDueDate(@Param("loanId") Long loanId);

    /**
     * Find all overdue installments across all loans (for reporting)
     */
    @Query("SELECT rs FROM RepaymentSchedule rs " +
            "WHERE rs.dueDate < :currentDate " +
            "AND rs.status != 'PAID' " +
            "ORDER BY rs.dueDate ASC")
    List<RepaymentSchedule> findAllOverdueInstallments(@Param("currentDate") LocalDate currentDate);

    /**
     * Calculate total overdue amount across all loans for a branch
     */
    @Query("SELECT COALESCE(SUM(rs.outstandingAmount), 0) FROM RepaymentSchedule rs " +
            "JOIN rs.loan l " +
            "WHERE rs.dueDate < :currentDate " +
            "AND rs.status != 'PAID' " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId)")
    BigDecimal calculateTotalBranchArrears(
            @Param("branchId") Long branchId,
            @Param("currentDate") LocalDate currentDate);



    // FIX 1: This method should return List, not Optional
    @Query("SELECT rs FROM RepaymentSchedule rs " +
            "WHERE rs.loan.id = :loanId " +
            "AND rs.status = 'PENDING' " +
            "AND rs.dueDate >= :currentDate " +
            "ORDER BY rs.dueDate ASC")
    List<RepaymentSchedule> findNextDueCandidates(
            @Param("loanId") Long loanId,
            @Param("currentDate") LocalDate currentDate);

    // Alternative native query with LIMIT


    // Add these to RepaymentScheduleRepository.java

    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs WHERE rs.dueDate = :date AND rs.status = 'PENDING'")
    Long countDueToday(@Param("date") LocalDate date);

    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs WHERE rs.dueDate < :date AND rs.status = 'PENDING'")
    Long countOverdueAsOfDate(@Param("date") LocalDate date);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate < :date AND rs.status = 'PENDING' AND (:branchId IS NULL OR rs.loan.branch.id = :branchId)")
    Page<RepaymentSchedule> findOverdueInstallments(@Param("date") LocalDate date, @Param("branchId") Long branchId, Pageable pageable);

// In RepaymentScheduleRepository.java

    @Query("SELECT DISTINCT rs FROM RepaymentSchedule rs " +
            "LEFT JOIN FETCH rs.loan l " +
            "LEFT JOIN FETCH l.borrower b " +
            "LEFT JOIN FETCH l.branch br " +
            "LEFT JOIN FETCH l.loanProduct lp " +
            "WHERE (:status IS NULL OR rs.status = :status) " +
            "AND (:branchId IS NULL OR br.id = :branchId) " +
            "AND (:loanProductId IS NULL OR lp.id = :loanProductId) " +
            "AND (:startDate IS NULL OR rs.dueDate >= :startDate) " +
            "AND (:endDate IS NULL OR rs.dueDate <= :endDate) " +
            "AND (:search IS NULL OR " +
            "   LOWER(l.loanAccountNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(b.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(b.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(b.borrowerNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<RepaymentSchedule> findWithFilters(
            @Param("status") String status,
            @Param("branchId") Long branchId,
            @Param("loanProductId") Long loanProductId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable);

    /*
    @EntityGraph(attributePaths = {"borrower", "branch", "loanProduct", "repaymentSchedules"})
    @Query("SELECT DISTINCT l FROM Loan l " +
            "LEFT JOIN FETCH l.borrower b " +
            "LEFT JOIN FETCH l.branch br " +
            "LEFT JOIN FETCH l.loanProduct lp " +
            "LEFT JOIN FETCH l.repaymentSchedules rs " +
            "WHERE (:status IS NULL OR l.status = :status) " +
            "AND (:branchId IS NULL OR br.id = :branchId) " +
            "AND (:loanProductId IS NULL OR lp.id = :loanProductId) " +
            "AND (:search IS NULL OR " +
            "   LOWER(l.loanAccountNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(b.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(b.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(b.borrowerNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Loan> findLoansWithFilters(
            @Param("status") String status,
            @Param("branchId") Long branchId,
            @Param("loanProductId") Long loanProductId,
            @Param("search") String search,
            Pageable pageable);


*/


    @Query("SELECT DISTINCT l FROM Loan l " +
            "LEFT JOIN FETCH l.borrower b " +
            "LEFT JOIN FETCH l.branch br " +
            "LEFT JOIN FETCH l.loanProduct lp " +
            "LEFT JOIN l.repaymentSchedules rs " +  // This is the correct JPQL syntax
            "WHERE (:status IS NULL OR l.status = :status) " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId) " +
            "AND (:loanProductId IS NULL OR l.loanProduct.id = :loanProductId) " +
            "AND (:search IS NULL OR :search = '' OR " +
            "      LOWER(l.loanAccountNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "      LOWER(b.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "      LOWER(b.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "      LOWER(b.borrowerNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Loan> findLoansWithFilters(
            @Param("status") String status,
            @Param("branchId") Long branchId,
            @Param("loanProductId") Long loanProductId,
            @Param("search") String search,
            Pageable pageable);



    @Query(value = """
    SELECT DISTINCT l.* 
    FROM loans l
    LEFT JOIN borrowers b ON b.id = l.borrower_id
    LEFT JOIN branches br ON br.id = l.branch_id
    LEFT JOIN loan_products lp ON lp.id = l.loan_product_id
    LEFT JOIN repayment_schedules rs ON rs.loan_id = l.id
    WHERE (:status IS NULL OR CAST(:status AS text) IS NULL OR l.status = CAST(:status AS text))
      AND (:branchId IS NULL OR CAST(:branchId AS bigint) IS NULL OR l.branch_id = CAST(:branchId AS bigint))
      AND (:loanProductId IS NULL OR CAST(:loanProductId AS bigint) IS NULL OR l.loan_product_id = CAST(:loanProductId AS bigint))
      AND (:search IS NULL OR CAST(:search AS text) IS NULL OR 
           LOWER(l.loan_account_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
           LOWER(b.first_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
           LOWER(b.last_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
           LOWER(b.borrower_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
    ORDER BY l.id ASC
    """,
            nativeQuery = true,
            countQuery = """
    SELECT COUNT(DISTINCT l.id)
    FROM loans l
    LEFT JOIN borrowers b ON b.id = l.borrower_id
    WHERE (:status IS NULL OR CAST(:status AS text) IS NULL OR l.status = CAST(:status AS text))
      AND (:branchId IS NULL OR CAST(:branchId AS bigint) IS NULL OR l.branch_id = CAST(:branchId AS bigint))
      AND (:loanProductId IS NULL OR CAST(:loanProductId AS bigint) IS NULL OR l.loan_product_id = CAST(:loanProductId AS bigint))
      AND (:search IS NULL OR CAST(:search AS text) IS NULL OR 
           LOWER(l.loan_account_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
           LOWER(b.first_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
           LOWER(b.last_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
           LOWER(b.borrower_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
    """)
    Page<Loan> findLoansWithFiltersORG(
            @Param("status") String status,
            @Param("branchId") Long branchId,
            @Param("loanProductId") Long loanProductId,
            @Param("search") String search,
            Pageable pageable);









    @Query("SELECT rs FROM RepaymentSchedule rs " +
            "LEFT JOIN FETCH rs.loan l " +
            "LEFT JOIN FETCH l.borrower b " +
            "LEFT JOIN FETCH l.branch br " +
            "LEFT JOIN FETCH l.loanProduct lp " +
            "WHERE rs.loan.id = :loanId ORDER BY rs.installmentNumber")
    List<RepaymentSchedule> findByLoanIdOrderByInstallmentNumber(@Param("loanId") Long loanId);

    @Query("SELECT rs FROM RepaymentSchedule rs " +
            "LEFT JOIN FETCH rs.loan l " +
            "LEFT JOIN FETCH l.borrower b " +
            "LEFT JOIN FETCH l.branch br " +
            "WHERE rs.id = :id")
    Optional<RepaymentSchedule> findByIdWithDetails(@Param("id") Long id);


    @Query("SELECT COALESCE(SUM(rs.totalDue - COALESCE(rs.paidAmount, 0)), 0) FROM RepaymentSchedule rs " +
            "WHERE rs.dueDate <= :asOfDate " +
            "AND rs.status != 'PAID' " +
            "AND (:branchId IS NULL OR rs.loan.branch.id = :branchId)")
    BigDecimal sumTotalDue(@Param("branchId") Long branchId, @Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs " +
            "WHERE rs.dueDate BETWEEN :startDate AND :endDate " +
            "AND rs.status != 'PAID' " +
            "AND (:branchId IS NULL OR rs.loan.branch.id = :branchId)")
    Long countUpcomingPayments(@Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate,
                               @Param("branchId") Long branchId);

    @Query("SELECT rs FROM RepaymentSchedule rs " +
            "WHERE rs.dueDate BETWEEN :startDate AND :endDate " +
            "AND rs.status != 'PAID' " +
            "AND (:branchId IS NULL OR rs.loan.branch.id = :branchId) " +
            "ORDER BY rs.dueDate")
    List<RepaymentSchedule> findUpcomingPayments(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("branchId") Long branchId);

    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs " +
            "WHERE rs.dueDate < :asOfDate " +
            "AND (rs.paidAmount IS NULL OR rs.paidAmount < rs.totalDue) " +
            "AND (:branchId IS NULL OR rs.loan.branch.id = :branchId)")
    Long countOverduePayments(@Param("asOfDate") LocalDate asOfDate, @Param("branchId") Long branchId);

    @Query("SELECT rs FROM RepaymentSchedule rs " +
            "WHERE rs.dueDate < :asOfDate " +
            "AND (rs.paidAmount IS NULL OR rs.paidAmount < rs.totalDue) " +
            "AND (:branchId IS NULL OR rs.loan.branch.id = :branchId) " +
            "ORDER BY rs.dueDate")
    List<RepaymentSchedule> findOverduePayments(@Param("asOfDate") LocalDate asOfDate,
                                                @Param("branchId") Long branchId);

    @Query("SELECT rs FROM RepaymentSchedule rs " +
            "WHERE rs.dueDate BETWEEN :startDate AND :endDate " +
            "AND (:branchId IS NULL OR rs.loan.branch.id = :branchId) " +
            "ORDER BY rs.dueDate")
    List<RepaymentSchedule> findInstallmentsInDateRange(@Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate,
                                                        @Param("branchId") Long branchId);

    @Query("SELECT rs FROM RepaymentSchedule rs " +
            "WHERE rs.dueDate BETWEEN :startDate AND :endDate " +
            "AND (rs.paidAmount IS NULL OR rs.paidAmount < rs.totalDue) " +
            "AND (:branchId IS NULL OR rs.loan.branch.id = :branchId) " +
            "ORDER BY rs.dueDate")
    List<RepaymentSchedule> findDueInstallmentsInDateRange(@Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate,
                                                           @Param("branchId") Long branchId);


    @Query("SELECT COUNT(DISTINCT l.id) FROM Loan l " +
            "WHERE l.status = 'ACTIVE' " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId) " +
            "AND l.disbursementDate <= :asOfDate")
    Long countActiveSchedules(@Param("branchId") Long branchId,
                              @Param("asOfDate") LocalDate asOfDate);




    List<RepaymentSchedule> findByLoanIdAndStatus(Long id, GeneralConfig.InstallmentStatus installmentStatus);

    List<RepaymentSchedule> findByLoanIdOrderByDueDateAsc(Long loanId);

    Page<RepaymentSchedule> findByLoanId(Long loanId, Pageable pageable);


    /**
     * Find overdue repayment schedules for a loan
     */
    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.loan.id = :loanId " +
            "AND rs.dueDate < :currentDate " +
            "AND rs.status != :status " +
            "ORDER BY rs.dueDate ASC")
    List<RepaymentSchedule> findByLoanIdAndDueDateBeforeAndStatusNot(
            @Param("loanId") Long loanId,
            @Param("currentDate") LocalDate currentDate,
            @Param("status") GeneralConfig.InstallmentStatus status);




    List<RepaymentSchedule> findByLoanId(Long loanId);

    List<RepaymentSchedule> findByLoanIdAndStatusOrderByDueDateAsc(Long loanId, GeneralConfig.InstallmentStatus status);

    Optional<RepaymentSchedule> findFirstByLoanIdAndStatusOrderByDueDateAsc(Long loanId, GeneralConfig.InstallmentStatus status);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.loan.id = :loanId AND rs.dueDate <= :date AND rs.status IN ('PENDING', 'PARTIAL') ORDER BY rs.dueDate ASC")
    List<RepaymentSchedule> findOverdueInstallments(@Param("loanId") Long loanId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(rs.principalDue - rs.principalPaid), 0) FROM RepaymentSchedule rs WHERE rs.loan.id = :loanId")
    BigDecimal getTotalPrincipalOutstanding(@Param("loanId") Long loanId);

    @Query("SELECT COALESCE(SUM(rs.interestDue - rs.interestPaid), 0) FROM RepaymentSchedule rs WHERE rs.loan.id = :loanId")
    BigDecimal getTotalInterestOutstanding(@Param("loanId") Long loanId);

    /**
     * Find the most recent payment date using native SQL
     */
    @Query(value = "SELECT MAX(rs.payment_date) FROM repayment_schedules rs WHERE rs.loan_id = :loanId AND rs.payment_date IS NOT NULL",
            nativeQuery = true)
    Optional<LocalDate> findLastPaymentDateByLoanIdNative(@Param("loanId") Long loanId);


    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate BETWEEN :start AND :end AND rs.status = 'PENDING'")
    List<RepaymentSchedule> findDueBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate BETWEEN :start AND :end AND rs.status = 'PENDING' AND rs.loan.branch.id = :branchId")
    List<RepaymentSchedule> findDueBetweenAndBranch(@Param("start") LocalDate start,
                                                    @Param("end") LocalDate end,
                                                    @Param("branchId") Long branchId);

    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs WHERE rs.dueDate < :date AND rs.status = 'PENDING'")
    int countOverdue(@Param("date") LocalDate date);

    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs WHERE rs.dueDate < :date AND rs.status = 'PENDING' AND rs.loan.branch.id = :branchId")
    int countOverdueByBranch(@Param("date") LocalDate date, @Param("branchId") Long branchId);

    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs WHERE rs.dueDate <= :threshold AND rs.daysOverdue >= :days AND rs.status = 'PENDING'")
    int countOverdueByDays(@Param("threshold") LocalDate threshold, @Param("days") int days);

    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs WHERE rs.dueDate <= :threshold AND rs.daysOverdue >= :days AND rs.status = 'PENDING' AND rs.loan.branch.id = :branchId")
    int countOverdueByDaysAndBranch(@Param("threshold") LocalDate threshold,
                                    @Param("days") int days,
                                    @Param("branchId") Long branchId);

    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs WHERE rs.dueDate = :date AND rs.status = 'PENDING' AND rs.loan.branch.id = :branchId")
    int countDueTodayByBranch(@Param("date") LocalDate date, @Param("branchId") Long branchId);



}