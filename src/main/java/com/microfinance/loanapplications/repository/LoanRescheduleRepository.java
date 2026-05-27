package com.microfinance.loanapplications.repository;

import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.entity.LoanReschedule;
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
public interface LoanRescheduleRepository extends JpaRepository<LoanReschedule, Long> {
    
    List<LoanReschedule> findByStatus(GeneralConfig.RescheduleStatus status);
    
    List<LoanReschedule> findByLoanIdAndStatus(Long loanId, GeneralConfig.RescheduleStatus status);



    // Count methods for validation
    long countByLoanAndStatus(Loan loan, GeneralConfig.RescheduleStatus status);

    long countByLoanIdAndStatus(Long loanId, GeneralConfig.RescheduleStatus status);

    @Query("SELECT COUNT(lr) FROM LoanReschedule lr WHERE lr.loan = :loan AND lr.status = 'APPROVED'")
    long countApprovedReschedulesByLoan(@Param("loan") Loan loan);

    @Query("SELECT COUNT(lr) FROM LoanReschedule lr WHERE lr.loan.id = :loanId AND lr.status = 'APPROVED'")
    long countApprovedReschedulesByLoanId(@Param("loanId") Long loanId);

    Page<LoanReschedule> findByStatus(GeneralConfig.RescheduleStatus status, Pageable pageable);

    // Find pending requests for a specific loan
    List<LoanReschedule> findByLoanIdAndStatusOrderByRequestDateDesc(
            Long loanId, GeneralConfig.RescheduleStatus status);

    // Find by loan and date range
    List<LoanReschedule> findByLoanAndRequestDateBetween(
            Loan loan, LocalDate startDate, LocalDate endDate);

    // Find recent reschedules across all loans
    @Query("SELECT lr FROM LoanReschedule lr WHERE lr.requestDate >= :sinceDate ORDER BY lr.requestDate DESC")
    List<LoanReschedule> findRecentReschedules(@Param("sinceDate") LocalDate sinceDate);

    // Find reschedules by approver
    List<LoanReschedule> findByApprovedBy_IdOrderByApprovalDateDesc(Long approverId);

    // Find reschedules by requester
    List<LoanReschedule> findByRequestedBy_IdOrderByRequestDateDesc(Long requestedById);

    // Check if a loan has any pending reschedule requests
    boolean existsByLoanAndStatus(Loan loan, GeneralConfig.RescheduleStatus status);

    boolean existsByLoanIdAndStatus(Long loanId, GeneralConfig.RescheduleStatus status);

    // Find the latest approved reschedule for a loan
    @Query("SELECT lr FROM LoanReschedule lr WHERE lr.loan = :loan AND lr.status = 'APPROVED' ORDER BY lr.approvalDate DESC")
    Optional<LoanReschedule> findLatestApprovedReschedule(@Param("loan") Loan loan);

    @Query("SELECT lr FROM LoanReschedule lr WHERE lr.loan.id = :loanId AND lr.status = 'APPROVED' ORDER BY lr.approvalDate DESC")
    Optional<LoanReschedule> findLatestApprovedRescheduleByLoanId(@Param("loanId") Long loanId);

    // Statistics and reporting methods
    @Query("SELECT COUNT(lr) FROM LoanReschedule lr WHERE lr.status = :status AND lr.requestDate BETWEEN :startDate AND :endDate")
    long countByStatusAndRequestDateBetween(
            @Param("status") GeneralConfig.RescheduleStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT lr.loan.branch.id, COUNT(lr) FROM LoanReschedule lr WHERE lr.status = :status AND lr.requestDate BETWEEN :startDate AND :endDate GROUP BY lr.loan.branch.id")
    List<Object[]> countByStatusAndRequestDateBetweenGroupByBranch(
            @Param("status") GeneralConfig.RescheduleStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Find reschedules that are effective from a specific date
    List<LoanReschedule> findByEffectiveDate(LocalDate effectiveDate);

    List<LoanReschedule> findByEffectiveDateBetween(LocalDate startDate, LocalDate endDate);

    // Find reschedules with grace period
    List<LoanReschedule> findByGracePeriodDaysGreaterThan(Integer gracePeriodDays);

    // Find reschedules with rescheduling fee
    List<LoanReschedule> findByReschedulingFeeGreaterThan(BigDecimal minFee);


    @Query("SELECT DISTINCT lr FROM LoanReschedule lr " +
            "LEFT JOIN FETCH lr.loan l " +
            "LEFT JOIN FETCH l.borrower b " +
            "LEFT JOIN FETCH l.branch br " +
            "LEFT JOIN FETCH lr.requestedBy ru " +
            "LEFT JOIN FETCH lr.approvedBy au " +
            "WHERE (:status IS NULL OR lr.status = :status) " +
            "AND (:branchId IS NULL OR br.id = :branchId) " +
            "AND (:startDate IS NULL OR lr.requestDate >= :startDate) " +
            "AND (:endDate IS NULL OR lr.requestDate <= :endDate)")
    Page<LoanReschedule> findWithFilters(
            @Param("status") GeneralConfig.RescheduleStatus status,
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);


    @Query("SELECT DISTINCT lr FROM LoanReschedule lr " +
            "LEFT JOIN FETCH lr.loan l " +
            "LEFT JOIN FETCH l.borrower b " +
            "LEFT JOIN FETCH l.branch br " +
            "LEFT JOIN FETCH lr.requestedBy ru " +
            "LEFT JOIN FETCH lr.approvedBy au " +
            "WHERE lr.id = :id")
    Optional<LoanReschedule> findByIdWithAllDetails(@Param("id") Long id);


    @Query("SELECT COUNT(lr) FROM LoanReschedule lr WHERE lr.requestDate BETWEEN :startDate AND :endDate")
    long countByRequestDateBetween(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT AVG(DATEDIFF('SECOND', request_date, approval_date)) " +
            "FROM loan_reschedules " +
            "WHERE CAST(approval_date AS DATE) BETWEEN :startDate AND :endDate " +
            "AND status = 'APPROVED'",
            nativeQuery = true)
    Double getAverageProcessingTimeBK(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);



    @Query(value = """
    SELECT AVG(EXTRACT(EPOCH FROM (CAST(approval_date AS timestamp) - CAST(request_date AS timestamp))))
    FROM loan_reschedules
    WHERE CAST(approval_date AS date) BETWEEN :startDate AND :endDate
      AND status = 'APPROVED'
    """,
            nativeQuery = true)
    Double getAverageProcessingTime(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);


    // Alternative if you want hours instead of seconds
    /*
    @Query(value = "SELECT AVG(DATEDIFF('HOUR', request_date, approval_date)) " +
            "FROM loan_reschedules " +
            "WHERE DATE(approval_date) BETWEEN :startDate AND :endDate " +
            "AND status = 'APPROVED'",
            nativeQuery = true)
    Double getAverageProcessingTimeHours(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);
*/
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (approval_date - request_date))) / 3600.0 " +
            "FROM loan_reschedules " +
            "WHERE DATE(approval_date) BETWEEN :startDate AND :endDate " +
            "AND status = 'APPROVED'",
            nativeQuery = true)
    Double getAverageProcessingTimeHours(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);


    @Query("SELECT lr FROM LoanReschedule lr WHERE lr.loan.id IN " +
            "(SELECT l.id FROM Loan l WHERE l.status IN ('ACTIVE', 'DELINQUENT') " +
            "AND l NOT IN (SELECT DISTINCT lr2.loan FROM LoanReschedule lr2 WHERE lr2.status = 'PENDING_APPROVAL'))")
    List<Loan> findEligibleLoansForRescheduling();

    @Query("SELECT lr FROM LoanReschedule lr WHERE lr.loan.id = :loanId ORDER BY lr.requestDate DESC")
    List<LoanReschedule> findByLoanIdOrderByRequestDateDesc(@Param("loanId") Long loanId);

    @Query("SELECT CASE WHEN COUNT(lr) > 0 THEN true ELSE false END FROM LoanReschedule lr " +
            "WHERE lr.loan.id = :loanId AND lr.status = 'PENDING_APPROVAL'")
    boolean hasPendingRequest(@Param("loanId") Long loanId);


    // Count by status
    long countByStatus(GeneralConfig.RescheduleStatus status);

    // Count by status and date range with branch filter
    @Query("SELECT COUNT(lr) FROM LoanReschedule lr WHERE lr.status = :status " +
            "AND lr.requestDate BETWEEN :startDate AND :endDate " +
            "AND (:branchId IS NULL OR lr.loan.branch.id = :branchId)")
    long countByStatusAndDateRangeAndBranch(
            @Param("status") GeneralConfig.RescheduleStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("branchId") Long branchId);







}