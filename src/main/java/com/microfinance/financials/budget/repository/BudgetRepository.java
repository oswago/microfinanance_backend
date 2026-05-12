// repository/BudgetRepository.java
package com.microfinance.financials.budget.repository;

import com.microfinance.financials.budget.entity.Budget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
    Optional<Budget> findByBudgetCode(String budgetCode);
    
    List<Budget> findByFiscalYearOrderByCategoryAsc(Integer fiscalYear);

    // Fixed: Added Pageable parameter
    Page<Budget> findByFiscalYearAndCategory(Integer fiscalYear, String category, Pageable pageable);

    // Non-paginated versions for reports (returns all matching records)
    List<Budget> findByFiscalYearAndCategory(Integer fiscalYear, String category);

    // Fixed: Added Pageable parameter
    Page<Budget> findByFiscalYear(Integer fiscalYear, Pageable pageable);
    
    List<Budget> findByFiscalYearAndIsActiveTrue(Integer fiscalYear);
    
    @Query("SELECT b FROM Budget b WHERE b.fiscalYear = :year AND b.category = :category AND b.isActive = true")
    List<Budget> findActiveBudgetsByYearAndCategory(@Param("year") Integer year, @Param("category") String category);
    
    @Query("SELECT DISTINCT b.fiscalYear FROM Budget b ORDER BY b.fiscalYear DESC")
    List<Integer> findDistinctFiscalYears();
    
    @Query("SELECT b FROM Budget b WHERE b.isActive = true AND b.fiscalYear >= :year")
    List<Budget> findActiveBudgetsFromYear(@Param("year") Integer year);
}

