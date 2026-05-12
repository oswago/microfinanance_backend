// repository/LoanRecoveryRepository.java
package com.microfinance.financials.provisions.repository;

import com.microfinance.financials.provisions.entity.LoanRecovery;
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
public interface LoanRecoveryRepository extends JpaRepository<LoanRecovery, Long> {
    
    Optional<LoanRecovery> findByRecoveryNumber(String recoveryNumber);
    
    Page<LoanRecovery> findByRecoveryDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    List<LoanRecovery> findByLoanIdOrderByRecoveryDateDesc(Long loanId);
    
    List<LoanRecovery> findByWriteOffId(Long writeOffId);
    
    @Query("SELECT SUM(lr.recoveredAmount) FROM LoanRecovery lr WHERE lr.recoveryDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRecoveriesBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT lr.recoveryType, SUM(lr.recoveredAmount) FROM LoanRecovery lr WHERE lr.recoveryDate BETWEEN :startDate AND :endDate GROUP BY lr.recoveryType")
    List<Object[]> getRecoverySummaryByType(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}