// repository/BudgetActualRepository.java
package com.microfinance.financials.budget.repository;

import com.microfinance.financials.budget.entity.BudgetActual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BudgetActualRepository extends JpaRepository<BudgetActual, Long> {
    
    List<BudgetActual> findByBudgetId(Long budgetId);
    
    @Query("SELECT ba FROM BudgetActual ba WHERE ba.budgetId IN :budgetIds AND ba.periodDate BETWEEN :startDate AND :endDate")
    List<BudgetActual> findByBudgetIdsAndDateRange(@Param("budgetIds") List<Long> budgetIds,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);
    
    @Query("SELECT SUM(ba.actualAmount) FROM BudgetActual ba WHERE ba.budgetId = :budgetId AND ba.periodDate BETWEEN :startDate AND :endDate")
    BigDecimal sumActualAmountByBudgetAndDateRange(@Param("budgetId") Long budgetId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);
}

