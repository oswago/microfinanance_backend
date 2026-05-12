package com.microfinance.loanapplications.repository;

import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanapplications.entity.LoanRestructure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRestructureRepository extends JpaRepository<LoanRestructure, Long> {

    List<LoanRestructure> findByLoanId(Long loanId);

    Page<LoanRestructure> findByLoanId(Long loanId, Pageable pageable);

    Optional<LoanRestructure> findByRestructureReference(String restructureReference);

    List<LoanRestructure> findByLoanIdAndStatus(Long loanId, GeneralConfig.RestructureStatus status);

    @Query("SELECT lr FROM LoanRestructure lr WHERE lr.status = :status")
    List<LoanRestructure> findByStatus(@Param("status") GeneralConfig.RestructureStatus status);

    @Query("SELECT lr FROM LoanRestructure lr WHERE lr.loan.id = :loanId ORDER BY lr.requestDate DESC")
    List<LoanRestructure> findLatestByLoanId(@Param("loanId") Long loanId);

    @Query("SELECT COUNT(lr) > 0 FROM LoanRestructure lr WHERE lr.loan.id = :loanId AND lr.status = 'PENDING'")
    boolean existsPendingRestructure(@Param("loanId") Long loanId);

    @Query("SELECT lr FROM LoanRestructure lr WHERE " +
           "(:status IS NULL OR lr.status = :status) AND " +
           "(:loanId IS NULL OR lr.loan.id = :loanId) " +
           "ORDER BY lr.requestDate DESC")
    Page<LoanRestructure> findRestructuresByFilters(
            @Param("status") GeneralConfig.RestructureStatus status,
            @Param("loanId") Long loanId,
            Pageable pageable);

    @Query("SELECT COUNT(lr) FROM LoanRestructure lr WHERE lr.loan.id = :loanId")
    long countByLoanId(@Param("loanId") Long loanId);
}