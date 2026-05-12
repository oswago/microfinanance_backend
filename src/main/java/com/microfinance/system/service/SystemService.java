package com.microfinance.system.service;

import com.microfinance.base.entity.User;
import com.microfinance.system.dto.BranchRequest;
import com.microfinance.system.entity.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SystemService {
    
    // System Settings
    SystemSettings getSystemSettings();
    SystemSettings updateSystemSettings(SystemSettings settings);
    
    // Branch Management
    List<Branch> getAllBranches();
    List<Branch> getActiveBranches();
    //Branch createBranch(Branch branch);
    //Branch updateBranch(Long id, Branch branch);
    Branch createBranch(BranchRequest branch);
    Branch updateBranch(Long id, BranchRequest branch);
    void deleteBranch(Long id);
    Optional<Branch> getBranchByCode(String code);
    List<Branch> getBranchesByType(Branch.BranchType type);
    
    // Currency Management
    List<CurrencySettings> getAllCurrencies();
    List<CurrencySettings> getActiveCurrencies();
    CurrencySettings createCurrency(CurrencySettings currency);
    CurrencySettings updateCurrency(String code, CurrencySettings currency);
    Optional<CurrencySettings> getDefaultCurrency();
    void setDefaultCurrency(String currencyCode);
    
    // Holiday Management
    List<HolidayCalendar> getAllHolidays();
    List<HolidayCalendar> getActiveHolidays();
    HolidayCalendar createHoliday(HolidayCalendar holiday);
    boolean isHoliday(LocalDate date);
    List<HolidayCalendar> getHolidaysBetween(LocalDate startDate, LocalDate endDate);
    
    // Number Sequences
    List<NumberSequence> getAllNumberSequences();
    List<NumberSequence> getActiveNumberSequences();
    String getNextNumber(String sequenceCode);
    NumberSequence updateNumberSequence(Long id, NumberSequence sequence);
    Optional<NumberSequence> getNumberSequenceByCode(String sequenceCode);
    HolidayCalendar updateHoliday(Long id, HolidayCalendar holiday);

    NumberSequence createNumberSequence(NumberSequence numberSequence);

    Branch getBranchForUser(User currentUser);
}