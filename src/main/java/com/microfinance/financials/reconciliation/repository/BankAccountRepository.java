// repository/BankAccountRepository.java
package com.microfinance.financials.reconciliation.repository;

import com.microfinance.financials.reconciliation.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    
    List<BankAccount> findByStatus(String status);
    
    List<BankAccount> findByCurrency(String currency);
    
    @Query("SELECT ba FROM BankAccount ba WHERE ba.status = 'ACTIVE' ORDER BY ba.accountName")
    List<BankAccount> findActiveAccounts();
    
    @Query("SELECT ba FROM BankAccount ba WHERE ba.chartOfAccount.id = :chartOfAccountId")
    Optional<BankAccount> findByChartOfAccountId(@Param("chartOfAccountId") Long chartOfAccountId);
}

