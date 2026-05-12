// service/BankReconciliationService.java (Complete with all methods)
package com.microfinance.financials.reconciliation.service;

import com.microfinance.base.entity.User;
import com.microfinance.financials.reconciliation.dto.*;
import com.microfinance.financials.reconciliation.entity.*;
import com.microfinance.financials.reconciliation.repository.*;
import com.microfinance.financials.chartofaccounts.entity.Account;
import com.microfinance.financials.chartofaccounts.repository.AccountRepository;
import com.microfinance.financials.generalledger.entity.GeneralLedger;
import com.microfinance.financials.generalledger.repository.GeneralLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankReconciliationService {

    private final BankAccountRepository bankAccountRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final ReconciliationItemRepository reconciliationItemRepository;
    private final BankStatementRepository bankStatementRepository;
    private final AccountRepository accountRepository;
    private final GeneralLedgerRepository generalLedgerRepository;

    // Bank Account Management Methods
    @Transactional
    public BankAccountDTO createBankAccount(BankAccountDTO dto, User currentUser) {
        log.info("User {} creating bank account: {}", currentUser.getUsername(), dto.getAccountName());

        Account chartOfAccount = accountRepository.findById(dto.getChartOfAccountId())
                .orElseThrow(() -> new RuntimeException("Chart of account not found"));

        BankAccount bankAccount = BankAccount.builder()
                .accountName(dto.getAccountName())
                .accountNumber(dto.getAccountNumber())
                .bankName(dto.getBankName())
                .branchCode(dto.getBranchCode())
                .swiftCode(dto.getSwiftCode())
                .chartOfAccount(chartOfAccount)
                .currentBalance(dto.getCurrentBalance() != null ? dto.getCurrentBalance() : BigDecimal.ZERO)
                .availableBalance(dto.getAvailableBalance() != null ? dto.getAvailableBalance() : BigDecimal.ZERO)
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "KES")
                .status("ACTIVE")
                .openingBalance(dto.getOpeningBalance() != null ? dto.getOpeningBalance() : BigDecimal.ZERO)
                .notes(dto.getNotes())
                .createdBy(currentUser.getId())
                .build();

        bankAccount = bankAccountRepository.save(bankAccount);
        return convertToDTO(bankAccount);
    }

    @Transactional
    public BankAccountDTO updateBankAccount(Long id, BankAccountDTO dto, User currentUser) {
        log.info("User {} updating bank account: {}", currentUser.getUsername(), id);

        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank account not found"));

        bankAccount.setAccountName(dto.getAccountName());
        bankAccount.setAccountNumber(dto.getAccountNumber());
        bankAccount.setBankName(dto.getBankName());
        bankAccount.setBranchCode(dto.getBranchCode());
        bankAccount.setSwiftCode(dto.getSwiftCode());
        bankAccount.setNotes(dto.getNotes());

        if (dto.getStatus() != null) {
            bankAccount.setStatus(dto.getStatus());
        }

        bankAccount = bankAccountRepository.save(bankAccount);
        return convertToDTO(bankAccount);
    }

    @Transactional(readOnly = true)
    public List<BankAccountDTO> getBankAccounts() {
        return bankAccountRepository.findActiveAccounts().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BankAccountDTO getBankAccountById(Long id) {
        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank account not found"));
        return convertToDTO(bankAccount);
    }

    // Reconciliation Process Methods
    @Transactional
    public ReconciliationDTO startReconciliation(ReconcileRequestDTO request, User currentUser) {
        log.info("User {} starting reconciliation for bank account: {}", 
                currentUser.getUsername(), request.getBankAccountId());

        BankAccount bankAccount = bankAccountRepository.findById(request.getBankAccountId())
                .orElseThrow(() -> new RuntimeException("Bank account not found"));

        // Check if there's a pending reconciliation
        if (reconciliationRepository.hasPendingReconciliation(request.getBankAccountId())) {
            throw new RuntimeException("There is already a pending reconciliation for this account");
        }

        // Get system balance as of reconciliation date
        BigDecimal systemBalance = getSystemBalanceAsOfDate(
            request.getBankAccountId(), 
            request.getReconciliationDate()
        );

        BigDecimal difference = systemBalance.subtract(request.getStatementBalance());

        // Generate reconciliation number
        String reconciliationNumber = generateReconciliationNumber();

        Reconciliation reconciliation = Reconciliation.builder()
                .reconciliationNumber(reconciliationNumber)
                .reconciliationDate(request.getReconciliationDate())
                .bankAccountId(request.getBankAccountId())
                .bankAccountName(bankAccount.getAccountName())
                .bankAccountNumber(bankAccount.getAccountNumber())
                .systemBalance(systemBalance)
                .statementBalance(request.getStatementBalance())
                .difference(difference)
                .status("PENDING")
                .notes(request.getNotes())
                .createdBy(currentUser.getId())
                .build();

        reconciliation = reconciliationRepository.save(reconciliation);

        // Generate reconciliation items
        generateReconciliationItems(reconciliation, bankAccount, request.getReconciliationDate());

        return getReconciliationById(reconciliation.getId());
    }

    private void generateReconciliationItems(Reconciliation reconciliation, 
                                              BankAccount bankAccount, 
                                              LocalDate asOfDate) {
        // Get unmatched transactions from system
        List<GeneralLedger> unmatchedSystemTransactions = getUnmatchedSystemTransactions(
            bankAccount.getChartOfAccount().getId(), 
            asOfDate
        );

        // Create system-only items
        for (GeneralLedger transaction : unmatchedSystemTransactions) {
            ReconciliationItem item = ReconciliationItem.builder()
                    .reconciliationId(reconciliation.getId())
                    .transactionDate(transaction.getTransactionDate())
                    .description(transaction.getDescription())
                    .referenceNumber(transaction.getReferenceNumber())
                    .itemType(determineItemType(transaction))
                    .category("SYSTEM_ONLY")
                    .amount(transaction.getAmount())
                    .status("PENDING")
                    .isMatched(false)
                    .journalEntryId(transaction.getJournalId())
                    .build();
            reconciliationItemRepository.save(item);
        }
    }

    @Transactional
    public ReconciliationDTO matchItems(MatchItemsRequestDTO request, User currentUser) {
        log.info("User {} matching items for reconciliation: {}", 
                currentUser.getUsername(), request.getReconciliationId());

        Reconciliation reconciliation = reconciliationRepository.findById(request.getReconciliationId())
                .orElseThrow(() -> new RuntimeException("Reconciliation not found"));

        // Mark system items as matched
        if (request.getSystemItemIds() != null && !request.getSystemItemIds().isEmpty()) {
            for (Long itemId : request.getSystemItemIds()) {
                ReconciliationItem item = reconciliationItemRepository.findById(itemId)
                        .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));
                item.setIsMatched(true);
                item.setStatus("MATCHED");
                item.setMatchedWith("MANUAL_MATCH");
                reconciliationItemRepository.save(item);
            }
        }

        // Mark bank items as matched (if any)
        if (request.getBankItemIds() != null && !request.getBankItemIds().isEmpty()) {
            for (Long itemId : request.getBankItemIds()) {
                ReconciliationItem item = reconciliationItemRepository.findById(itemId)
                        .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));
                item.setIsMatched(true);
                item.setStatus("MATCHED");
                item.setMatchedWith("MANUAL_MATCH");
                reconciliationItemRepository.save(item);
            }
        }

        // Update reconciliation status if all items are matched
        long unmatchedCount = reconciliationItemRepository.countUnmatchedItems(request.getReconciliationId());
        
        if (unmatchedCount == 0) {
            reconciliation.setStatus("COMPLETED");
            reconciliation.setCompletedAt(LocalDateTime.now());
            reconciliation.setCompletedBy(currentUser.getId());
            reconciliationRepository.save(reconciliation);

            // Update bank account last reconciliation date
            BankAccount bankAccount = bankAccountRepository.findById(reconciliation.getBankAccountId())
                    .orElseThrow(() -> new RuntimeException("Bank account not found"));
            bankAccount.setLastReconciliationDate(LocalDateTime.now());
            bankAccount.setCurrentBalance(reconciliation.getStatementBalance());
            bankAccountRepository.save(bankAccount);
        }

        return getReconciliationById(reconciliation.getId());
    }

    @Transactional
    public ReconciliationDTO completeReconciliation(Long reconciliationId, User currentUser) {
        log.info("User {} completing reconciliation: {}", currentUser.getUsername(), reconciliationId);

        Reconciliation reconciliation = reconciliationRepository.findById(reconciliationId)
                .orElseThrow(() -> new RuntimeException("Reconciliation not found"));

        // Verify all items are matched
        long unmatchedCount = reconciliationItemRepository.countUnmatchedItems(reconciliationId);
        
        if (unmatchedCount > 0) {
            throw new RuntimeException("Cannot complete reconciliation with " + unmatchedCount + " unmatched items");
        }

        reconciliation.setStatus("COMPLETED");
        reconciliation.setCompletedAt(LocalDateTime.now());
        reconciliation.setCompletedBy(currentUser.getId());
        reconciliation = reconciliationRepository.save(reconciliation);

        // Update bank account balance
        BankAccount bankAccount = bankAccountRepository.findById(reconciliation.getBankAccountId())
                .orElseThrow(() -> new RuntimeException("Bank account not found"));
        bankAccount.setCurrentBalance(reconciliation.getStatementBalance());
        bankAccount.setLastReconciliationDate(LocalDateTime.now());
        bankAccountRepository.save(bankAccount);

        return getReconciliationById(reconciliationId);
    }

    @Transactional(readOnly = true)
    public ReconciliationDTO getReconciliationById(Long id) {
        Reconciliation reconciliation = reconciliationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reconciliation not found"));

        List<ReconciliationItem> items = reconciliationItemRepository.findByReconciliationId(id);
        List<ReconciliationItemDTO> itemDTOs = items.stream()
                .map(this::convertItemToDTO)
                .collect(Collectors.toList());

        long matchedCount = reconciliationItemRepository.countMatchedItems(id);
        long unmatchedCount = reconciliationItemRepository.countUnmatchedItems(id);
        
        ReconciliationSummaryDTO summary = ReconciliationSummaryDTO.builder()
                .totalItems(items.size())
                .matchedItems((int) matchedCount)
                .pendingItems((int) unmatchedCount)
                .totalAmount(items.stream().map(ReconciliationItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .matchedAmount(getMatchedAmount(items))
                .outstandingAmount(getOutstandingAmount(items))
                .build();

        ReconciliationDTO dto = convertToDTO(reconciliation);
        dto.setItems(itemDTOs);
        dto.setSummary(summary);
        
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationDTO> getReconciliationHistory(Long bankAccountId, Pageable pageable) {
        Page<Reconciliation> reconciliations = reconciliationRepository.findByBankAccountId(bankAccountId, pageable);
        return reconciliations.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public ReconciliationDTO getLatestReconciliation(Long bankAccountId) {
        return reconciliationRepository.findFirstByBankAccountIdOrderByReconciliationDateDesc(bankAccountId)
                .map(this::convertToDTO)
                .orElse(null);
    }

    // Bank Statement Methods
    @Transactional
    public BankStatementDTO uploadBankStatement(BankStatementDTO dto, User currentUser) {
        log.info("User {} uploading bank statement for account: {}", 
                currentUser.getUsername(), dto.getBankAccountId());

        // Check if statement already exists for this date
        java.util.Optional<BankStatement> existing = bankStatementRepository.findByBankAccountIdAndStatementDate(
            dto.getBankAccountId(), dto.getStatementDate());
        
        if (existing.isPresent()) {
            throw new RuntimeException("Bank statement already uploaded for this date");
        }

        BankStatement bankStatement = BankStatement.builder()
                .bankAccountId(dto.getBankAccountId())
                .statementDate(dto.getStatementDate())
                .openingBalance(dto.getOpeningBalance())
                .closingBalance(dto.getClosingBalance())
                .totalDeposits(dto.getTotalDeposits())
                .totalWithdrawals(dto.getTotalWithdrawals())
                .fileName(dto.getFileName())
                .status("PENDING")
                .notes(dto.getNotes())
                .build();

        bankStatement = bankStatementRepository.save(bankStatement);
        return convertToDTO(bankStatement);
    }

    // Helper Methods
    private BigDecimal getMatchedAmount(List<ReconciliationItem> items) {
        return items.stream()
                .filter(ReconciliationItem::getIsMatched)
                .map(ReconciliationItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getOutstandingAmount(List<ReconciliationItem> items) {
        return items.stream()
                .filter(item -> !item.getIsMatched())
                .map(ReconciliationItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    private BigDecimal getSystemBalanceAsOfDate(Long bankAccountId, LocalDate asOfDate) {
        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new RuntimeException("Bank account not found"));

        // Use the query method to get balance directly (more efficient)
        BigDecimal balance = generalLedgerRepository.getBalanceUpToDate(
                bankAccount.getChartOfAccount().getId(),
                asOfDate
        );

        // Add opening balance
        balance = balance.add(bankAccount.getOpeningBalance());

        return balance;
    }

    private List<GeneralLedger> getUnmatchedSystemTransactions(Long accountId, LocalDate asOfDate) {
        // Get all transactions that haven't been reconciled in the last 6 months
        LocalDate sixMonthsAgo = asOfDate.minusMonths(6);

        // Use the unreconciled transactions query
        return generalLedgerRepository.findUnreconciledTransactions(
                accountId,
                sixMonthsAgo,
                asOfDate
        );
    }

    // Alternative method using Pageable if you need pagination
    private Page<GeneralLedger> getUnmatchedSystemTransactionsPaginated(Long accountId, LocalDate asOfDate, Pageable pageable) {
        LocalDate sixMonthsAgo = asOfDate.minusMonths(6);

        return generalLedgerRepository.findByAccountIdAndTransactionDateBetween(
                accountId,
                sixMonthsAgo,
                asOfDate,
                pageable
        );
    }


    private String determineItemType(GeneralLedger transaction) {
        if (transaction.getReferenceType() != null) {
            switch (transaction.getReferenceType()) {
                case "LOAN_DISBURSEMENT":
                    return "WITHDRAWAL";
                case "LOAN_REPAYMENT":
                    return "DEPOSIT";
                case "FEE_CHARGE":
                    return "SERVICE_CHARGE";
                case "INTEREST_PAYMENT":
                    return "INTEREST";
                case "TRANSFER_IN":
                    return "DEPOSIT";
                case "TRANSFER_OUT":
                    return "WITHDRAWAL";
                default:
                    return "TRANSFER";
            }
        }
        return "TRANSFER";
    }

    private String generateReconciliationNumber() {
        return "REC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private BankAccountDTO convertToDTO(BankAccount bankAccount) {
        return BankAccountDTO.builder()
                .id(bankAccount.getId())
                .accountName(bankAccount.getAccountName())
                .accountNumber(bankAccount.getAccountNumber())
                .bankName(bankAccount.getBankName())
                .branchCode(bankAccount.getBranchCode())
                .swiftCode(bankAccount.getSwiftCode())
                .chartOfAccountId(bankAccount.getChartOfAccount().getId())
                .chartOfAccountCode(bankAccount.getChartOfAccount().getCode())
                .chartOfAccountName(bankAccount.getChartOfAccount().getName())
                .currentBalance(bankAccount.getCurrentBalance())
                .availableBalance(bankAccount.getAvailableBalance())
                .currency(bankAccount.getCurrency())
                .status(bankAccount.getStatus())
                .lastReconciliationDate(bankAccount.getLastReconciliationDate())
                .openingBalance(bankAccount.getOpeningBalance())
                .notes(bankAccount.getNotes())
                .createdAt(bankAccount.getCreatedAt())
                .updatedAt(bankAccount.getUpdatedAt())
                .build();
    }

    private ReconciliationDTO convertToDTO(Reconciliation reconciliation) {
        return ReconciliationDTO.builder()
                .id(reconciliation.getId())
                .reconciliationNumber(reconciliation.getReconciliationNumber())
                .reconciliationDate(reconciliation.getReconciliationDate())
                .bankAccountId(reconciliation.getBankAccountId())
                .bankAccountName(reconciliation.getBankAccountName())
                .bankAccountNumber(reconciliation.getBankAccountNumber())
                .systemBalance(reconciliation.getSystemBalance())
                .statementBalance(reconciliation.getStatementBalance())
                .difference(reconciliation.getDifference())
                .status(reconciliation.getStatus())
                .completedAt(reconciliation.getCompletedAt())
                .completedBy(reconciliation.getCompletedBy())
                .notes(reconciliation.getNotes())
                .build();
    }

    private ReconciliationItemDTO convertItemToDTO(ReconciliationItem item) {
        return ReconciliationItemDTO.builder()
                .id(item.getId())
                .transactionDate(item.getTransactionDate())
                .description(item.getDescription())
                .referenceNumber(item.getReferenceNumber())
                .itemType(item.getItemType())
                .category(item.getCategory())
                .amount(item.getAmount())
                .status(item.getStatus())
                .notes(item.getNotes())
                .isMatched(item.getIsMatched())
                .matchedWith(item.getMatchedWith())
                .build();
    }

    private BankStatementDTO convertToDTO(BankStatement bankStatement) {
        return BankStatementDTO.builder()
                .id(bankStatement.getId())
                .bankAccountId(bankStatement.getBankAccountId())
                .statementDate(bankStatement.getStatementDate())
                .openingBalance(bankStatement.getOpeningBalance())
                .closingBalance(bankStatement.getClosingBalance())
                .totalDeposits(bankStatement.getTotalDeposits())
                .totalWithdrawals(bankStatement.getTotalWithdrawals())
                .fileName(bankStatement.getFileName())
                .status(bankStatement.getStatus())
                .notes(bankStatement.getNotes())
                .build();
    }
}