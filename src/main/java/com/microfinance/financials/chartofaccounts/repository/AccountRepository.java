// repository/AccountRepository.java
package com.microfinance.financials.chartofaccounts.repository;

import com.microfinance.financials.chartofaccounts.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    Optional<Account> findByCode(String code);
    
    List<Account> findByIsActiveTrueOrderByCodeAsc();
    
    List<Account> findByAccountTypeOrderByCodeAsc(String accountType);
    
    List<Account> findByCategoryId(Long categoryId);
    
    List<Account> findByParentAccountId(Long parentAccountId);
    
    @Query("SELECT a FROM Account a WHERE a.isActive = true AND a.isLeaf = true ORDER BY a.code")
    List<Account> findLeafAccounts();
    
    @Query("SELECT COALESCE(SUM(a.currentBalance), 0) FROM Account a WHERE a.accountType = :accountType AND a.normalBalance = :normalBalance")
    BigDecimal getTotalBalanceByTypeAndNormalBalance(@Param("accountType") String accountType,
                                                      @Param("normalBalance") String normalBalance);

    Optional<Account> findByName(String name);
}