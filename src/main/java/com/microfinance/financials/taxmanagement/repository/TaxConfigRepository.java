// repository/TaxConfigRepository.java
package com.microfinance.financials.taxmanagement.repository;

import com.microfinance.financials.taxmanagement.entity.TaxConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaxConfigRepository extends JpaRepository<TaxConfig, Long> {
    
    Optional<TaxConfig> findByTaxCode(String taxCode);
    
    List<TaxConfig> findByIsActiveTrue();
    
    List<TaxConfig> findByTaxType(String taxType);
    
    @Query("SELECT tc FROM TaxConfig tc WHERE tc.isActive = true AND tc.effectiveFrom <= :date AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :date)")
    List<TaxConfig> findActiveTaxesAsOfDate(@Param("date") LocalDateTime date);
    
    @Query("SELECT tc FROM TaxConfig tc WHERE tc.taxCode = :taxCode AND tc.effectiveFrom <= :date AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :date)")
    Optional<TaxConfig> findActiveTaxByCodeAsOfDate(@Param("taxCode") String taxCode, @Param("date") LocalDateTime date);
}



