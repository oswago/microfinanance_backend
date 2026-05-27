package com.microfinance.loanapplications.repository;

import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanapplications.entity.EarlyRepaymentRequest;
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
public interface EarlyRepaymentRepository extends JpaRepository<EarlyRepaymentRequest, Long> {

    Optional<EarlyRepaymentRequest> findByRequestNumber(String requestNumber);

    List<EarlyRepaymentRequest> findByLoanId(Long loanId);

    Page<EarlyRepaymentRequest> findByStatus(GeneralConfig.EarlyRepaymentStatus status, Pageable pageable);


    /*
    @Query("SELECT e FROM EarlyRepaymentRequest e WHERE " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:branchId IS NULL OR e.loan.branch.id = :branchId) AND " +
           "(:loanProductId IS NULL OR e.loan.loanProduct.id = :loanProductId) AND " +
           "(:search IS NULL OR " +
           "   LOWER(e.requestNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(e.loan.loanAccountNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(e.borrower.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(e.borrower.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(e.borrower.borrowerNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<EarlyRepaymentRequest> findEarlyRepaymentRequests(
            @Param("status") GeneralConfig.EarlyRepaymentStatus status,
            @Param("branchId") Long branchId,
            @Param("loanProductId") Long loanProductId,
            @Param("search") String search,
            Pageable pageable);

*/
        @Query(value = """
        SELECT DISTINCT e.* 
        FROM early_repayment_requests e
        INNER JOIN loans l ON l.id = e.loan_id
        INNER JOIN borrowers b ON b.id = e.borrower_id
        WHERE (CAST(:status AS text) IS NULL OR CAST(:status AS text) = 'ALL' OR e.status = CAST(:status AS text))
          AND (CAST(:branchId AS text) IS NULL OR CAST(:branchId AS text) = 'ALL' OR l.branch_id = CAST(:branchId AS bigint))
          AND (CAST(:loanProductId AS text) IS NULL OR CAST(:loanProductId AS text) = 'ALL' OR l.loan_product_id = CAST(:loanProductId AS bigint))
          AND (CAST(:search AS text) IS NULL OR CAST(:search AS text) = 'ALL' OR 
               LOWER(e.request_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
               LOWER(l.loan_account_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
               LOWER(b.first_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
               LOWER(b.last_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
               LOWER(b.borrower_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
        ORDER BY e.requested_date DESC
        """,
                nativeQuery = true,
                countQuery = """
        SELECT COUNT(DISTINCT e.id)
        FROM early_repayment_requests e
        INNER JOIN loans l ON l.id = e.loan_id
        INNER JOIN borrowers b ON b.id = e.borrower_id
        WHERE (CAST(:status AS text) IS NULL OR CAST(:status AS text) = 'ALL' OR e.status = CAST(:status AS text))
          AND (CAST(:branchId AS text) IS NULL OR CAST(:branchId AS text) = 'ALL' OR l.branch_id = CAST(:branchId AS bigint))
          AND (CAST(:loanProductId AS text) IS NULL OR CAST(:loanProductId AS text) = 'ALL' OR l.loan_product_id = CAST(:loanProductId AS bigint))
          AND (CAST(:search AS text) IS NULL OR CAST(:search AS text) = 'ALL' OR 
               LOWER(e.request_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
               LOWER(l.loan_account_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
               LOWER(b.first_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
               LOWER(b.last_name) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
               LOWER(b.borrower_number) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
        """)
        Page<EarlyRepaymentRequest> findEarlyRepaymentRequestsNative(
                @Param("status") String status,
                @Param("branchId") Long branchId,
                @Param("loanProductId") Long loanProductId,
                @Param("search") String search,
                Pageable pageable);

        // Public method that accepts enum - this is what your service calls
        default Page<EarlyRepaymentRequest> findEarlyRepaymentRequests(
                GeneralConfig.EarlyRepaymentStatus status,
                Long branchId,
                Long loanProductId,
                String search,
                Pageable pageable) {

            // Convert enum to string for the native query
            String statusString = status != null ? status.name() : null;
            return findEarlyRepaymentRequestsNative(statusString, branchId, loanProductId, search, pageable);
        }


    @Query("SELECT COALESCE(SUM(e.earlyRepaymentAmount), 0) FROM EarlyRepaymentRequest e WHERE e.status = 'PAID'")
    BigDecimal getTotalEarlyRepayments();

    @Query("SELECT COALESCE(SUM(e.interestSavings), 0) FROM EarlyRepaymentRequest e WHERE e.status = 'PAID'")
    BigDecimal getTotalInterestSaved();

    @Query("SELECT COUNT(e) FROM EarlyRepaymentRequest e WHERE e.status = 'PENDING' OR e.status = 'UNDER_REVIEW'")
    Integer getActiveRequests();

    @Query("SELECT COALESCE(AVG(e.discountPercentage), 0) FROM EarlyRepaymentRequest e WHERE e.status = 'APPROVED' OR e.status = 'PAID'")
    BigDecimal getAverageDiscount();

    @Query("SELECT COUNT(e) FROM EarlyRepaymentRequest e WHERE e.status = :status")
    Integer countByStatus(@Param("status") GeneralConfig.EarlyRepaymentStatus status);


    // In EarlyRepaymentRepository.java
    @Query("SELECT FUNCTION('TO_CHAR', e.requestedDate, 'YYYY-MM') as month, " +
            "COUNT(e), COALESCE(SUM(e.earlyRepaymentAmount), 0), COALESCE(SUM(e.interestSavings), 0) " +
            "FROM EarlyRepaymentRequest e WHERE e.status = 'PAID' " +
            "GROUP BY FUNCTION('TO_CHAR', e.requestedDate, 'YYYY-MM') " +
            "ORDER BY month DESC")
    List<Object[]> getMonthlyTrends();




    // History with filters method
    @Query("SELECT er FROM EarlyRepaymentRequest er " +
            "LEFT JOIN FETCH er.loan l " +
            "LEFT JOIN FETCH l.borrower b " +
            "LEFT JOIN FETCH l.branch br " +
            "LEFT JOIN FETCH l.loanProduct lp " +
            "WHERE (:status IS NULL OR er.status = :status) " +
            "AND (:branchId IS NULL OR br.id = :branchId) " +
            "AND (:loanProductId IS NULL OR lp.id = :loanProductId) " +
            "AND (CAST(:startDate AS date) IS NULL OR er.requestedDate >= :startDate) " +
            "AND (CAST(:endDate AS date) IS NULL OR er.requestedDate <= :endDate) " +
            "AND (:search IS NULL OR " +
            "   LOWER(er.requestNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(l.loanAccountNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(b.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(b.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "   LOWER(b.borrowerNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<EarlyRepaymentRequest> findHistoryWithFilters(
            @Param("status") GeneralConfig.EarlyRepaymentStatus status,
            @Param("branchId") Long branchId,
            @Param("loanProductId") Long loanProductId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable);

    // Get statistics for dashboard
    @Query("SELECT COUNT(er) FROM EarlyRepaymentRequest er WHERE er.status IN ('APPROVED', 'PAID')")
    long countCompleted();

    @Query("SELECT COUNT(er) FROM EarlyRepaymentRequest er WHERE er.status = 'PENDING'")
    long countPending();

    @Query("SELECT COALESCE(SUM(er.earlyRepaymentAmount), 0) FROM EarlyRepaymentRequest er WHERE er.status IN ('APPROVED', 'PAID')")
    BigDecimal sumTotalEarlyRepayments();

    @Query("SELECT COALESCE(SUM(er.interestSavings), 0) FROM EarlyRepaymentRequest er WHERE er.status IN ('APPROVED', 'PAID')")
    BigDecimal sumTotalInterestSaved();

    @Query("SELECT COALESCE(AVG(er.discountPercentage), 0) FROM EarlyRepaymentRequest er WHERE er.status IN ('APPROVED', 'PAID')")
    BigDecimal averageDiscount();

    // Find by date range for reports
    @Query("SELECT er FROM EarlyRepaymentRequest er " +
            "WHERE er.requestedDate BETWEEN :startDate AND :endDate " +
            "AND (:branchId IS NULL OR er.loan.branch.id = :branchId)")
    List<EarlyRepaymentRequest> findByDateRangeAndBranch(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("branchId") Long branchId);

    // Check if loan has pending early repayment request
    @Query("SELECT CASE WHEN COUNT(er) > 0 THEN true ELSE false END FROM EarlyRepaymentRequest er " +
            "WHERE er.loan.id = :loanId AND er.status = 'PENDING'")
    boolean hasPendingRequest(@Param("loanId") Long loanId);

    // Find latest request for a loan
    Optional<EarlyRepaymentRequest> findTopByLoanIdOrderByRequestedDateDesc(Long loanId);

    // Count by date range for statistics
    @Query("SELECT COUNT(er) FROM EarlyRepaymentRequest er WHERE er.requestedDate BETWEEN :startDate AND :endDate")
    long countByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Sum by date range for statistics
    @Query("SELECT COALESCE(SUM(er.earlyRepaymentAmount), 0) FROM EarlyRepaymentRequest er " +
            "WHERE er.requestedDate BETWEEN :startDate AND :endDate AND er.status IN ('APPROVED', 'PAID')")
    BigDecimal sumAmountByDateRange(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

}