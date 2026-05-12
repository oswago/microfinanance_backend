// repository/ProvisionCalculationRepository.java
package com.microfinance.financials.provisions.repository;

import com.microfinance.financials.provisions.entity.ProvisionCalculation;
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
public interface ProvisionCalculationRepository extends JpaRepository<ProvisionCalculation, Long> {
    
    Optional<ProvisionCalculation> findByCalculationNumber(String calculationNumber);
    
    List<ProvisionCalculation> findByCalculationDate(LocalDate calculationDate);
    
    Page<ProvisionCalculation> findByCalculationDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    List<ProvisionCalculation> findByLoanIdOrderByCalculationDateDesc(Long loanId);
    
    @Query("SELECT pc FROM ProvisionCalculation pc WHERE pc.calculationDate = (SELECT MAX(pc2.calculationDate) FROM ProvisionCalculation pc2 WHERE pc2.loanId = pc.loanId) AND pc.loanId = :loanId")
    Optional<ProvisionCalculation> findLatestProvisionByLoanId(@Param("loanId") Long loanId);


    @Query("SELECT pc.agingBucket, COUNT(pc), SUM(pc.totalOutstanding), pc.provisionRate, SUM(pc.provisionAmount), SUM(pc.existingProvision), SUM(pc.provisionAdjustment) " +
            "FROM ProvisionCalculation pc WHERE pc.calculationDate = :calculationDate GROUP BY pc.agingBucket, pc.provisionRate ORDER BY pc.agingBucket")
    List<Object[]> getProvisionSummaryByBucket(@Param("calculationDate") LocalDate calculationDate);
}



