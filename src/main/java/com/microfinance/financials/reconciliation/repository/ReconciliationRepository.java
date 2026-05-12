// repository/ReconciliationRepository.java
package com.microfinance.financials.reconciliation.repository;

import com.microfinance.financials.reconciliation.entity.Reconciliation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReconciliationRepository extends JpaRepository<Reconciliation, Long> {
    
    Optional<Reconciliation> findByReconciliationNumber(String reconciliationNumber);
    
    List<Reconciliation> findByBankAccountIdOrderByReconciliationDateDesc(Long bankAccountId);
    
    Page<Reconciliation> findByBankAccountId(Long bankAccountId, Pageable pageable);
    
    Page<Reconciliation> findByStatus(String status, Pageable pageable);
    
    @Query("SELECT r FROM Reconciliation r WHERE r.bankAccountId = :bankAccountId AND r.reconciliationDate BETWEEN :startDate AND :endDate")
    List<Reconciliation> findByBankAccountIdAndDateRange(@Param("bankAccountId") Long bankAccountId,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);
    
    Optional<Reconciliation> findFirstByBankAccountIdOrderByReconciliationDateDesc(Long bankAccountId);
    
    @Query("SELECT COUNT(r) > 0 FROM Reconciliation r WHERE r.bankAccountId = :bankAccountId AND r.status = 'PENDING'")
    boolean hasPendingReconciliation(@Param("bankAccountId") Long bankAccountId);
}



