// repository/GeneralLedgerRepository.java
package com.microfinance.financials.generalledger.repository;

import com.microfinance.financials.generalledger.entity.GeneralLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface GeneralLedgerRepository extends JpaRepository<GeneralLedger, Long> {
    
    List<GeneralLedger> findByJournalId(Long journalId);

    Page<GeneralLedger> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    @Query("SELECT gl FROM GeneralLedger gl WHERE gl.account.id = :accountId ORDER BY gl.transactionDate DESC")
    Page<GeneralLedger> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);
    
    @Query("SELECT gl.accountCode, gl.accountName, " +
           "SUM(CASE WHEN gl.debitCredit = 'DEBIT' THEN gl.amount ELSE 0 END) as totalDebit, " +
           "SUM(CASE WHEN gl.debitCredit = 'CREDIT' THEN gl.amount ELSE 0 END) as totalCredit " +
           "FROM GeneralLedger gl " +
           "WHERE gl.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY gl.accountCode, gl.accountName " +
           "ORDER BY gl.accountCode")
    List<Object[]> getTrialBalance(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);
    
    @Query("SELECT gl.accountCode, gl.accountName, gl.debitCredit, SUM(gl.amount) " +
           "FROM GeneralLedger gl " +
           "WHERE gl.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY gl.accountCode, gl.accountName, gl.debitCredit")
    List<Object[]> getLedgerSummary(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);



        // CORRECTED: Method with Pageable parameter
        Page<GeneralLedger> findByAccountIdAndTransactionDateBetween(
                Long accountId,
                LocalDate startDate,
                LocalDate endDate,
                Pageable pageable
        );

        // Alternative: Method without Pageable (returns List)
        List<GeneralLedger> findByAccountIdAndTransactionDateBetween(
                Long accountId,
                LocalDate startDate,
                LocalDate endDate
        );

        // Method to find by account ID ordered by date
        List<GeneralLedger> findByAccountIdOrderByTransactionDateDesc(Long accountId, Pageable pageable);

        // Method to get balance up to a specific date
        @Query("SELECT COALESCE(SUM(CASE WHEN gl.debitCredit = 'DEBIT' THEN gl.amount ELSE -gl.amount END), 0) " +
                "FROM GeneralLedger gl WHERE gl.account.id = :accountId AND gl.transactionDate <= :asOfDate")
        BigDecimal getBalanceUpToDate(@Param("accountId") Long accountId, @Param("asOfDate") LocalDate asOfDate);

        // Method to get all transactions for reconciliation
        @Query("SELECT gl FROM GeneralLedger gl WHERE gl.account.id = :accountId " +
                "AND gl.transactionDate BETWEEN :startDate AND :endDate " +
                "AND gl.id NOT IN (SELECT ri.journalEntryId FROM ReconciliationItem ri WHERE ri.journalEntryId IS NOT NULL)")
        List<GeneralLedger> findUnreconciledTransactions(
                @Param("accountId") Long accountId,
                @Param("startDate") LocalDate startDate,
                @Param("endDate") LocalDate endDate
        );


}