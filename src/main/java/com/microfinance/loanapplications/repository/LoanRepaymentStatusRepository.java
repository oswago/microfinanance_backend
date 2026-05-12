package com.microfinance.loanapplications.repository;

import com.microfinance.base.utils.GeneralConfig;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.entity.LoanRepaymentStatus;
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
public interface LoanRepaymentStatusRepository extends JpaRepository<LoanRepaymentStatus, Long> {

    Optional<LoanRepaymentStatus> findByLoanId(Long loanId);

    Optional<LoanRepaymentStatus> findByLoan(Loan loan);

    List<LoanRepaymentStatus> findByIsDelinquentTrue();

    Page<LoanRepaymentStatus> findByIsDelinquentTrue(Pageable pageable);

    List<LoanRepaymentStatus> findByDelinquencyBucket(GeneralConfig.DelinquencyBucket bucket);

    @Query("SELECT lrs FROM LoanRepaymentStatus lrs WHERE lrs.daysDelinquent >= :minDays")
    List<LoanRepaymentStatus> findDelinquentLoans(@Param("minDays") Integer minDays);

    @Query("SELECT lrs FROM LoanRepaymentStatus lrs WHERE lrs.nextDueDate <= :date AND lrs.isDelinquent = false")
    List<LoanRepaymentStatus> findLoansDueOnOrBefore(@Param("date") LocalDate date);

    @Query("SELECT COUNT(lrs) FROM LoanRepaymentStatus lrs WHERE lrs.isDelinquent = true")
    long countDelinquentLoans();

    @Query("SELECT COALESCE(SUM(lrs.totalArrears), 0) FROM LoanRepaymentStatus lrs WHERE lrs.isDelinquent = true")
    BigDecimal getTotalArrears();

    @Query("SELECT lrs FROM LoanRepaymentStatus lrs WHERE " +
           "(:bucket IS NULL OR lrs.delinquencyBucket = :bucket) AND " +
           "(:branchId IS NULL OR lrs.loan.branch.id = :branchId)")
    Page<LoanRepaymentStatus> findByDelinquencyBucketAndBranch(
            @Param("bucket") GeneralConfig.DelinquencyBucket bucket,
            @Param("branchId") Long branchId,
            Pageable pageable);

    @Query("SELECT lrs.delinquencyBucket, COUNT(lrs), COALESCE(SUM(lrs.totalArrears), 0) " +
           "FROM LoanRepaymentStatus lrs " +
           "WHERE (:branchId IS NULL OR lrs.loan.branch.id = :branchId) " +
           "GROUP BY lrs.delinquencyBucket")
    List<Object[]> getDelinquencySummary(@Param("branchId") Long branchId);

    @Query("SELECT AVG(lrs.collectionRate) FROM LoanRepaymentStatus lrs " +
           "WHERE lrs.loan.branch.id = :branchId")
    BigDecimal getAverageCollectionRateByBranch(@Param("branchId") Long branchId);
}