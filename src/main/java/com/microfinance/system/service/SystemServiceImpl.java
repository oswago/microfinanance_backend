package com.microfinance.system.service;

import com.microfinance.base.entity.User;
import com.microfinance.system.dto.BranchRequest;
import com.microfinance.system.entity.*;
import com.microfinance.system.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.microfinance.base.utils.SecurityUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemServiceImpl implements SystemService {
    
    private final SystemSettingsRepository systemSettingsRepository;
    private final BranchRepository branchRepository;
    private final CurrencySettingsRepository currencySettingsRepository;
    private final HolidayCalendarRepository holidayCalendarRepository;
    private final NumberSequenceRepository numberSequenceRepository;
    private final SecurityUtils securityUtils; // Inject SecurityUtils
    // System Settings Methods
    @Override
    @Transactional(readOnly = true)
    public SystemSettings getSystemSettings() {
        return systemSettingsRepository.findFirst()
                .orElseGet(this::createDefaultSystemSettings);
    }
    
    @Override
    @Transactional
    public SystemSettings updateSystemSettings(SystemSettings settings) {
        SystemSettings existing = getSystemSettings();
        
        // Update fields
        existing.setDefaultInterestCalculationMethod(settings.getDefaultInterestCalculationMethod());
        existing.setDefaultInterestRate(settings.getDefaultInterestRate());
        existing.setDefaultPenaltyRate(settings.getDefaultPenaltyRate());
        existing.setDefaultPenaltyGracePeriodDays(settings.getDefaultPenaltyGracePeriodDays());
        existing.setCompanyName(settings.getCompanyName());
        existing.setCompanyAddress(settings.getCompanyAddress());
        existing.setCompanyPhone(settings.getCompanyPhone());
        existing.setCompanyEmail(settings.getCompanyEmail());
        existing.setDefaultCurrency(settings.getDefaultCurrency());
        existing.setSessionTimeoutMinutes(settings.getSessionTimeoutMinutes());
        existing.setPasswordExpiryDays(settings.getPasswordExpiryDays());
        existing.setMfaEnabled(settings.isMfaEnabled());
        existing.setAutoBackupEnabled(settings.isAutoBackupEnabled());
        existing.setBackupSchedule(settings.getBackupSchedule());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(securityUtils.getCurrentUserId());

        return systemSettingsRepository.save(existing);
    }
    
    private SystemSettings createDefaultSystemSettings() {
        SystemSettings defaultSettings = new SystemSettings();
        defaultSettings.setDefaultInterestCalculationMethod(SystemSettings.InterestCalculationMethod.REDUCING_BALANCE);
        defaultSettings.setDefaultInterestRate(BigDecimal.valueOf(12.5));
        defaultSettings.setDefaultPenaltyRate(BigDecimal.valueOf(2.0));
        defaultSettings.setDefaultPenaltyGracePeriodDays(7);
        defaultSettings.setCompanyName("Microfinance System");
        defaultSettings.setCompanyAddress("123 Main Street, City");
        defaultSettings.setCompanyPhone("+1-555-0100");
        defaultSettings.setCompanyEmail("info@microfinance.com");
        defaultSettings.setDefaultCurrency("USD");
        defaultSettings.setSessionTimeoutMinutes(30);
        defaultSettings.setPasswordExpiryDays(90);
        defaultSettings.setMfaEnabled(true);
        defaultSettings.setAutoBackupEnabled(true);
        defaultSettings.setBackupSchedule("0 0 2 * * ?"); // Daily at 2 AM


        return systemSettingsRepository.save(defaultSettings);
    }
    
    // Branch Management Methods
    @Override
    @Transactional(readOnly = true)
    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Branch> getActiveBranches() {
        return branchRepository.findByActiveTrue();
    }


    @Override
    @Transactional
    public Branch createBranch(BranchRequest request) {
        // Validate branch code uniqueness
        if (branchRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Branch code already exists: " + request.getCode());
        }

        Branch branch = new Branch();
        branch.setCode(request.getCode());
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setPhone(request.getPhone());
        branch.setEmail(request.getEmail());
        branch.setType(request.getType());
        branch.setActive(request.isActive());
        branch.setCreatedAt(LocalDateTime.now());
        branch.setCreatedBy(securityUtils.getCurrentUserId());

        // Set parent branch if ID is provided
        if (request.getParentBranchId() != null) {
            Branch parentBranch = branchRepository.findById(request.getParentBranchId())
                    .orElseThrow(() -> new RuntimeException("Parent branch not found with id: " + request.getParentBranchId()));
            branch.setParentBranch(parentBranch);
        }

        return branchRepository.save(branch);
    }

    @Override
    @Transactional
    public Branch updateBranch(Long id, BranchRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + id));

        // Validate branch code uniqueness (excluding current branch)
        if (branchRepository.existsByCodeAndIdNot(request.getCode(), id)) {
            throw new RuntimeException("Branch code already exists: " + request.getCode());
        }

        branch.setCode(request.getCode());
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setPhone(request.getPhone());
        branch.setEmail(request.getEmail());
        branch.setType(request.getType());
        branch.setActive(request.isActive());
        branch.setUpdatedAt(LocalDateTime.now());
        branch.setUpdatedBy(securityUtils.getCurrentUserId());

        // Set parent branch if ID is provided
        if (request.getParentBranchId() != null) {
            Branch parentBranch = branchRepository.findById(request.getParentBranchId())
                    .orElseThrow(() -> new RuntimeException("Parent branch not found with id: " + request.getParentBranchId()));
            branch.setParentBranch(parentBranch);
        } else {
            branch.setParentBranch(null);
        }

        return branchRepository.save(branch);
    }

    @Override
    @Transactional
    public void deleteBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + id));
        
        // Soft delete by setting active to false
        branch.setActive(false);
        branch.setUpdatedAt(LocalDateTime.now());
        branch.setUpdatedBy(securityUtils.getCurrentUserId());
        branchRepository.save(branch);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Branch> getBranchByCode(String code) {
        return branchRepository.findByCode(code);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Branch> getBranchesByType(Branch.BranchType type) {
        return branchRepository.findByTypeAndActiveTrue(type);
    }

    public Branch getBranchForUser(User user) {
        if (user == null || user.getBranchId() == null) {
            return getDefaultBranch();
        }

        return branchRepository.findById(user.getBranchId())
                .orElseGet(this::getDefaultBranch);
    }

    public Branch getDefaultBranch() {
        return branchRepository.findById(1L)
                .orElseGet(() -> branchRepository.findAll().stream()
                        .findFirst()
                        .orElseThrow(() -> new EntityNotFoundException("No branches found")));
    }
    
    // Currency Management Methods
    @Override
    @Transactional(readOnly = true)
    public List<CurrencySettings> getAllCurrencies() {
        return currencySettingsRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CurrencySettings> getActiveCurrencies() {
        return currencySettingsRepository.findByActiveTrue();
    }
    
    @Override
    @Transactional
    public CurrencySettings createCurrency(CurrencySettings currency) {
        currency.setCreatedAt(LocalDateTime.now());
        currency.setCreatedBy(securityUtils.getCurrentUserId());
        return currencySettingsRepository.save(currency);
    }
    
    @Override
    @Transactional
    public CurrencySettings updateCurrency(String code, CurrencySettings currency) {
        CurrencySettings existing = currencySettingsRepository.findById(code)
                .orElseThrow(() -> new RuntimeException("Currency not found with code: " + code));
        
        existing.setCurrencyName(currency.getCurrencyName());
        existing.setSymbol(currency.getSymbol());
        existing.setExchangeRate(currency.getExchangeRate());
        existing.setActive(currency.isActive());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(securityUtils.getCurrentUserId());

        // If setting as default, clear other default flags
        if (currency.isDefaultValue() && !existing.isDefaultValue()) {
            currencySettingsRepository.clearAllDefaultFlags();
            existing.setDefaultValue(true);
        }
        
        return currencySettingsRepository.save(existing);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<CurrencySettings> getDefaultCurrency() {
        return currencySettingsRepository.findByDefaultValueTrueAndActiveTrue();
    }
    
    @Override
    @Transactional
    public void setDefaultCurrency(String currencyCode) {
        CurrencySettings currency = currencySettingsRepository.findById(currencyCode)
                .orElseThrow(() -> new RuntimeException("Currency not found with code: " + currencyCode));
        
        if (!currency.isActive()) {
            throw new RuntimeException("Cannot set inactive currency as default");
        }
        
        currencySettingsRepository.clearAllDefaultFlags();
        currency.setDefaultValue(true);
        currency.setUpdatedAt(LocalDateTime.now());
        currency.setUpdatedBy(securityUtils.getCurrentUserId());
        currencySettingsRepository.save(currency);
    }
    
    // Holiday Management Methods
    @Override
    @Transactional(readOnly = true)
    public List<HolidayCalendar> getAllHolidays() {
        return holidayCalendarRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<HolidayCalendar> getActiveHolidays() {
        return holidayCalendarRepository.findByActiveTrue();
    }
    
    @Override
    @Transactional
    public HolidayCalendar createHoliday(HolidayCalendar holiday) {
        holiday.setCreatedAt(LocalDateTime.now());
        holiday.setCreatedBy(securityUtils.getCurrentUserId());
        return holidayCalendarRepository.save(holiday);
    }

    @Override
    @Transactional
    public HolidayCalendar updateHoliday(Long id, HolidayCalendar holidayCalendar) {
        HolidayCalendar existing = holidayCalendarRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Holiday not found with Id: " + id));

        existing.setName(holidayCalendar.getName());
        existing.setHolidayDate(holidayCalendar.getHolidayDate());
        existing.setDescription(holidayCalendar.getDescription());
        existing.setCountryCode(holidayCalendar.getCountryCode());
        existing.setRecurring(holidayCalendar.isRecurring());
        existing.setActive(holidayCalendar.isActive());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(securityUtils.getCurrentUserId());

        return holidayCalendarRepository.save(existing);
    }



    @Override
    @Transactional(readOnly = true)
    public boolean isHoliday(LocalDate date) {
        return holidayCalendarRepository.existsByHolidayDateAndActiveTrue(date);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<HolidayCalendar> getHolidaysBetween(LocalDate startDate, LocalDate endDate) {
        return holidayCalendarRepository.findByHolidayDateBetweenAndActiveTrue(startDate, endDate);
    }
    
    // Number Sequence Methods
    @Override
    @Transactional(readOnly = true)
    public List<NumberSequence> getAllNumberSequences() {
        return numberSequenceRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<NumberSequence> getActiveNumberSequences() {
        return numberSequenceRepository.findByActiveTrue();
    }
    
    @Override
    @Transactional
    public String getNextNumber(String sequenceCode) {
        NumberSequence sequence = numberSequenceRepository.findBySequenceCode(sequenceCode)
                .orElseThrow(() -> new RuntimeException("Number sequence not found: " + sequenceCode));
        
        if (!sequence.isActive()) {
            throw new RuntimeException("Number sequence is inactive: " + sequenceCode);
        }
        
        String nextNumber = sequence.getPrefix() + 
                String.format("%0" + sequence.getPadding() + "d", sequence.getNextValue()) + 
                sequence.getSuffix();
        
        // Increment the sequence
        numberSequenceRepository.incrementNextValue(sequenceCode);
        
        log.info("Generated next number for sequence {}: {}", sequenceCode, nextNumber);
        return nextNumber;
    }


    @Override
    public NumberSequence createNumberSequence(NumberSequence numberSequence) {
        numberSequence.setCreatedAt(LocalDateTime.now());
        numberSequence.setCreatedBy(securityUtils.getCurrentUserId());
        return numberSequenceRepository.save(numberSequence);
    }
    
    @Override
    @Transactional
    public NumberSequence updateNumberSequence(Long id, NumberSequence sequence) {
        NumberSequence existing = numberSequenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Number sequence not found with id: " + id));
        
        // Validate sequence code uniqueness excluding current sequence
        if (numberSequenceRepository.existsBySequenceCodeAndIdNot(sequence.getSequenceCode(), id)) {
            throw new RuntimeException("Sequence code already exists: " + sequence.getSequenceCode());
        }
        
        existing.setSequenceCode(sequence.getSequenceCode());
        existing.setDescription(sequence.getDescription());
        existing.setPrefix(sequence.getPrefix());
        existing.setSuffix(sequence.getSuffix());
        existing.setPadding(sequence.getPadding());
        existing.setResetDaily(sequence.isResetDaily());
        existing.setResetMonthly(sequence.isResetMonthly());
        existing.setResetYearly(sequence.isResetYearly());
        existing.setActive(sequence.isActive());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(securityUtils.getCurrentUserId());
        
        return numberSequenceRepository.save(existing);
    }


    
    @Override
    @Transactional(readOnly = true)
    public Optional<NumberSequence> getNumberSequenceByCode(String sequenceCode) {
        return numberSequenceRepository.findBySequenceCode(sequenceCode);
    }
}