// service/ChartOfAccountsService.java
package com.microfinance.financials.chartofaccounts.service;


import com.microfinance.audit.service.AuditService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.exception.BusinessException;
import com.microfinance.financials.chartofaccounts.dto.AccountCategoryDto;
import com.microfinance.financials.chartofaccounts.dto.AccountDto;
import com.microfinance.financials.chartofaccounts.entity.Account;
import com.microfinance.financials.chartofaccounts.entity.AccountCategory;
import com.microfinance.financials.chartofaccounts.enums.AccountType;
import com.microfinance.financials.chartofaccounts.enums.NormalBalance;
import com.microfinance.financials.chartofaccounts.repository.AccountCategoryRepository;
import com.microfinance.financials.chartofaccounts.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChartOfAccountsService {
    
    private final AccountCategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;
    private final SecurityUtils securityUtils;
    
    // Account Category Methods
    @Transactional(readOnly = true)
    public List<AccountCategoryDto> getAllCategories() {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::convertToCategoryDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public AccountCategoryDto getCategoryById(Long id) {
        AccountCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return convertToCategoryDto(category);
    }

    @Transactional
    public AccountCategoryDto createCategory(AccountCategoryDto dto) {
        log.info("Creating new category with code: {}, name: {}", dto.getCode(), dto.getName());

        // Check for duplicate category code
        if (categoryRepository.findByCode(dto.getCode()).isPresent()) {
            log.warn("Category code already exists: {}", dto.getCode());
            throw new BusinessException("Category code '" + dto.getCode() + "' already exists. Please use a unique code.");
        }

        // Check for duplicate category name
        Optional<AccountCategory> existingCategory = categoryRepository.findByName(dto.getName());
        if (existingCategory.isPresent()) {
            log.warn("Category name already exists: {}", dto.getName());
            throw new BusinessException("Category name '" + dto.getName() + "' already exists. Please use a unique name.");
        }

        // Validate account type
        AccountType accountType;
        try {
            accountType = AccountType.valueOf(dto.getAccountType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid account type: " + dto.getAccountType());
        }

        // Validate normal balance
        NormalBalance normalBalance;
        try {
            normalBalance = NormalBalance.valueOf(dto.getNormalBalance());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid normal balance: " + dto.getNormalBalance());
        }

        AccountCategory category = AccountCategory.builder()
                .code(dto.getCode().toUpperCase().trim())
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .accountType(accountType)
                .normalBalance(normalBalance)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .isActive(true)
                .build();

        category = categoryRepository.save(category);

        if (category != null && category.getId() != null) {
            log.info("Category created successfully with ID: {}", category.getId());
            auditService.logChartOfAccountAction(
                    category.getId(),
                    "ACCOUNT_CATEGORY_CREATION",
                    securityUtils.getCurrentUserId(),
                    "Account Category Created"
            );
        }

        return convertToCategoryDto(category);
    }

    @Transactional
    public AccountCategoryDto updateCategory(Long id, AccountCategoryDto dto) {
        log.info("Updating category with ID: {}", id);

        AccountCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Category not found with ID: " + id));

        // Check for duplicate category code (excluding current category)
        Optional<AccountCategory> existingCode = categoryRepository.findByCode(dto.getCode());
        if (existingCode.isPresent() && !existingCode.get().getId().equals(id)) {
            log.warn("Category code already exists: {}", dto.getCode());
            throw new BusinessException("Category code '" + dto.getCode() + "' already exists. Please use a unique code.");
        }

        // Check for duplicate category name (excluding current category)
        Optional<AccountCategory> existingName = categoryRepository.findByName(dto.getName());
        if (existingName.isPresent() && !existingName.get().getId().equals(id)) {
            log.warn("Category name already exists: {}", dto.getName());
            throw new BusinessException("Category name '" + dto.getName() + "' already exists. Please use a unique name.");
        }

        category.setCode(dto.getCode().toUpperCase().trim());
        category.setName(dto.getName().trim());
        category.setDescription(dto.getDescription());
        category.setSortOrder(dto.getSortOrder());
        category.setIsActive(category.getIsActive());

        category = categoryRepository.save(category);

        if (category != null && category.getId() != null) {
            log.info("Category updated successfully with ID: {}", category.getId());
            auditService.logChartOfAccountAction(
                    category.getId(),
                    "ACCOUNT_CATEGORY_UPDATE",
                    securityUtils.getCurrentUserId(),
                    "Account Category Updated"
            );
        }

        return convertToCategoryDto(category);
    }

    
    @Transactional
    public void deleteCategory(Long id) {
        Integer accountCount = categoryRepository.countAccountsByCategory(id);
        if (accountCount > 0) {
            throw new RuntimeException("Cannot delete category with existing accounts");
        }
       categoryRepository.deleteById(id);

            log.info("Category Deleted successfully with ID: {}", id);
            auditService.logChartOfAccountAction(
                    id,
                    "ACCOUNT_CATEGORY_DELETION",
                    securityUtils.getCurrentUserId(),
                    "Account Category Deleted"
            );


    }
    
    // Account Methods
    @Transactional(readOnly = true)
    public List<AccountDto> getAllAccounts() {
        return accountRepository.findByIsActiveTrueOrderByCodeAsc()
                .stream()
                .map(this::convertToAccountDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public AccountDto getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return convertToAccountDto(account);
    }
    
    @Transactional(readOnly = true)
    public List<AccountDto> getAccountsByCategory(Long categoryId) {
        return accountRepository.findByCategoryId(categoryId)
                .stream()
                .map(this::convertToAccountDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public AccountDto createAccount(AccountDto dto) {

        log.info("Creating new account with code: {}, name: {}", dto.getCode(), dto.getName());

        // Check for duplicate account code
        if (accountRepository.findByCode(dto.getCode()).isPresent()) {
            log.warn("Account code already exists: {}", dto.getCode());
            throw new BusinessException("Account code '" + dto.getCode() + "' already exists. Please use a unique code.");
        }

        // Check for duplicate account name
        Optional<Account> existingAccount = accountRepository.findByName(dto.getName());
        if (existingAccount.isPresent()) {
            log.warn("Account name already exists: {}", dto.getName());
            throw new BusinessException("Account name '" + dto.getName() + "' already exists. Please use a unique name.");
        }

        // Validate category exists
        AccountCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new BusinessException("Category not found with ID: " + dto.getCategoryId()));

        
        Account account = Account.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .description(dto.getDescription())
                .category(category)
                .accountType(AccountType.valueOf(dto.getAccountType()))
                .normalBalance(NormalBalance.valueOf(dto.getNormalBalance()))
                .parentAccountId(dto.getParentAccountId())
                .openingBalance(dto.getOpeningBalance() != null ? dto.getOpeningBalance() : BigDecimal.ZERO)
                .currentBalance(dto.getOpeningBalance() != null ? dto.getOpeningBalance() : BigDecimal.ZERO)
                .isActive(true)
                .isLeaf(true)
                .bankAccountDetails(dto.getBankAccountDetails())
                .build();
        
        account = accountRepository.save(account);

        if (account != null && account.getId() != null) {
            log.info("Chart of Account created successfully with ID: {}", account.getId());
            auditService.logChartOfAccountAction(
                    account.getId(),
                    "ACCOUNT_CREATION",
                    securityUtils.getCurrentUserId(),
                    "Chart Of Account Created"
            );
        }

        return convertToAccountDto(account);
    }
    
    @Transactional
    public AccountDto updateAccount(Long id, AccountDto dto) {

        log.info("Updating account with ID: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Account not found with ID: " + id));

        // Check for duplicate account code (excluding current account)
        Optional<Account> existingCode = accountRepository.findByCode(dto.getCode());
        if (existingCode.isPresent() && !existingCode.get().getId().equals(id)) {
            log.warn("Account code already exists: {}", dto.getCode());
            throw new BusinessException("Account code '" + dto.getCode() + "' already exists. Please use a unique code.");
        }

        // Check for duplicate account name (excluding current account)
        Optional<Account> existingName = accountRepository.findByName(dto.getName());
        if (existingName.isPresent() && !existingName.get().getId().equals(id)) {
            log.warn("Account name already exists: {}", dto.getName());
            throw new BusinessException("Account name '" + dto.getName() + "' already exists. Please use a unique name.");
        }

        
        account.setName(dto.getName());
        account.setDescription(dto.getDescription());
        account.setIsActive(account.getIsActive());
        account.setBankAccountDetails(dto.getBankAccountDetails());
        account.setOpeningBalance(dto.getOpeningBalance());
        
        account = accountRepository.save(account);

        if (account != null && account.getId() != null) {
            log.info(">>>>>> Opening Balance of : {}", account.getOpeningBalance());
            log.info("Account created successfully with ID: {}", account.getId());

            auditService.logChartOfAccountAction(
                    account.getId(),
                    "ACCOUNT_UPDATE",
                    securityUtils.getCurrentUserId(),
                    "Chart Of Account Updated"
            );
        }

        return convertToAccountDto(account);
    }
    
    @Transactional
    public void deleteAccount(Long id) {
        // Check if account has transactions
        // TODO: Add check for existing journal entries
            log.info("Chart of Account Deleted  with ID: {}", id);
            auditService.logChartOfAccountAction(
                    id,
                    "ACCOUNT_DELETED",
                    securityUtils.getCurrentUserId(),
                    "Chart Of Account Deleted"
            );

        accountRepository.deleteById(id);
    }
    
    // Conversion Methods
    private AccountCategoryDto convertToCategoryDto(AccountCategory category) {
        Integer accountCount = categoryRepository.countAccountsByCategory(category.getId());
        
        return AccountCategoryDto.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .accountType(category.getAccountType().name())
                .accountTypeDisplay(category.getAccountType().getDisplayName())
                .normalBalance(category.getNormalBalance().name())
                .normalBalanceDisplay(category.getNormalBalance().getDisplayName())
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .accountCount(accountCount)
                .build();
    }
    
    private AccountDto convertToAccountDto(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .code(account.getCode())
                .name(account.getName())
                .description(account.getDescription())
                .categoryId(account.getCategory() != null ? account.getCategory().getId() : null)
                .categoryName(account.getCategory() != null ? account.getCategory().getName() : null)
                .accountType(account.getAccountType().name())
                .accountTypeDisplay(account.getAccountType().getDisplayName())
                .normalBalance(account.getNormalBalance().name())
                .normalBalanceDisplay(account.getNormalBalance().getDisplayName())
                .parentAccountId(account.getParentAccountId())
                .currentBalance(account.getCurrentBalance())
                .openingBalance(account.getOpeningBalance())
                .isActive(account.getIsActive())
                .isLeaf(account.getIsLeaf())
                .bankAccountDetails(account.getBankAccountDetails())
                .build();
    }
}