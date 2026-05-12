// repository/TaxTransactionRepository.java
package com.microfinance.financials.taxmanagement.repository;

import com.microfinance.financials.taxmanagement.entity.TaxTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaxTransactionRepository extends JpaRepository<TaxTransaction, Long> {
    
    List<TaxTransaction> findByReferenceIdAndReferenceType(Long referenceId, String referenceType);
    
    Page<TaxTransaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    List<TaxTransaction> findByTaxCodeAndTransactionDateBetween(String taxCode, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT tt FROM TaxTransaction tt WHERE tt.status = 'CALCULATED' AND tt.transactionDate <= :date")
    List<TaxTransaction> findUnwithheldTaxes(@Param("date") LocalDate date);
    
    @Query("SELECT tt FROM TaxTransaction tt WHERE tt.status = 'WITHHELD' AND tt.remittanceDate IS NULL")
    List<TaxTransaction> findUnremittedTaxes();
    
    @Query("SELECT SUM(tt.taxAmount) FROM TaxTransaction tt WHERE tt.taxCode = :taxCode AND tt.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumTaxAmountByCodeAndDateRange(@Param("taxCode") String taxCode,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
}