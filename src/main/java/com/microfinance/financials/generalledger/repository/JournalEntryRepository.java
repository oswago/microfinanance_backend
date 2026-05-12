// repository/JournalEntryRepository.java
package com.microfinance.financials.generalledger.repository;

import com.microfinance.financials.generalledger.entity.JournalEntry;
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
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    
    Optional<JournalEntry> findByJournalNumber(String journalNumber);
    
    Page<JournalEntry> findByStatus(String status, Pageable pageable);
    
    List<JournalEntry> findByEntryDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT je FROM JournalEntry je WHERE je.status = 'POSTED' AND je.entryDate BETWEEN :startDate AND :endDate")
    List<JournalEntry> findPostedEntriesByDateRange(@Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);
    
    @Query("SELECT je FROM JournalEntry je WHERE je.financialPeriod.id = :periodId")
    List<JournalEntry> findByFinancialPeriodId(@Param("periodId") Long periodId);
}