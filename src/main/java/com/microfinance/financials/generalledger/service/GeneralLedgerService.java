// service/GeneralLedgerService.java
package com.microfinance.financials.generalledger.service;

import com.microfinance.base.entity.User;
import com.microfinance.financials.generalledger.dto.FinancialPeriodDto;
import com.microfinance.financials.generalledger.dto.GeneralLedgerDto;
import com.microfinance.financials.generalledger.dto.JournalEntryDto;
import com.microfinance.financials.generalledger.dto.TrialBalanceDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface GeneralLedgerService {
    
    // Journal Entry methods
    JournalEntryDto createJournalEntry(JournalEntryDto dto, User currentUser);
    JournalEntryDto postJournalEntry(Long id, User currentUser);
    JournalEntryDto reverseJournalEntry(Long id, String reason, User currentUser);
    Page<JournalEntryDto> getJournalEntries(String status, LocalDate startDate, LocalDate endDate, Pageable pageable, User currentUser);
    JournalEntryDto getJournalEntryById(Long id, User currentUser);
    
    // General Ledger methods
    Page<GeneralLedgerDto> getLedgerEntries(Long accountId, LocalDate startDate, LocalDate endDate, Pageable pageable, User currentUser);
    List<TrialBalanceDto> getTrialBalance(LocalDate asOfDate, User currentUser);
    
    // Financial Period methods
    FinancialPeriodDto createFinancialPeriod(FinancialPeriodDto dto, User currentUser);
    FinancialPeriodDto closeFinancialPeriod(Long id, User currentUser);
    List<FinancialPeriodDto> getFinancialPeriods(Integer year, User currentUser);
    FinancialPeriodDto getCurrentFinancialPeriod(User currentUser);
}