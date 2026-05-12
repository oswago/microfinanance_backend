package com.microfinance.loanapplications.repository;

import com.microfinance.loanapplications.entity.LoanAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoanAuditRepository extends JpaRepository<LoanAudit, Long> {

    List<LoanAudit> findByLoanIdOrderByPerformedAtDesc(Long loanId);

    Page<LoanAudit> findByLoanIdOrderByPerformedAtDesc(Long loanId, Pageable pageable);

    @Query("SELECT la FROM LoanAudit la WHERE la.loan.id = :loanId AND la.action = :action ORDER BY la.performedAt DESC")
    List<LoanAudit> findByLoanIdAndAction(@Param("loanId") Long loanId, @Param("action") String action);

    @Query("SELECT la FROM LoanAudit la WHERE la.performedBy.id = :userId ORDER BY la.performedAt DESC")
    List<LoanAudit> findByPerformedBy(@Param("userId") Long userId);

    @Query("SELECT la FROM LoanAudit la WHERE la.performedAt BETWEEN :startDate AND :endDate ORDER BY la.performedAt DESC")
    List<LoanAudit> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT la FROM LoanAudit la WHERE la.loan.id = :loanId AND la.fieldName = :fieldName ORDER BY la.performedAt DESC")
    List<LoanAudit> findByLoanIdAndFieldName(
            @Param("loanId") Long loanId,
            @Param("fieldName") String fieldName);

    @Query("SELECT COUNT(la) FROM LoanAudit la WHERE la.loan.id = :loanId")
    long countByLoanId(@Param("loanId") Long loanId);

    @Query("SELECT la FROM LoanAudit la WHERE " +
           "(:loanId IS NULL OR la.loan.id = :loanId) AND " +
           "(:action IS NULL OR la.action = :action) AND " +
           "(:userId IS NULL OR la.performedBy.id = :userId) AND " +
           "(:startDate IS NULL OR la.performedAt >= :startDate) AND " +
           "(:endDate IS NULL OR la.performedAt <= :endDate) " +
           "ORDER BY la.performedAt DESC")
    Page<LoanAudit> findAuditsByFilters(
            @Param("loanId") Long loanId,
            @Param("action") String action,
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}