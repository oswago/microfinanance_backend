package com.microfinance.loanapplications.repository;

import com.microfinance.loanapplications.dto.repayment.RepaymentReceiptDto;
import com.microfinance.loanapplications.entity.LoanRepayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.microfinance.base.utils.GeneralConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {
    
    // Existing methods
    List<LoanRepayment> findByLoanIdOrderByPaymentDateDesc(Long loanId);
    
    Page<LoanRepayment> findByLoanId(Long loanId, Pageable pageable);
    
    Optional<LoanRepayment> findByReceiptNumber(String receiptNumber);
    
    @Query("SELECT SUM(lr.amountPaid) FROM LoanRepayment lr WHERE lr.loan.id = :loanId")
    BigDecimal getTotalRepaidByLoan(@Param("loanId") Long loanId);
    
    @Query("SELECT lr FROM LoanRepayment lr WHERE lr.paymentDate BETWEEN :startDate AND :endDate")
    List<LoanRepayment> findRepaymentsBetweenDates(@Param("startDate") LocalDate startDate, 
                                                  @Param("endDate") LocalDate endDate);
    
    @Query("SELECT lr FROM LoanRepayment lr WHERE lr.loan.borrower.id = :borrowerId ORDER BY lr.paymentDate DESC")
    List<LoanRepayment> findByBorrowerId(@Param("borrowerId") Long borrowerId);
    
    // NEW METHODS FOR SERVICE IMPLEMENTATION
    
    // For repayment history with pagination and reversal filter
    Page<LoanRepayment> findByLoanIdAndIsReversedFalse(Long loanId, Pageable pageable);
    
    Page<LoanRepayment> findByLoanBorrowerIdAndIsReversedFalse(Long borrowerId, Pageable pageable);
    
    // Collection performance queries
    @Query("SELECT COUNT(DISTINCT lr.loan) FROM LoanRepayment lr WHERE lr.receivedBy.id = :officerId AND lr.paymentDate BETWEEN :startDate AND :endDate")
    Long countUniqueLoansCollectedByOfficer(@Param("officerId") Long officerId, 
                                           @Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);
    
    @Query("SELECT SUM(lr.amountPaid) FROM LoanRepayment lr WHERE lr.receivedBy.id = :officerId AND lr.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalCollectionByOfficer(@Param("officerId") Long officerId, 
                                          @Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(lr) FROM LoanRepayment lr WHERE lr.receivedBy.id = :officerId AND lr.paymentDate BETWEEN :startDate AND :endDate")
    Long countRepaymentsByOfficer(@Param("officerId") Long officerId, 
                                 @Param("startDate") LocalDate startDate, 
                                 @Param("endDate") LocalDate endDate);
    
    // Daily collection report queries
    @Query("SELECT SUM(lr.amountPaid) FROM LoanRepayment lr WHERE lr.paymentDate = :date AND lr.isReversed = false")
    BigDecimal getDailyCollectionTotal(@Param("date") LocalDate date);
    
    @Query("SELECT SUM(lr.amountPaid) FROM LoanRepayment lr WHERE lr.paymentDate = :date AND lr.loan.branch.id = :branchId AND lr.isReversed = false")
    BigDecimal getDailyCollectionByBranch(@Param("date") LocalDate date, @Param("branchId") Long branchId);
    
    @Query("SELECT SUM(lr.amountPaid) FROM LoanRepayment lr WHERE lr.paymentDate = :date AND lr.receivedBy.id = :officerId AND lr.isReversed = false")
    BigDecimal getDailyCollectionByOfficer(@Param("date") LocalDate date, @Param("officerId") Long officerId);
    
    @Query("SELECT lr.paymentMethod, SUM(lr.amountPaid) FROM LoanRepayment lr WHERE lr.paymentDate = :date AND lr.isReversed = false GROUP BY lr.paymentMethod")
    List<Object[]> getDailyCollectionByPaymentMethod(@Param("date") LocalDate date);


    // ✅ Find all repayments for a specific installment
    List<LoanRepayment> findByInstallmentId(Long installmentId);

    // ✅ Find all repayments for a loan
    List<LoanRepayment> findByLoanId(Long loanId);

    // Reversal tracking
    List<LoanRepayment> findByIsReversedTrueAndReversedAtBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COUNT(lr) FROM LoanRepayment lr WHERE lr.isReversed = true AND lr.reversedAt BETWEEN :startDate AND :endDate")
    Long countReversedRepaymentsInPeriod(@Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate);
    
    // Payment method analytics
    @Query("SELECT lr.paymentMethod, COUNT(lr), SUM(lr.amountPaid) FROM LoanRepayment lr WHERE lr.paymentDate BETWEEN :startDate AND :endDate AND lr.isReversed = false GROUP BY lr.paymentMethod")
    List<Object[]> getPaymentMethodAnalytics(@Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate);
    
    // Loan status queries
    @Query("SELECT lr.loan.status, COUNT(lr), SUM(lr.amountPaid) FROM LoanRepayment lr WHERE lr.paymentDate BETWEEN :startDate AND :endDate AND lr.isReversed = false GROUP BY lr.loan.status")
    List<Object[]> getCollectionByLoanStatus(@Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate);
    
    // Bulk repayment support
    List<LoanRepayment> findByTransactionReferenceIn(List<String> transactionReferences);
    
    // For reconciliation
    @Query("SELECT lr FROM LoanRepayment lr WHERE lr.paymentDate = :date AND lr.transactionReference IS NOT NULL")
    List<LoanRepayment> findRepaymentsWithTransactionRef(@Param("date") LocalDate date);
    
    // Performance optimization - recent repayments
    @Query("SELECT lr FROM LoanRepayment lr WHERE lr.loan.id = :loanId AND lr.paymentDate >= :sinceDate ORDER BY lr.paymentDate DESC")
    List<LoanRepayment> findRecentRepaymentsByLoan(@Param("loanId") Long loanId, 
                                                  @Param("sinceDate") LocalDate sinceDate);
    
    // Statistics for dashboard
    @Query("SELECT COUNT(lr), SUM(lr.amountPaid), AVG(lr.amountPaid) FROM LoanRepayment lr WHERE lr.paymentDate = :date AND lr.isReversed = false")
    Object[] getDailyCollectionStats(@Param("date") LocalDate date);
    
    // Find repayments by status
    Page<LoanRepayment> findByStatusAndIsReversedFalse(String status, Pageable pageable);
    
    // Find for specific loan officer
    Page<LoanRepayment> findByReceivedByIdAndIsReversedFalse(Long officerId, Pageable pageable);


    Page<LoanRepayment> findByLoanIdOrderByPaymentDateDesc(Long loanId, Pageable pageable);


    /**
     * Find the most recent LoanRepayment for a loan
     * This returns the latest LoanRepayment by payment date
     */
    Optional<LoanRepayment> findTopByLoanIdOrderByPaymentDateDesc(Long loanId);

    /**
     * Find the most recent LoanRepayment for a loan with a specific status
     */
    Optional<LoanRepayment> findTopByLoanIdAndStatusOrderByPaymentDateDesc(
            Long loanId,String repaymentStatus);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM LoanRepayment r WHERE r.loan.id = :loanId")
    BigDecimal calculateTotalPaid(@Param("loanId") Long loanId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM LoanRepayment r " +
            "WHERE r.paymentDate >= :fromDate AND (:branchId IS NULL OR r.loan.branch.id = :branchId)")
    BigDecimal calculateLoanRepaymentsSince(
            @Param("fromDate") LocalDate fromDate,
            @Param("branchId") Long branchId);

    @Query("SELECT r FROM LoanRepayment r WHERE r.loan.id = :loanId " +
            "AND r.paymentDate BETWEEN :startDate AND :endDate ORDER BY r.paymentDate DESC")
    List<LoanRepayment> findLoanRepaymentsInPeriod(
            @Param("loanId") Long loanId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(r) FROM LoanRepayment r WHERE r.loan.id = :loanId")
    long countByLoanId(@Param("loanId") Long loanId);

    @Query("SELECT r FROM LoanRepayment r WHERE r.paymentDate = :date AND r.loan.branch.id = :branchId")
    List<LoanRepayment> findByPaymentDateAndBranch(
            @Param("date") LocalDate date,
            @Param("branchId") Long branchId);

    @Query("SELECT r FROM LoanRepayment r WHERE r.paymentMethod = :method AND r.paymentDate BETWEEN :startDate AND :endDate")
    List<LoanRepayment> findByPaymentMethodAndDateRange(
            @Param("method") String method,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM LoanRepayment r WHERE " +
            "(:loanId IS NULL OR r.loan.id = :loanId) AND " +
            "(:paymentMethod IS NULL OR r.paymentMethod = :paymentMethod) AND " +
            "(:startDate IS NULL OR r.paymentDate >= :startDate) AND " +
            "(:endDate IS NULL OR r.paymentDate <= :endDate)")
    Page<LoanRepayment> findLoanRepaymentsByFilters(
            @Param("loanId") Long loanId,
            @Param("paymentMethod") String paymentMethod,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    boolean existsByTransactionReference(String transactionReference);

    boolean existsByReceiptNumber(String receiptNumber);


    // Add these to LoanRepaymentRepository.java

    @Query("SELECT lr FROM LoanRepayment lr WHERE lr.paymentDate = :date AND lr.isReversed = false ORDER BY lr.paymentDate DESC")
    Page<LoanRepayment> findByPaymentDateAndIsReversedFalse(@Param("date") LocalDate date, Pageable pageable);

    @Query("SELECT COUNT(lr) FROM LoanRepayment lr WHERE lr.paymentDate BETWEEN :startDate AND :endDate AND lr.isReversed = false")
    Long countRepaymentsBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(lr) FROM LoanRepayment lr WHERE lr.paymentDate BETWEEN :startDate AND :endDate AND lr.isReversed = false AND lr.penaltyAmount = 0")
    Long countOnTimeRepaymentsBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(lr.amountPaid), 0) FROM LoanRepayment lr WHERE lr.paymentDate BETWEEN :startDate AND :endDate AND lr.isReversed = false")
    BigDecimal getTotalCollectionBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT lr FROM LoanRepayment lr WHERE lr.isReversed = false ORDER BY lr.paymentDate DESC")
    Page<LoanRepayment> findRecentRepayments(Pageable pageable);


    // In your LoanRepaymentRepository.java
    @Query("SELECT new com.microfinance.loanapplications.dto.repayment.RepaymentReceiptDto(" +
            "r.id, " +
            "r.receiptNumber, " +
            "l.id, " +
            "l.loanAccountNumber, " +
            "CONCAT(b.firstName, ' ', COALESCE(b.lastName, '')), " +
            "b.borrowerNumber, " +
            "br.name, " +
            "r.amountPaid, " +
            "r.principalAmount, " +
            "r.interestAmount, " +
            "r.penaltyAmount, " +
            "r.paymentDate, " +
            "r.paymentMethod, " +
            "r.transactionReference, " +
            "CONCAT(u.firstName, ' ', COALESCE(u.lastName, '')), " +
            "r.createdAt) " +
            "FROM LoanRepayment r " +
            "LEFT JOIN r.loan l " +
            "LEFT JOIN l.borrower b " +
            "LEFT JOIN l.branch br " +
            "LEFT JOIN r.receivedBy u " +
            "WHERE r.id = :id")
    Optional<RepaymentReceiptDto> findReceiptDtoById(@Param("id") Long id);

    // For multiple repayments
    @Query("SELECT new com.microfinance.loanapplications.dto.repayment.RepaymentReceiptDto(" +
            "r.id, r.receiptNumber, l.id, l.loanAccountNumber, " +
            "CONCAT(b.firstName, ' ', COALESCE(b.lastName, '')), b.borrowerNumber, " +
            "br.name, r.amountPaid, r.principalAmount, r.interestAmount, " +
            "r.penaltyAmount, r.paymentDate, r.paymentMethod, r.transactionReference, " +
            "CONCAT(u.firstName, ' ', COALESCE(u.lastName, '')), r.createdAt) " +
            "FROM LoanRepayment r " +
            "LEFT JOIN r.loan l " +
            "LEFT JOIN l.borrower b " +
            "LEFT JOIN l.branch br " +
            "LEFT JOIN r.receivedBy u " +
            "WHERE r.loan.id = :loanId " +
            "ORDER BY r.paymentDate DESC")
    List<RepaymentReceiptDto> findReceiptDtosByLoanId(@Param("loanId") Long loanId, Pageable pageable);



    // Add these methods to your LoanRepaymentRepository.java

// ==================== FINANCIAL REPORT METHODS ====================

    /**
     * Sum total interest collected in a date range
     * Used for financial report - Interest Income
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Total interest collected
     */
    @Query("SELECT COALESCE(SUM(r.interestAmount), 0) FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    BigDecimal sumInterestCollected(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    /**
     * Sum total fees collected in a date range
     * Used for financial report - Fee Income
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Total fees collected
     */
    @Query("SELECT COALESCE(SUM(r.feesAmount), 0) FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    BigDecimal sumFeesCollected(@Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    /**
     * Sum total penalties collected in a date range
     * Used for financial report - Penalty Income
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Total penalties collected
     */
    @Query("SELECT COALESCE(SUM(r.penaltyAmount), 0) FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    BigDecimal sumPenaltiesCollected(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);


    /**
     * Get total penalties collected in period for reports
     */
    @Query("SELECT COALESCE(SUM(rs.penaltyAmount), 0) FROM RepaymentSchedule rs " +
            "WHERE rs.paidDate BETWEEN :startDate AND :endDate")
    BigDecimal sumPenaltiesCollectedForReport(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);



    /**
     * Sum total principal collected in a date range
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Total principal collected
     */
    @Query("SELECT COALESCE(SUM(r.principalAmount), 0) FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    BigDecimal sumPrincipalCollected(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    /**
     * Sum total amount paid in a date range (principal + interest + fees + penalties)
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Total amount paid
     */
    @Query("SELECT COALESCE(SUM(r.amountPaid), 0) FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    BigDecimal sumTotalAmountPaid(@Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    /**
     * Get repayment statistics by payment method for financial reports
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of objects [paymentMethod, count, totalAmount]
     */
    @Query("SELECT r.paymentMethod, COUNT(r), COALESCE(SUM(r.amountPaid), 0) " +
            "FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false " +
            "GROUP BY r.paymentMethod")
    List<Object[]> getRepaymentStatsByMethodForReport(@Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    /**
     * Get daily repayment summary for financial reports
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of objects [paymentDate, count, totalAmount]
     */
    @Query("SELECT r.paymentDate, COUNT(r), COALESCE(SUM(r.amountPaid), 0) " +
            "FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false " +
            "GROUP BY r.paymentDate " +
            "ORDER BY r.paymentDate")
    List<Object[]> getDailyRepaymentSummaryForReport(@Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    /**
     * Get monthly repayment summary for financial reports
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of objects [year, month, count, totalAmount]
     */
    @Query("SELECT YEAR(r.paymentDate), MONTH(r.paymentDate), COUNT(r), COALESCE(SUM(r.amountPaid), 0) " +
            "FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false " +
            "GROUP BY YEAR(r.paymentDate), MONTH(r.paymentDate) " +
            "ORDER BY YEAR(r.paymentDate), MONTH(r.paymentDate)")
    List<Object[]> getMonthlyRepaymentSummaryForReport(@Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    /**
     * Get total interest collected by branch
     *
     * @param branchId Branch ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Total interest collected by branch
     */
    @Query("SELECT COALESCE(SUM(r.interestAmount), 0) FROM LoanRepayment r " +
            "WHERE r.loan.branch.id = :branchId " +
            "AND r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    BigDecimal sumInterestCollectedByBranch(@Param("branchId") Long branchId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * Get total fees collected by branch
     *
     * @param branchId Branch ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Total fees collected by branch
     */
    @Query("SELECT COALESCE(SUM(r.feesAmount), 0) FROM LoanRepayment r " +
            "WHERE r.loan.branch.id = :branchId " +
            "AND r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    BigDecimal sumFeesCollectedByBranch(@Param("branchId") Long branchId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    /**
     * Get total penalties collected by branch
     *
     * @param branchId Branch ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Total penalties collected by branch
     */
    @Query("SELECT COALESCE(SUM(r.penaltyAmount), 0) FROM LoanRepayment r " +
            "WHERE r.loan.branch.id = :branchId " +
            "AND r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    BigDecimal sumPenaltiesCollectedByBranch(@Param("branchId") Long branchId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    /**
     * Get total interest collected by loan product
     *
     * @param productId Loan Product ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Total interest collected by product
     */
    @Query("SELECT COALESCE(SUM(r.interestAmount), 0) FROM LoanRepayment r " +
            "WHERE r.loan.loanProduct.id = :productId " +
            "AND r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    BigDecimal sumInterestCollectedByProduct(@Param("productId") Long productId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    /**
     * Get total fees collected by loan product
     *
     * @param productId Loan Product ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Total fees collected by product
     */
    @Query("SELECT COALESCE(SUM(r.feesAmount), 0) FROM LoanRepayment r " +
            "WHERE r.loan.loanProduct.id = :productId " +
            "AND r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    BigDecimal sumFeesCollectedByProduct(@Param("productId") Long productId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    /**
     * Get total penalties collected by loan product
     *
     * @param productId Loan Product ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Total penalties collected by product
     */
    @Query("SELECT COALESCE(SUM(r.penaltyAmount), 0) FROM LoanRepayment r " +
            "WHERE r.loan.loanProduct.id = :productId " +
            "AND r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    BigDecimal sumPenaltiesCollectedByProduct(@Param("productId") Long productId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    /**
     * Get repayment efficiency ratio (actual vs expected) for financial reports
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Efficiency ratio as percentage
     */
    @Query("SELECT COALESCE(SUM(r.amountPaid), 0) / NULLIF(COALESCE(SUM(rs.totalDue), 0), 0) * 100 " +
            "FROM LoanRepayment r " +
            "LEFT JOIN RepaymentSchedule rs ON rs.loan.id = r.loan.id AND rs.dueDate BETWEEN :startDate AND :endDate " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    BigDecimal getRepaymentEfficiencyRatio(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * Get on-time vs late repayment statistics
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Object array [onTimeCount, onTimeAmount, lateCount, lateAmount]
     */
    @Query("SELECT " +
            "SUM(CASE WHEN r.penaltyAmount = 0 THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(CASE WHEN r.penaltyAmount = 0 THEN r.amountPaid ELSE 0 END), 0), " +
            "SUM(CASE WHEN r.penaltyAmount > 0 THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(CASE WHEN r.penaltyAmount > 0 THEN r.amountPaid ELSE 0 END), 0) " +
            "FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    Object[] getOnTimeVsLateRepaymentStats(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * Get average repayment amount per day in period
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Average daily collection
     */
    @Query("SELECT COALESCE(SUM(r.amountPaid), 0) / NULLIF(COUNT(DISTINCT r.paymentDate), 0) " +
            "FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    BigDecimal getAverageDailyCollection(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);


    // Add these methods to your LoanRepaymentRepository.java

// ==================== REPAYMENT COUNT METHODS ====================

    /**
     * Count total repayments processed in a date range
     * Used for audit reports - total transactions
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of repayments processed
     */
    @Query("SELECT COUNT(r) FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    Integer countProcessedInPeriod(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    /**
     * Count total repayments processed in a date range (including reversed)
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of all repayments including reversed
     */
    @Query("SELECT COUNT(r) FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate")
    Integer countAllRepaymentsInPeriod(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    /**
     * Count repayments processed by a specific user in a date range
     *
     * @param userId User ID who processed the repayment
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of repayments processed by the user
     */
    @Query("SELECT COUNT(r) FROM LoanRepayment r " +
            "WHERE r.receivedBy.id = :userId " +
            "AND r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    Integer countProcessedByUserInPeriod(@Param("userId") Long userId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    /**
     * Count repayments processed by branch in a date range
     *
     * @param branchId Branch ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of repayments processed by branch
     */
    @Query("SELECT COUNT(r) FROM LoanRepayment r " +
            "WHERE r.loan.branch.id = :branchId " +
            "AND r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    Integer countProcessedByBranchInPeriod(@Param("branchId") Long branchId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * Count repayments processed by payment method in a date range
     *
     * @param paymentMethod Payment method (CASH, BANK_TRANSFER, etc.)
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of repayments by payment method
     */
    @Query("SELECT COUNT(r) FROM LoanRepayment r " +
            "WHERE r.paymentMethod = :paymentMethod " +
            "AND r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    Integer countProcessedByPaymentMethodInPeriod(@Param("paymentMethod") String paymentMethod,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    /**
     * Count repayments processed today
     *
     * @return Count of repayments processed today
     */
    @Query("SELECT COUNT(r) FROM LoanRepayment r " +
            "WHERE DATE(r.paymentDate) = CURRENT_DATE " +
            "AND r.isReversed = false")
    Integer countProcessedToday();

    /**
     * Count repayments processed this week
     *
     * @return Count of repayments processed this week
     */
    @Query("SELECT COUNT(r) FROM LoanRepayment r " +
            "WHERE YEARWEEK(r.paymentDate) = YEARWEEK(CURDATE()) " +
            "AND r.isReversed = false")
    Integer countProcessedThisWeek();

    /**
     * Count repayments processed this month
     *
     * @return Count of repayments processed this month
     */
    @Query("SELECT COUNT(r) FROM LoanRepayment r " +
            "WHERE YEAR(r.paymentDate) = YEAR(CURRENT_DATE) " +
            "AND MONTH(r.paymentDate) = MONTH(CURRENT_DATE) " +
            "AND r.isReversed = false")
    Integer countProcessedThisMonth();

    /**
     * Count repayments processed this year
     *
     * @return Count of repayments processed this year
     */
    @Query("SELECT COUNT(r) FROM LoanRepayment r " +
            "WHERE YEAR(r.paymentDate) = YEAR(CURRENT_DATE) " +
            "AND r.isReversed = false")
    Integer countProcessedThisYear();

    /**
     * Count repayments by status in a date range
     *
     * @param status Repayment status (COMPLETED, PENDING, FAILED)
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of repayments by status
     */
    @Query("SELECT COUNT(r) FROM LoanRepayment r " +
            "WHERE r.status = :status " +
            "AND r.paymentDate BETWEEN :startDate AND :endDate")
    Integer countByStatusInPeriod(@Param("status") String status,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    /**
     * Get daily repayment count for a date range
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of objects [paymentDate, count]
     */
    @Query("SELECT r.paymentDate, COUNT(r) FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false " +
            "GROUP BY r.paymentDate " +
            "ORDER BY r.paymentDate")
    List<Object[]> getDailyRepaymentCount(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /**
     * Get monthly repayment count for a date range
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of objects [year, month, count]
     */
    @Query("SELECT YEAR(r.paymentDate), MONTH(r.paymentDate), COUNT(r) " +
            "FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false " +
            "GROUP BY YEAR(r.paymentDate), MONTH(r.paymentDate) " +
            "ORDER BY YEAR(r.paymentDate), MONTH(r.paymentDate)")
    List<Object[]> getMonthlyRepaymentCount(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * Count reversed repayments in a date range
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of reversed repayments
     */
    @Query("SELECT COUNT(r) FROM LoanRepayment r " +
            "WHERE r.isReversed = true " +
            "AND r.reversedAt BETWEEN :startDate AND :endDate")
    Integer countReversedInPeriod(@Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    /**
     * Count successful repayments (not reversed) in a date range
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of successful repayments
     */
    @Query("SELECT COUNT(r) FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false " +
            "AND r.status = 'COMPLETED'")
    Integer countSuccessfulInPeriod(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    /**
     * Count repayments by loan product in a date range
     *
     * @param productId Loan Product ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of repayments by product
     */
    @Query("SELECT COUNT(r) FROM LoanRepayment r " +
            "WHERE r.loan.loanProduct.id = :productId " +
            "AND r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    Integer countProcessedByProductInPeriod(@Param("productId") Long productId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * Count unique loans that had repayments in a date range
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of unique loans with repayments
     */
    @Query("SELECT COUNT(DISTINCT r.loan.id) FROM LoanRepayment r " +
            "WHERE r.paymentDate BETWEEN :startDate AND :endDate " +
            "AND r.isReversed = false")
    Integer countUniqueLoansWithRepayments(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);



   // @Query("SELECT COALESCE(SUM(r.amountPaid), 0) FROM LoanRepayment r WHERE r.paymentDate BETWEEN :start AND :end AND r.isOnTime = true")
    @Query("SELECT COALESCE(SUM(r.amountPaid), 0) FROM LoanRepayment r WHERE r.paymentDate BETWEEN :start AND :end ")
    BigDecimal sumOnTimePayments(@Param("start") LocalDate start, @Param("end") LocalDate end);

    //@Query("SELECT COALESCE(SUM(r.amountPaid), 0) FROM LoanRepayment r WHERE r.paymentDate BETWEEN :start AND :end AND r.isOnTime = true AND r.loan.branch.id = :branchId")
    @Query("SELECT COALESCE(SUM(r.amountPaid), 0) FROM LoanRepayment r WHERE r.paymentDate BETWEEN :start AND :end  AND r.loan.branch.id = :branchId")
    BigDecimal sumOnTimePaymentsByBranch(@Param("start") LocalDate start,
                                         @Param("end") LocalDate end,
                                         @Param("branchId") Long branchId);

    @Query("SELECT COALESCE(SUM(r.amountPaid), 0) FROM LoanRepayment r WHERE r.paymentDate BETWEEN :start AND :end")
    BigDecimal sumAmountByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(r.amountPaid), 0) FROM LoanRepayment r WHERE r.paymentDate BETWEEN :start AND :end AND r.loan.branch.id = :branchId")
    BigDecimal sumAmountByDateRangeAndBranch(@Param("start") LocalDate start,
                                             @Param("end") LocalDate end,
                                             @Param("branchId") Long branchId);

    // Recent activities
    @Query("SELECT r FROM LoanRepayment r ORDER BY r.createdAt DESC")
    List<LoanRepayment> findTopByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT r FROM LoanRepayment r WHERE r.loan.branch.id = :branchId ORDER BY r.createdAt DESC")
    List<LoanRepayment> findTopByBranchIdOrderByCreatedAtDesc(@Param("branchId") Long branchId, Pageable pageable);


    // Add this method
    @Query("SELECT lr FROM LoanRepayment lr WHERE lr.loan.borrower.id = :borrowerId ORDER BY lr.createdAt DESC")
    List<LoanRepayment> findByLoanBorrowerIdOrderByCreatedAtDesc(@Param("borrowerId") Long borrowerId, Pageable pageable);


    List<LoanRepayment> findByLoanIdOrderByPaymentDateAsc(Long id);
}