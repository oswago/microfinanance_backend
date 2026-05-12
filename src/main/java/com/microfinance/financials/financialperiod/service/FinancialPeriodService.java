// service/FinancialPeriodService.java
package com.microfinance.financials.financialperiod.service;

import com.microfinance.base.entity.User;
import com.microfinance.financials.financialperiod.dto.ClosePeriodDto;
import com.microfinance.financials.generalledger.dto.FinancialPeriodDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface FinancialPeriodService {
    
    FinancialPeriodDto createFinancialPeriod(FinancialPeriodDto dto, User currentUser);
    
    FinancialPeriodDto updateFinancialPeriod(Long id, FinancialPeriodDto dto, User currentUser);
    
    FinancialPeriodDto closeFinancialPeriod(Long id, ClosePeriodDto request, User currentUser);
    
    FinancialPeriodDto lockFinancialPeriod(Long id, String reason, User currentUser);
    
    FinancialPeriodDto unlockFinancialPeriod(Long id, String reason, User currentUser);
    
    FinancialPeriodDto reopenFinancialPeriod(Long id, String reason, User currentUser);
    
    Page<FinancialPeriodDto> getFinancialPeriods(Integer year, String status, Pageable pageable, User currentUser);
    
    FinancialPeriodDto getFinancialPeriodById(Long id, User currentUser);
    
    FinancialPeriodDto getCurrentFinancialPeriod(User currentUser);
    
    FinancialPeriodDto getNextFinancialPeriod(Long id, User currentUser);
    
    FinancialPeriodDto getPreviousFinancialPeriod(Long id, User currentUser);
    
    List<FinancialPeriodDto> getPeriodSummary(Integer year, User currentUser);
    
    void autoCreatePeriods(Integer year, User currentUser);
}