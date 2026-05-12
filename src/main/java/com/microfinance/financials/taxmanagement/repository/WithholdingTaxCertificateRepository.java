// repository/WithholdingTaxCertificateRepository.java
package com.microfinance.financials.taxmanagement.repository;

import com.microfinance.financials.taxmanagement.entity.WithholdingTaxCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WithholdingTaxCertificateRepository extends JpaRepository<WithholdingTaxCertificate, Long> {
    
    Optional<WithholdingTaxCertificate> findByCertificateNumber(String certificateNumber);
    
    List<WithholdingTaxCertificate> findByBorrowerIdOrderByCertificateDateDesc(Long borrowerId);
    
    List<WithholdingTaxCertificate> findByCertificateDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT wtc FROM WithholdingTaxCertificate wtc WHERE wtc.borrowerId = :borrowerId AND wtc.certificateDate BETWEEN :startDate AND :endDate")
    List<WithholdingTaxCertificate> findByBorrowerAndDateRange(@Param("borrowerId") Long borrowerId,
                                                                @Param("startDate") LocalDate startDate,
                                                                @Param("endDate") LocalDate endDate);
}