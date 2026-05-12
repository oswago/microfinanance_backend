// repository/BankStatementRepository.java
package com.microfinance.financials.reconciliation.repository;

import com.microfinance.financials.reconciliation.entity.BankStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankStatementRepository extends JpaRepository<BankStatement, Long> {
    
    List<BankStatement> findByBankAccountIdOrderByStatementDateDesc(Long bankAccountId);
    
    Optional<BankStatement> findByBankAccountIdAndStatementDate(Long bankAccountId, LocalDate statementDate);
    
    @Query("SELECT bs FROM BankStatement bs WHERE bs.bankAccountId = :bankAccountId AND bs.statementDate BETWEEN :startDate AND :endDate")
    List<BankStatement> findByBankAccountIdAndDateRange(@Param("bankAccountId") Long bankAccountId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);
}