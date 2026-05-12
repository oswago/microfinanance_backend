package com.microfinance.loanapplications.repository;

import com.microfinance.base.entity.User;
import com.microfinance.common.config.DocumentConfig;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanapplications.dto.application.PortfolioStats;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.reports.dto.ProductPortfolioDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    
    List<Loan> findByBorrowerId(Long borrowerId);
    
    List<Loan> findByStatus(GeneralConfig.LoanStatus status);
    
    Page<Loan> findByStatus(GeneralConfig.LoanStatus status, Pageable pageable);
    
    Optional<Loan> findByLoanAccountNumber(String loanAccountNumber);
    
    Optional<Loan> findByLoanApplicationId(Long applicationId);

    @Query("SELECT l.loanProduct.id FROM Loan l WHERE l.borrower.id = :borrowerId")
    List<Long> findLoanProductIdsByBorrowerId(@Param("borrowerId") Long borrowerId);
    
    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.maturityDate < :date")
    List<Loan> findOverdueLoans(@Param("date") LocalDate date);
    
    @Query("SELECT l FROM Loan l WHERE l.borrower.id = :borrowerId AND l.status IN ('ACTIVE', 'DELINQUENT')")
    List<Loan> findActiveLoansByBorrower(@Param("borrowerId") Long borrowerId);
    
    @Query("SELECT SUM(l.outstandingBalance) FROM Loan l WHERE l.status IN ('ACTIVE', 'DELINQUENT')")
    BigDecimal getTotalOutstandingPortfolio();
    
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = 'DELINQUENT'")
    long countDelinquentLoans();
    
    @Query("SELECT l FROM Loan l WHERE l.status = 'PENDING_DISBURSEMENT'")
    List<Loan> findLoansPendingDisbursement();


    // Add these to your LoanRepository
        Page<Loan> findByBorrowerId(Long borrowerId, Pageable pageable);

        @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = 'ACTIVE' AND l.branch.id = :branchId")
        Long countActiveLoansByBranch(@Param("branchId") Long branchId);

        @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l WHERE l.status IN ('ACTIVE', 'DELINQUENT') AND l.branch.id = :branchId")
        BigDecimal sumOutstandingBalanceByBranch(@Param("branchId") Long branchId);

        @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = 'DELINQUENT' AND l.branch.id = :branchId")
        Long countDelinquentLoansByBranch(@Param("branchId") Long branchId);

    @Query("SELECT NEW com.microfinance.loanapplications.dto.application.PortfolioStats(" +
            "SUM(CASE WHEN l.status = 'ACTIVE' THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(CASE WHEN l.status = 'ACTIVE' THEN l.outstandingBalance ELSE 0 END), 0), " +
            "COALESCE(SUM(l.principalAmount), 0), " +
            "SUM(CASE WHEN l.daysDelinquent > 0 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN l.disbursementDate >= FUNCTION('DATE_TRUNC', 'MONTH', CURRENT_DATE) THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(CASE WHEN l.disbursementDate >= FUNCTION('DATE_TRUNC', 'MONTH', CURRENT_DATE) THEN l.principalAmount ELSE 0 END), 0)" +
            ") FROM Loan l " +
            "WHERE (:branchId IS NULL OR l.branch.id = :branchId) " +
            "AND (l.disbursementDate BETWEEN CAST(:startOfDay AS date) AND CAST(:endOfDay AS date))")
    PortfolioStats getPortfolioStatistics(
            @Param("branchId") Long branchId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    // In LoanRepository
    @Query("SELECT DISTINCT lp.requiredDocuments " +
            "FROM Loan l " +
            "JOIN l.loanProduct lp " +
            "WHERE l.borrower.id = :borrowerId " +
            "AND lp.requiredDocuments IS NOT NULL")
    List<String> findRequiredDocumentStringsByBorrowerId(@Param("borrowerId") Long borrowerId);


    @Query(value = "SELECT DISTINCT lp.required_documents " +
            "FROM loans l " +
            "JOIN loan_products lp ON l.loan_product_id = lp.id " +
            "WHERE l.borrower_id = :borrowerId " +
            "AND lp.required_documents IS NOT NULL",
            nativeQuery = true)
    List<String> findRequiredDocumentStringsByBorrowerIdNative(
            @Param("borrowerId") Long borrowerId);


     @Query("SELECT l.loanProduct.id FROM Loan l WHERE l.borrower.id = :borrowerId")
     Long findLoanProductIdByBorrowerId(@Param("borrowerId") Long borrowerId);


    @Query("SELECT l FROM Loan l WHERE l.branch.id = :branchId")
    List<Loan> findByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT l FROM Loan l WHERE l.daysDelinquent >= :minDays AND (:branchId IS NULL OR l.branch.id = :branchId)")
    Page<Loan> findDelinquentLoans(@Param("minDays") Integer minDays, @Param("branchId") Long branchId, Pageable pageable);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = :status AND (:branchId IS NULL OR l.branch.id = :branchId)")
    Long countByStatusAndBranch(@Param("status") GeneralConfig.LoanStatus status, @Param("branchId") Long branchId);

    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l WHERE l.status = :status AND (:branchId IS NULL OR l.branch.id = :branchId)")
    BigDecimal sumOutstandingByStatus(@Param("status") GeneralConfig.LoanStatus status, @Param("branchId") Long branchId);

    // Get recent disbursements
    List<Loan> findByStatusAndDisbursementDateIsNotNull(
            GeneralConfig.LoanStatus status, Pageable pageable);

    // Count by status
    long countByStatus(GeneralConfig.LoanStatus status);

    // Sum disbursed amounts since a given date
    @Query("SELECT COALESCE(SUM(l.netDisbursementAmount), 0) FROM Loan l " +
            "WHERE l.disbursementDate >= :since AND l.status = :status")
    BigDecimal sumDisbursedAmountSince(@Param("since") LocalDateTime since,
                                       @Param("status") GeneralConfig.LoanStatus status);

    // Convenience method for ACTIVE status
    default BigDecimal sumDisbursedAmountSince(LocalDateTime since) {
        return sumDisbursedAmountSince(since, GeneralConfig.LoanStatus.ACTIVE);
    }


    // Get recent disbursements
    @Query("SELECT l FROM Loan l WHERE l.status = :status AND l.disbursementDate IS NOT NULL " +
            "ORDER BY l.disbursementDate DESC")
    List<Loan> findRecentDisbursements(@Param("status") GeneralConfig.LoanStatus status, Pageable pageable);

    // Sum disbursed amounts for today
    @Query("SELECT COALESCE(SUM(l.netDisbursementAmount), 0) FROM Loan l " +
            "WHERE l.status = :status AND l.disbursementDate >= :startOfDay")
    BigDecimal sumDisbursedAmountForToday(@Param("status") GeneralConfig.LoanStatus status,
                                          @Param("startOfDay") LocalDate startOfDay);

    // Sum disbursed amounts for this week
    @Query("SELECT COALESCE(SUM(l.netDisbursementAmount), 0) FROM Loan l " +
            "WHERE l.status = :status AND l.disbursementDate >= :startOfWeek")
    BigDecimal sumDisbursedAmountForWeek(@Param("status") GeneralConfig.LoanStatus status,
                                         @Param("startOfWeek") LocalDate startOfWeek);

    // Sum disbursed amounts for this month
    @Query("SELECT COALESCE(SUM(l.netDisbursementAmount), 0) FROM Loan l " +
            "WHERE l.status = :status AND l.disbursementDate >= :startOfMonth")
    BigDecimal sumDisbursedAmountForMonth(@Param("status") GeneralConfig.LoanStatus status,
                                          @Param("startOfMonth") LocalDate startOfMonth);



    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.disbursementDate BETWEEN :startDate AND :endDate")
    List<Loan> findDisbursedLoansByDateRange(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    @Query("SELECT l FROM Loan l WHERE l.branch.id = :branchId AND l.status = 'ACTIVE' " +
            "AND l.disbursementDate BETWEEN :startDate AND :endDate")
    List<Loan> findDisbursedLoansByBranchAndDateRange(@Param("branchId") Long branchId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);


        // Find loans eligible for write-off
        @Query("SELECT l FROM Loan l WHERE l.status IN ('ACTIVE', 'DELINQUENT') " +
                "AND (l.writeOffStatus IS NULL OR l.writeOffStatus != 'APPROVED')")
        List<Loan> findEligibleForWriteOff();

        // Find written-off loans with filters
        @Query("SELECT l FROM Loan l WHERE l.status = 'WRITTEN_OFF' " +
                "AND (:branchId IS NULL OR l.branch.id = :branchId) " +
                "AND (:startDate IS NULL OR l.writeOffDate >= :startDate) " +
                "AND (:endDate IS NULL OR l.writeOffDate <= :endDate) " +
                "AND (:recoveryPlan IS NULL OR l.recoveryPlan = :recoveryPlan) " +
                "AND (:searchTerm IS NULL OR (l.loanAccountNumber LIKE %:searchTerm% OR " +
                "l.borrower.firstName LIKE %:searchTerm% OR l.borrower.lastName LIKE %:searchTerm%))")
        Page<Loan> findWrittenOffLoans(@Param("branchId") Long branchId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("recoveryPlan") String recoveryPlan,
                                       @Param("searchTerm") String searchTerm,
                                       Pageable pageable);

        // Get write-off summary statistics

    @Query("SELECT COUNT(l), COALESCE(SUM(l.writeOffAmount), 0) FROM Loan l " +
            "WHERE l.status = 'WRITTEN_OFF' AND l.writeOffDate BETWEEN :startDate AND :endDate")
    List<Object[]> getWriteOffStats(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

        // Get write-offs by reason
        @Query("SELECT l.writeOffReason, COUNT(l), COALESCE(SUM(l.writeOffAmount), 0) " +
                "FROM Loan l WHERE l.status = 'WRITTEN_OFF' GROUP BY l.writeOffReason")
        List<Object[]> getWriteOffsByReason();


        // Get pending write-off approvals
        @Query("SELECT l FROM Loan l WHERE l.writeOffStatus = 'PENDING'")
        List<Loan> findPendingWriteOffApprovals();

    @Query("SELECT l FROM Loan l WHERE l.status IN ('ACTIVE', 'DELINQUENT') " +
            "AND l NOT IN (SELECT DISTINCT lr.loan FROM LoanApplication lr WHERE lr.status = 'PENDING_APPROVAL') " +
            "AND (LOWER(l.loanAccountNumber) LIKE %:searchTerm% OR " +
            "LOWER(l.borrower.firstName) LIKE %:searchTerm% OR " +
            "LOWER(l.borrower.lastName) LIKE %:searchTerm% OR " +
            "LOWER(l.borrower.borrowerNumber) LIKE %:searchTerm%)")
    List<Loan> searchEligibleForRescheduling(@Param("searchTerm") String searchTerm);

    @Query("SELECT l FROM Loan l WHERE l.status IN ('ACTIVE', 'DELINQUENT') " +
            "AND l NOT IN (SELECT DISTINCT lr.loan FROM LoanApplication lr WHERE lr.status = 'PENDING_APPROVAL')")
    List<Loan> findEligibleForRescheduling();



        // Find loans by borrower ID and status in list
        List<Loan> findByBorrowerIdAndStatusIn(Long borrowerId, List<GeneralConfig.LoanStatus> statuses);

        // Find loans by borrower ID and status in list with pagination
        Page<Loan> findByBorrowerIdAndStatusIn(Long borrowerId, List<GeneralConfig.LoanStatus> statuses, Pageable pageable);

        // Find active loans for borrower (ACTIVE or DELINQUENT)
        @Query("SELECT l FROM Loan l WHERE l.borrower.id = :borrowerId AND l.status IN ('ACTIVE', 'DELINQUENT')")
        List<Loan> findActiveLoansByBorrowerId(@Param("borrowerId") Long borrowerId);

        // Find completed loans for borrower (CLOSED, PAID, COMPLETED)
        @Query("SELECT l FROM Loan l WHERE l.borrower.id = :borrowerId AND l.status IN ('CLOSED', 'PAID', 'COMPLETED')")
        List<Loan> findCompletedLoansByBorrowerId(@Param("borrowerId") Long borrowerId);

        // Find all loans for borrower with status
        @Query("SELECT l FROM Loan l WHERE l.borrower.id = :borrowerId ORDER BY l.createdAt DESC")
        List<Loan> findAllByBorrowerIdOrderByCreatedAtDesc(@Param("borrowerId") Long borrowerId);

        // Count loans by borrower and status
        long countByBorrowerIdAndStatusIn(Long borrowerId, List<GeneralConfig.LoanStatus> statuses);

        // Get loan statistics for borrower
        @Query("SELECT COUNT(l), SUM(l.principalAmount), SUM(l.outstandingBalance) " +
                "FROM Loan l WHERE l.borrower.id = :borrowerId")
        Object[] getLoanStatisticsByBorrowerId(@Param("borrowerId") Long borrowerId);

        // Find delinquent loans for borrower
        @Query("SELECT l FROM Loan l WHERE l.borrower.id = :borrowerId AND l.status = 'DELINQUENT'")
        List<Loan> findDelinquentLoansByBorrowerId(@Param("borrowerId") Long borrowerId);

        // Find loans by status for borrower with date range
        @Query("SELECT l FROM Loan l WHERE l.borrower.id = :borrowerId " +
                "AND l.status IN :statuses " +
                "AND l.disbursementDate BETWEEN :startDate AND :endDate")
        List<Loan> findByBorrowerIdAndStatusInAndDateRange(
                @Param("borrowerId") Long borrowerId,
                @Param("statuses") List<GeneralConfig.LoanStatus> statuses,
                @Param("startDate") LocalDate startDate,
                @Param("endDate") LocalDate endDate);

        // Check if borrower has any active loans
        @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END " +
                "FROM Loan l WHERE l.borrower.id = :borrowerId AND l.status IN ('ACTIVE', 'DELINQUENT')")
        boolean hasActiveLoans(@Param("borrowerId") Long borrowerId);


    /**
     * Find loans eligible for early repayment
     * - Status is ACTIVE or DELINQUENT
     * - Has at least one unpaid installment
     * - Minimum 3 months since disbursement
     */
    @Query("SELECT DISTINCT l FROM Loan l " +
            "WHERE (l.status = 'ACTIVE' OR l.status = 'DELINQUENT') " +
            "AND EXISTS (SELECT rs FROM RepaymentSchedule rs WHERE rs.loan.id = l.id AND rs.status != 'PAID') " +
            "AND l.disbursementDate IS NOT NULL " +
            "AND l.disbursementDate <= :minDate " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId) " +
            "ORDER BY l.loanAccountNumber")
    List<Loan> findEligibleForEarlyRepayments(@Param("branchId") Long branchId);

    // With a calculated minDate (3 months ago)
    default List<Loan> findEligibleForEarlyRepayment(Long branchId) {
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        return findEligibleForEarlyRepayment(branchId, threeMonthsAgo);
    }

    @Query("SELECT DISTINCT l FROM Loan l " +
            "WHERE (l.status = 'ACTIVE' OR l.status = 'DELINQUENT') " +
            "AND EXISTS (SELECT rs FROM RepaymentSchedule rs WHERE rs.loan.id = l.id AND rs.status != 'PAID') " +
            "AND l.disbursementDate IS NOT NULL " +
            //"AND l.disbursementDate <= :minDate " +
            "AND l.disbursementDate >= :minDate " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId) " +
            "ORDER BY l.loanAccountNumber")
    List<Loan> findEligibleForEarlyRepayment(
            @Param("branchId") Long branchId,
            @Param("minDate") LocalDate minDate);



    /**
     * Count paid installments for a loan
     */
    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs WHERE rs.loan.id = :loanId AND rs.status = 'PAID'")
    long countPaidInstallments(@Param("loanId") Long loanId);



    // Find loans created by specific user
    List<Loan> findByCreatedBy(Long userId);

    // Find active loans with optional filters
    @Query("SELECT l FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DISBURSED') " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId) " +
            "AND (:loanProductId IS NULL OR l.loanProduct.id = :loanProductId)")
    List<Loan> findActiveLoansForRepayment(@Param("branchId") Long branchId,
                                           @Param("loanProductId") Long loanProductId);


    @Query("SELECT l FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE') " +
            "AND l.daysDelinquent > 0 " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId) " +
            "AND (:loanOfficerId IS NULL OR l.loanOfficer.id = :loanOfficerId) " +
            "AND (:minDays IS NULL OR l.daysDelinquent >= :minDays) " +
            "AND (:maxDays IS NULL OR l.daysDelinquent <= :maxDays)")
    Page<Loan> findOverdueLoans(@Param("branchId") Long branchId,
                                @Param("loanOfficerId") Long loanOfficerId,
                                @Param("minDays") Integer minDays,
                                @Param("maxDays") Integer maxDays,
                                Pageable pageable);



    @Query("SELECT l FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE') ")
    Page<Loan> findOverdueLoansTest(@Param("branchId") Long branchId,
                                @Param("loanOfficerId") Long loanOfficerId,
                                @Param("minDays") Integer minDays,
                                @Param("maxDays") Integer maxDays,
                                Pageable pageable);




    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE') AND l.daysDelinquent > 0 " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId)")
    BigDecimal getTotalOverdueAmount(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE') AND l.daysDelinquent > 0 " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId)")
    Long getOverdueLoanCount(@Param("branchId") Long branchId);

    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l WHERE l.status = 'ACTIVE' " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId)")
    BigDecimal getTotalActivePortfolio(@Param("branchId") Long branchId);


    //collections related///

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE') " +
            "AND l.daysDelinquent > 0 " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId) " +
            "AND (:asOfDate IS NULL OR l.disbursementDate <= :asOfDate)")
    Long countOverdueLoans(@Param("branchId") Long branchId,
                           @Param("asOfDate") LocalDate asOfDate);



    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE') " +
            "AND l.daysDelinquent > 0 " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId)")
    Long countOverdueLoans(@Param("branchId") Long branchId);


    // In LoanRepository.java - Add this method
    @Query("SELECT COUNT(l) FROM Loan l " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE') " +
            "AND l.daysDelinquent >= :daysOverdue")
    Long countOverdueLoansByDays(@Param("daysOverdue") Integer daysOverdue);



    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE') " +
            "AND l.daysDelinquent > 0 " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId) " +
            "AND (l.disbursementDate <= :asOfDate)")
    BigDecimal sumOverdueAmount(@Param("branchId") Long branchId,
                                @Param("asOfDate") LocalDate asOfDate);


    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE') " +
            "AND l.daysDelinquent > 0 " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId)")
    BigDecimal sumOverdueAmount(@Param("branchId") Long branchId);


    @Query("SELECT COUNT(DISTINCT l.loanOfficer.id) FROM Loan l WHERE l.loanOfficer IS NOT NULL " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId)")
    Integer countActiveLoanOfficers(@Param("branchId") Long branchId);



    @Query("SELECT COALESCE(SUM(r.amountPaid), 0) FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND (:branchId IS NULL OR r.loan.branch.id = :branchId)")
    BigDecimal sumCollectionsByDateRange(@Param("branchId") Long branchId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(r) FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND (:branchId IS NULL OR r.loan.branch.id = :branchId)")
    Integer countCollectionsByDateRange(@Param("branchId") Long branchId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);


    @Query("SELECT l FROM Loan l WHERE l.nextPaymentDueDate < :date AND l.status = 'ACTIVE'")
    List<Loan> findOverdueLoans(@Param("date") LocalDate date, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.branch.id = :branchId AND l.nextPaymentDueDate< :date AND l.status = 'ACTIVE'")
    List<Loan> findOverdueLoansByBranch(@Param("branchId") Long branchId, @Param("date") LocalDate date, Pageable pageable);


    @Query("SELECT COALESCE(SUM(rs.totalDue), 0) FROM RepaymentSchedule rs " +
            "WHERE rs.dueDate BETWEEN :startDate AND :endDate " +
            "AND rs.loan.status IN ('ACTIVE', 'OVERDUE') " +
            "AND (:branchId IS NULL OR rs.loan.branch.id = :branchId)")
    BigDecimal sumDueByDateRange(@Param("branchId") Long branchId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);


    @Query("SELECT l FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE') " +
            "AND l.daysDelinquent > 0 " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId) " +
            "AND (:loanOfficerId IS NULL OR l.loanOfficer.id = :loanOfficerId) " +
            "AND NOT EXISTS (SELECT ca FROM CollectionAction ca WHERE ca.loan = l " +
            "                AND ca.actionDate >= :sinceDate) " +
            "ORDER BY l.daysDelinquent DESC")
    List<Loan> findOverdueLoansWithoutRecentActions(@Param("branchId") Long branchId,
                                                    @Param("loanOfficerId") Long loanOfficerId,
                                                    @Param("sinceDate") LocalDate sinceDate,
                                                    Pageable pageable);
    // In LoanRepository.java
    Page<Loan> findByLoanOfficerId(Long loanOfficerId, Pageable pageable);
    Page<Loan> findByLoanOfficerIdAndStatus(Long loanOfficerId, GeneralConfig.LoanStatus status, Pageable pageable);


    // Add these methods to your LoanRepository interface

    /**
     * Find resolved loans (closed or fully paid) by officer and date range
     */
    @Query("SELECT l FROM Loan l WHERE l.loanOfficer.id = :officerId " +
            "AND l.status IN ('CLOSED', 'PAID', 'COMPLETED') " +
            "AND l.closedDate BETWEEN :startDate AND :endDate")
    List<Loan> findResolvedLoansByOfficerAndDateRange(@Param("officerId") Long officerId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    /**
     * Find resolved loans by date range (all loans closed during the period)
     */
    @Query("SELECT l FROM Loan l WHERE l.status IN ('CLOSED', 'PAID', 'COMPLETED') " +
            "AND l.closedDate BETWEEN :startDate AND :endDate " +
            "AND (:branchId IS NULL OR l.branch.id = :branchId)")
    List<Loan> findResolvedLoansByDateRange(@Param("branchId") Long branchId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * Get all users with a specific role (for collection officers)
     */
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findUsersByRole(@Param("role") User.UserRole role);

    /**
     * Get loans assigned to a collection officer
     */
    @Query("SELECT l FROM Loan l WHERE l.loanOfficer.id = :officerId " +
            "AND (:status IS NULL OR l.status = :status)")
    Page<Loan> findLoansByCollectionOfficer(@Param("officerId") Long officerId,
                                            @Param("status") GeneralConfig.LoanStatus status,
                                            Pageable pageable);

    /**
     * Get daily collection summary for an officer
     */
    @Query("SELECT FUNCTION('DATE', r.paymentDate), COUNT(r), COALESCE(SUM(r.amountPaid), 0) " +
            "FROM LoanRepayment r " +
            "WHERE r.loan.loanOfficer.id = :officerId " +
            "AND r.paymentDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('DATE', r.paymentDate) " +
            "ORDER BY FUNCTION('DATE', r.paymentDate)")
    List<Object[]> getDailyCollectionSummaryByOfficer(@Param("officerId") Long officerId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    /**
     * Get total resolved amount by officer
     */
    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM Loan l " +
            "WHERE l.loanOfficer.id = :officerId " +
            "AND l.status IN ('CLOSED', 'PAID', 'COMPLETED') " +
            "AND l.closedDate BETWEEN :startDate AND :endDate")
    BigDecimal getResolvedAmountByOfficer(@Param("officerId") Long officerId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /**
     * Get total assigned loans amount by officer
     */
    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM Loan l " +
            "WHERE l.loanOfficer.id = :officerId")
    BigDecimal getAssignedAmountByOfficer(@Param("officerId") Long officerId);

    /**
     * Get collection actions count by officer and action type
     */
    @Query("SELECT COUNT(ca) FROM CollectionAction ca " +
            "WHERE ca.performedBy.id = :officerId " +
            "AND ca.actionType = :actionType " +
            "AND ca.actionDate BETWEEN :startDate AND :endDate")
    Long countActionsByOfficerAndType(@Param("officerId") Long officerId,
                                      @Param("actionType") GeneralConfig.ActionType actionType,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    /**
     * Get successful actions count by officer (outcomes that lead to payment)
     */
    @Query("SELECT COUNT(ca) FROM CollectionAction ca " +
            "WHERE ca.performedBy.id = :officerId " +
            "AND ca.outcome IN ('PROMISED_TO_PAY', 'FULL_PAYMENT', 'PARTIAL_PAYMENT') " +
            "AND ca.actionDate BETWEEN :startDate AND :endDate")
    Long countSuccessfulActionsByOfficer(@Param("officerId") Long officerId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);


    /**
     * Get average recovery time for loans resolved by officer - using Java calculation
     */
    @Query("SELECT l.closedDate, l.disbursementDate FROM Loan l " +
            "WHERE l.loanOfficer.id = :officerId " +
            "AND l.status IN ('CLOSED', 'PAID', 'COMPLETED') " +
            "AND l.closedDate BETWEEN :startDate AND :endDate")
    List<Object[]> getAverageRecoveryTimeByOfficer(@Param("officerId") Long officerId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    @Query("SELECT l FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE') " +
            "AND l.daysDelinquent > 0 " +
            "AND l.id NOT IN :excludedLoanIds " +
            "ORDER BY l.daysDelinquent DESC")
    List<Loan> findOverdueLoansNotInRecovery(@Param("excludedLoanIds") List<Long> excludedLoanIds);



    @Query("SELECT l FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE') " +
         //   "AND l.daysDelinquent > 0 " +
            //"AND l.id NOT IN :excludedLoanIds " +
            "ORDER BY l.daysDelinquent DESC")
    List<Loan> findOverdueLoansNotInRecoveryTest(@Param("excludedLoanIds") List<Long> excludedLoanIds);

    // ==================== REPORT METHODS ====================


    /**
     * Count active loans for reports
     * Note: Uses same logic as existing methods but named distinctly for reports
     */
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT')")
    Integer countActiveLoansForReport();

    /**
     * Count total disbursed loans for reports
     */
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.disbursementDate IS NOT NULL")
    Integer countDisbursedLoansForReport();

    /**
     * Sum total disbursed amount for reports
     */
    @Query("SELECT COALESCE(SUM(l.netDisbursementAmount), 0) FROM Loan l WHERE l.disbursementDate IS NOT NULL")
    BigDecimal sumDisbursedAmountForReport();

    /**
     * Sum total outstanding balance for reports
     */
    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT')")
    BigDecimal sumOutstandingBalanceForReport();

    /**
     * Sum write-off amount for reports
     */
    @Query("SELECT COALESCE(SUM(l.writeOffAmount), 0) FROM Loan l WHERE l.status = 'WRITTEN_OFF'")
    BigDecimal sumWriteOffAmountForReport();

    /**
     * Sum recovered amount from write-offs for reports
     */
 //   @Query("SELECT COALESCE(SUM(l.recoveredAmount), 0) FROM Loan l WHERE l.status = 'WRITTEN_OFF'")
 //   BigDecimal sumRecoveredAmountForReport();

    /**
     * Calculate Portfolio at Risk for given days overdue (for reports)
     */
    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT') " +
            "AND l.daysDelinquent >= :daysOverdue")
    BigDecimal calculateParForReport(@Param("daysOverdue") Integer daysOverdue);

    /**
     * Calculate outstanding amount for aging range (for reports)
     */
    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT') " +
            "AND l.daysDelinquent BETWEEN :minDays AND :maxDays")
    BigDecimal calculateAgingPortfolioForReport(@Param("minDays") Integer minDays,
                                                @Param("maxDays") Integer maxDays);

    /**
     * Get portfolio by product type for reports
     */
    @Query("SELECT NEW com.microfinance.reports.dto.ProductPortfolioDto(" +
            "lp.name, " +
            "COUNT(l), " +
            "COALESCE(SUM(l.outstandingBalance), 0), " +
            "COALESCE(SUM(CASE WHEN l.daysDelinquent >= 30 THEN l.outstandingBalance ELSE 0 END), 0), " +
            "AVG(lp.interestRate)) " +
            "FROM Loan l " +
            "JOIN l.loanProduct lp " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT') " +
            "GROUP BY lp.name")
    List<ProductPortfolioDto> getPortfolioByProductForReport();

    /**
     * Get portfolio by branch for reports
     */
    @Query("SELECT NEW com.microfinance.reports.dto.ProductPortfolioDto(" +
            "b.name, " +
            "COUNT(l), " +
            "COALESCE(SUM(l.outstandingBalance), 0), " +
            "COALESCE(SUM(CASE WHEN l.daysDelinquent >= 30 THEN l.outstandingBalance ELSE 0 END), 0), " +
            "0.0) " +
            "FROM Loan l " +
            "JOIN l.branch b " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT') " +
            "GROUP BY b.name")
    List<ProductPortfolioDto> getPortfolioByBranchForReport();


    // In LoanRepository.java - Change to Object[]
    @Query("SELECT lp.name, " +
            "COUNT(l), " +
            "COALESCE(SUM(l.outstandingBalance), 0), " +
            "COALESCE(SUM(CASE WHEN l.daysDelinquent >= 30 THEN l.outstandingBalance ELSE 0 END), 0), " +
            "COALESCE(AVG(lp.interestRate), 0) " +
            "FROM Loan l " +
            "JOIN l.loanProduct lp " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT') " +
            "GROUP BY lp.name")
    List<Object[]> getPortfolioByProductForReportDash();


    /**
     * Sum non-performing loans (90+ days overdue) for reports
     */
    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT') AND l.daysDelinquent >= 90")
    BigDecimal sumNonPerformingLoansForReport();

    /**
     * Sum provision amount for reports
     */

    @Query("SELECT COALESCE(SUM(l.outstandingBalance * lp.provisionRate / 100), 0) FROM Loan l " +
            "JOIN l.loanProduct lp " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT')")
    BigDecimal sumProvisionAmountForReport();

    /**
     * Calculate average interest rate for reports
     */
    @Query("SELECT COALESCE(AVG(lp.interestRate), 0) FROM Loan l " +
            "JOIN l.loanProduct lp " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT')")
    BigDecimal calculateAverageInterestRateForReport();

    /**
     * Find maximum interest rate for reports
     */
    @Query("SELECT COALESCE(MAX(lp.interestRate), 0) FROM Loan l " +
            "JOIN l.loanProduct lp " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT')")
    BigDecimal findMaxInterestRateForReport();

    /**
     * Find minimum interest rate for reports
     */
    @Query("SELECT COALESCE(MIN(lp.interestRate), 0) FROM Loan l " +
            "JOIN l.loanProduct lp " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT')")
    BigDecimal findMinInterestRateForReport();

    /**
     * Count loans exceeding interest rate cap for reports
     */
    @Query("SELECT COUNT(l) FROM Loan l " +
            "JOIN l.loanProduct lp " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT') " +
            "AND lp.interestRate > :rateCap")
    Integer countLoansExceedingRateCapForReport(@Param("rateCap") BigDecimal rateCap);

    /**
     * Count loans created in date range for reports
     */
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.createdAt BETWEEN :startDate AND :endDate")
    Integer countLoansCreatedInPeriodForReport(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Count loans disbursed in date range for reports
     */
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.disbursementDate BETWEEN :startDate AND :endDate")
    Integer countLoansDisbursedInPeriodForReport(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * Get total cash and bank balance for reports
     */
/*
    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.accountType IN ('CASH', 'BANK')")
    BigDecimal getTotalCashAndBankForReport();*/
    default BigDecimal getTotalCashAndBankForReport() {
        // TODO: Implement Account Later
        return BigDecimal.valueOf(10000000); // Return default equity amount
    }
    /**
     * Get total receivables for reports
     */
    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT')")
    BigDecimal getTotalReceivablesForReport();

    /**
     * Get total liabilities for reports
     */
    @Query("SELECT COALESCE(SUM(l.principalAmount * 0.1), 0) FROM Loan l") // Placeholder - adjust based on your schema
    BigDecimal getTotalLiabilitiesForReport();

    /**
     * Calculate capital adequacy ratio for reports
     */

    // In LoanRepository.java - Return a placeholder value
    default BigDecimal getTotalEquityForReport() {
        // TODO: Implement when equity tracking is added
      //  log.warn("Equity tracking not implemented yet, returning default value");
        return BigDecimal.valueOf(10000000); // Return default equity amount
    }

    /**
     * Get KYC statistics for reports
     */
    @Query("SELECT COUNT(b), SUM(CASE WHEN b.kycStatus = 'VERIFIED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN b.kycStatus = 'PENDING' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN b.kycExpiryDate < CURRENT_DATE THEN 1 ELSE 0 END) " +
            "FROM Borrower b")
    Object[] getKYCStatisticsForReport();



    // Add these methods for reporting purposes (safe to add, won't affect existing code)

    /**
     * Sum outstanding amount for loans with overdue days >= specified days (for reports)
     */
    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT') " +
            "AND l.daysDelinquent >= :daysOverdue")
    BigDecimal sumOutstandingForOverdueDaysForReport(@Param("daysOverdue") Integer daysOverdue);
/**
  * Sum outstanding amount for loans within a specific overdue days range (for reports)
 *
         * @param minDays Minimum number of days overdue (inclusive)
 * @param maxDays Maximum number of days overdue (inclusive)
 * @param asOfDate The date to calculate as of (loans disbursed on or before this date)
 * @return Total outstanding amount for loans meeting the criteria
 */
    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT') " +
            "AND l.daysDelinquent BETWEEN :minDays AND :maxDays " +
            "AND (:asOfDate IS NULL OR l.disbursementDate <= :asOfDate)")
    BigDecimal sumOutstandingForAgingRangeForReport(@Param("minDays") Integer minDays,
                                                    @Param("maxDays") Integer maxDays,
                                                    @Param("asOfDate") LocalDate asOfDate);


    /**
     * Sum outstanding amount for loans with overdue days >= specified days (for reports)
     *
     * @param daysOverdue Minimum number of days overdue
     * @param asOfDate The date to calculate as of (loans disbursed on or before this date)
     * @return Total outstanding amount for loans meeting the criteria
     */
    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l " +
            "WHERE l.status IN ('ACTIVE', 'OVERDUE', 'DELINQUENT') " +
            "AND l.daysDelinquent >= :daysOverdue " +
            "AND (:asOfDate IS NULL OR l.disbursementDate <= :asOfDate)")
    BigDecimal sumOutstandingForOverdueDaysForReport(@Param("daysOverdue") Integer daysOverdue,
                                                     @Param("asOfDate") LocalDate asOfDate);






    /// NEW////
    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l")
    BigDecimal sumOutstandingBalance();


    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l WHERE l.createdAt BETWEEN :start AND :end")
    BigDecimal sumOutstandingBalanceByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l WHERE l.branch.id = :branchId AND l.createdAt BETWEEN :start AND :end")
    BigDecimal sumOutstandingBalanceByBranchAndDateRange(@Param("branchId") Long branchId,
                                                         @Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.riskRating = 'HIGH'")
    long countHighRisk();

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.branch.id = :branchId AND l.riskRating = 'HIGH'")
    long countHighRiskByBranch(@Param("branchId") Long branchId);


    @Query("SELECT COUNT(l) FROM Loan l WHERE l.daysDelinquent > 90 AND l.status = 'ACTIVE'")
    long countHighRiskByDelinquency();

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.branch.id = :branchId AND l.daysDelinquent > 90 AND l.status = 'ACTIVE'")
    long countHighRiskByBranchAndDelinquency(@Param("branchId") Long branchId);


    @Query("SELECT l.loanProduct.name, SUM(l.principalAmount) FROM Loan l GROUP BY l.loanProduct.name")
    List<Object[]> getPortfolioDistribution();

    @Query("SELECT l.loanProduct.name, SUM(l.principalAmount) FROM Loan l WHERE l.branch.id = :branchId GROUP BY l.loanProduct.name")
    List<Object[]> getPortfolioDistributionByBranch(@Param("branchId") Long branchId);

    @Query(value = "SELECT b.id, b.first_name, b.last_name, SUM(l.principal_amount) as total_borrowed, COUNT(l.id) as active_loans " +
            "FROM borrowers b JOIN loans l ON b.id = l.borrower_id " +
            "WHERE l.status = 'ACTIVE' " +
            "GROUP BY b.id, b.first_name, b.last_name " +
            "ORDER BY total_borrowed DESC", nativeQuery = true)
    List<Object[]> findTopBorrowers(Pageable pageable);

    @Query(value = "SELECT b.id, b.first_name, b.last_name, SUM(l.principal_amount) as total_borrowed, COUNT(l.id) as active_loans " +
            "FROM borrowers b JOIN loans l ON b.id = l.borrower_id " +
            "WHERE l.status = 'ACTIVE' AND l.branch_id = :branchId " +
            "GROUP BY b.id, b.first_name, b.last_name " +
            "ORDER BY total_borrowed DESC", nativeQuery = true)
    List<Object[]> findTopBorrowersByBranch(@Param("branchId") Long branchId, Pageable pageable);

    //new//
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = :status AND l.branch.id = :branchId")
    int countByStatusAndBranchId(@Param("status") String status, @Param("branchId") Long branchId);

    int countByStatus(String status);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = :status AND l.disbursementDate BETWEEN :start AND :end")
    int countByStatusAndDisbursementDateBetween(@Param("status") GeneralConfig.LoanStatus status,
                                                @Param("start") LocalDate start,
                                                @Param("end") LocalDate end);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = :status AND l.disbursementDate BETWEEN :start AND :end AND l.branch.id = :branchId")
    int countByStatusAndDisbursementDateBetweenAndBranchId(@Param("status") GeneralConfig.LoanStatus status,
                                                           @Param("start") LocalDate start,
                                                           @Param("end") LocalDate end,
                                                           @Param("branchId") Long branchId);

    // Date-based queries for growth calculations
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.createdAt BETWEEN :start AND :end")
    long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


    // Add these methods
    @Query("SELECT l FROM Loan l WHERE l.disbursementDate IS NOT NULL ORDER BY l.disbursementDate DESC")
    List<Loan> findTopByDisbursementDateNotNullOrderByDisbursementDateDesc(Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.branch.id = :branchId AND l.disbursementDate IS NOT NULL ORDER BY l.disbursementDate DESC")
    List<Loan> findTopByBranchIdAndDisbursementDateNotNullOrderByDisbursementDateDesc(@Param("branchId") Long branchId, Pageable pageable);

    // Count loans with any risk rating (not null)
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.riskRating IS NOT NULL AND l.status = 'ACTIVE'")
    long countByRiskRatingNotNull();

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.branch.id = :branchId AND l.riskRating IS NOT NULL AND l.status = 'ACTIVE'")
    long countByRiskRatingNotNullAndBranch(@Param("branchId") Long branchId);



    // Add these methods
    @Query("SELECT l FROM Loan l WHERE l.borrower.id = :borrowerId ORDER BY l.createdAt DESC")
    List<Loan> findByBorrowerIdOrderByCreatedAtDesc(@Param("borrowerId") Long borrowerId, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.borrower.id = :borrowerId AND l.disbursementDate IS NOT NULL ORDER BY l.disbursementDate DESC")
    List<Loan> findByBorrowerIdAndDisbursementDateNotNullOrderByDisbursementDateDesc(@Param("borrowerId") Long borrowerId, Pageable pageable);





}