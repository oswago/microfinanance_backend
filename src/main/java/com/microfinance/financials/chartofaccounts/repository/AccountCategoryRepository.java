// repository/AccountCategoryRepository.java
package com.microfinance.financials.chartofaccounts.repository;

import com.microfinance.financials.chartofaccounts.entity.AccountCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountCategoryRepository extends JpaRepository<AccountCategory, Long> {
    
    Optional<AccountCategory> findByCode(String code);
    
    List<AccountCategory> findByIsActiveTrueOrderBySortOrderAsc();
    
    List<AccountCategory> findByAccountTypeOrderBySortOrderAsc(String accountType);
    
    @Query("SELECT COUNT(a) FROM Account a WHERE a.category.id = :categoryId")
    Integer countAccountsByCategory(@Param("categoryId") Long categoryId);

    Optional<AccountCategory> findByName(String name);
}