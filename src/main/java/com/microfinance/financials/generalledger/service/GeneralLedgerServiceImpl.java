// service/impl/GeneralLedgerServiceImpl.java
package com.microfinance.financials.generalledger.service;
import com.microfinance.financials.chartofaccounts.entity.Account;
import com.microfinance.financials.chartofaccounts.repository.AccountRepository;
import com.microfinance.financials.generalledger.dto.*;
import com.microfinance.financials.generalledger.entity.FinancialPeriod;
import com.microfinance.financials.generalledger.entity.GeneralLedger;
import com.microfinance.financials.generalledger.entity.JournalEntry;
import com.microfinance.financials.generalledger.entity.JournalEntryLine;
import com.microfinance.financials.generalledger.repository.FinancialPeriodRepository;
import com.microfinance.financials.generalledger.repository.GeneralLedgerRepository;
import com.microfinance.base.entity.User;
import com.microfinance.financials.generalledger.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneralLedgerServiceImpl implements GeneralLedgerService {

    private final JournalEntryRepository journalEntryRepository;
    private final GeneralLedgerRepository generalLedgerRepository;
    private final FinancialPeriodRepository financialPeriodRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public JournalEntryDto createJournalEntry(JournalEntryDto dto, User currentUser) {
        log.info("Creating journal entry for user: {}", currentUser.getUsername());
        
        // Generate journal number
        String journalNumber = generateJournalNumber();
        
        // Get current financial period
        FinancialPeriod currentPeriod = getCurrentFinancialPeriodEntity();
        
        // Validate debit = credit
        BigDecimal totalDebit = dto.getLines().stream()
                .filter(line -> "DEBIT".equals(line.getDebitCredit()))
                .map(JournalEntryLineDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredit = dto.getLines().stream()
                .filter(line -> "CREDIT".equals(line.getDebitCredit()))
                .map(JournalEntryLineDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new RuntimeException("Total debits must equal total credits");
        }
        
        JournalEntry journalEntry = JournalEntry.builder()
                .journalNumber(journalNumber)
                .entryDate(dto.getEntryDate() != null ? dto.getEntryDate() : LocalDate.now())
                .description(dto.getDescription())
                .journalType(dto.getJournalType())
                .status("DRAFT")
                .financialPeriod(currentPeriod)
                .createdBy(currentUser.getId())
                .build();
        
        journalEntry = journalEntryRepository.save(journalEntry);
        
        // Create journal entry lines
        List<JournalEntryLine> lines = new ArrayList<>();
        for (JournalEntryLineDto lineDto : dto.getLines()) {
            Account account = accountRepository.findById(lineDto.getAccountId())
                    .orElseThrow(() -> new RuntimeException("Account not found: " + lineDto.getAccountId()));
            
            JournalEntryLine line = JournalEntryLine.builder()
                    .journalEntry(journalEntry)
                    .account(account)
                    .accountCode(account.getCode())
                    .accountName(account.getName())
                    .debitCredit(lineDto.getDebitCredit())
                    .amount(lineDto.getAmount())
                    .description(lineDto.getDescription())
                    .build();
            lines.add(line);
        }
        journalEntry.setLines(lines);
        
        journalEntry = journalEntryRepository.save(journalEntry);
        
        log.info("Journal entry created with number: {}", journalNumber);
        
        return convertToJournalEntryDto(journalEntry);
    }

    @Override
    @Transactional
    public JournalEntryDto postJournalEntry(Long id, User currentUser) {
        log.info("Posting journal entry with ID: {}", id);
        
        JournalEntry journalEntry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Journal entry not found"));
        
        if (!"DRAFT".equals(journalEntry.getStatus())) {
            throw new RuntimeException("Only draft journal entries can be posted");
        }
        
        // Create general ledger entries
        for (JournalEntryLine line : journalEntry.getLines()) {
            GeneralLedger gl = GeneralLedger.builder()
                    .journalId(journalEntry.getId())
                    .journalEntryId(journalEntry.getId())
                    .transactionDate(journalEntry.getEntryDate())
                    .account(line.getAccount())
                    .accountCode(line.getAccountCode())
                    .accountName(line.getAccountName())
                    .debitCredit(line.getDebitCredit())
                    .amount(line.getAmount())
                    .description(line.getDescription())
                    .referenceNumber(journalEntry.getJournalNumber())
                    .referenceType("JOURNAL_ENTRY")
                    .referenceId(journalEntry.getId())
                    .financialPeriod(journalEntry.getFinancialPeriod())
                    .isReversed(false)
                    .build();
            
            generalLedgerRepository.save(gl);
            
            // Update account balance
            updateAccountBalance(line.getAccount(), line.getDebitCredit(), line.getAmount());
        }
        
        journalEntry.setStatus("POSTED");
        journalEntry.setPostedBy(currentUser.getId());
        journalEntry.setPostedAt(LocalDateTime.now());
        
        journalEntry = journalEntryRepository.save(journalEntry);
        
        log.info("Journal entry posted successfully: {}", journalEntry.getJournalNumber());
        
        return convertToJournalEntryDto(journalEntry);
    }

    @Override
    @Transactional
    public JournalEntryDto reverseJournalEntry(Long id, String reason, User currentUser) {
        log.info("Reversing journal entry with ID: {}", id);
        
        JournalEntry originalEntry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Journal entry not found"));
        
        if (!"POSTED".equals(originalEntry.getStatus())) {
            throw new RuntimeException("Only posted journal entries can be reversed");
        }
        
        // Create reversal journal entry
        JournalEntry reversalEntry = JournalEntry.builder()
                .journalNumber(generateJournalNumber())
                .entryDate(LocalDate.now())
                .description("Reversal of: " + originalEntry.getDescription())
                .journalType(originalEntry.getJournalType())
                .status("DRAFT")
                .financialPeriod(getCurrentFinancialPeriodEntity())
                .createdBy(currentUser.getId())
                .build();
        
        reversalEntry = journalEntryRepository.save(reversalEntry);
        
        // Create reversal lines (opposite debit/credit)
        List<JournalEntryLine> reversalLines = new ArrayList<>();
        for (JournalEntryLine originalLine : originalEntry.getLines()) {
            String oppositeDebitCredit = "DEBIT".equals(originalLine.getDebitCredit()) ? "CREDIT" : "DEBIT";
            
            JournalEntryLine reversalLine = JournalEntryLine.builder()
                    .journalEntry(reversalEntry)
                    .account(originalLine.getAccount())
                    .accountCode(originalLine.getAccountCode())
                    .accountName(originalLine.getAccountName())
                    .debitCredit(oppositeDebitCredit)
                    .amount(originalLine.getAmount())
                    .description("Reversal: " + originalLine.getDescription())
                    .build();
            reversalLines.add(reversalLine);
        }
        reversalEntry.setLines(reversalLines);
        
        reversalEntry = journalEntryRepository.save(reversalEntry);
        
        // Post the reversal entry
        postJournalEntry(reversalEntry.getId(), currentUser);
        
        // Mark original as reversed
        originalEntry.setStatus("REVERSED");
        originalEntry.setReversedBy(currentUser.getId());
        originalEntry.setReversedAt(LocalDateTime.now());
        originalEntry.setReversalReason(reason);
        journalEntryRepository.save(originalEntry);
        
        log.info("Journal entry reversed: {} with reversal: {}", 
                 originalEntry.getJournalNumber(), reversalEntry.getJournalNumber());
        
        return convertToJournalEntryDto(reversalEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JournalEntryDto> getJournalEntries(String status, LocalDate startDate, 
                                                    LocalDate endDate, Pageable pageable, User currentUser) {
        log.info("Fetching journal entries with filters");
        
        Page<JournalEntry> entries;
        if (status != null && !status.isEmpty()) {
            entries = journalEntryRepository.findByStatus(status, pageable);
        } else if (startDate != null && endDate != null) {
            List<JournalEntry> entryList = journalEntryRepository.findByEntryDateBetween(startDate, endDate);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), entryList.size());
            entries = new PageImpl<>(entryList.subList(start, end), pageable, entryList.size());
        } else {
            entries = journalEntryRepository.findAll(pageable);
        }
        
        return entries.map(this::convertToJournalEntryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public JournalEntryDto getJournalEntryById(Long id, User currentUser) {
        JournalEntry journalEntry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Journal entry not found"));
        return convertToJournalEntryDto(journalEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GeneralLedgerDto> getLedgerEntries(Long accountId, LocalDate startDate,
                                                   LocalDate endDate, Pageable pageable, User currentUser) {
        log.info("Fetching ledger entries");
        
        Page<GeneralLedger> entries;
        if (accountId != null) {
            entries = generalLedgerRepository.findByAccountId(accountId, pageable);
        } else if (startDate != null && endDate != null) {
            entries = generalLedgerRepository.findByTransactionDateBetween(startDate, endDate, pageable);
        } else {
            entries = generalLedgerRepository.findAll(pageable);
        }
        
        return entries.map(this::convertToGeneralLedgerDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrialBalanceDto> getTrialBalance(LocalDate asOfDate, User currentUser) {
        log.info("Generating trial balance as of: {}", asOfDate);
        
        LocalDate startDate = LocalDate.of(asOfDate.getYear(), 1, 1);
        LocalDate endDate = asOfDate;
        
        List<Object[]> results = generalLedgerRepository.getTrialBalance(startDate, endDate);
        
        List<TrialBalanceDto> trialBalance = new ArrayList<>();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        
        for (Object[] row : results) {
            String accountCode = (String) row[0];
            String accountName = (String) row[1];
            BigDecimal debit = (BigDecimal) row[2];
            BigDecimal credit = (BigDecimal) row[3];
            BigDecimal balance = debit.subtract(credit);
            
            totalDebits = totalDebits.add(debit);
            totalCredits = totalCredits.add(credit);
            
            trialBalance.add(TrialBalanceDto.builder()
                    .accountCode(accountCode)
                    .accountName(accountName)
                    .debit(debit)
                    .credit(credit)
                    .balance(balance)
                    .build());
        }
        
        // Add totals row
        trialBalance.add(TrialBalanceDto.builder()
                .accountCode("")
                .accountName("TOTAL")
                .debit(totalDebits)
                .credit(totalCredits)
                .balance(totalDebits.subtract(totalCredits))
                .isTotal(true)
                .build());
        
        return trialBalance;
    }

    @Override
    @Transactional
    public FinancialPeriodDto createFinancialPeriod(FinancialPeriodDto dto, User currentUser) {
        log.info("Creating financial period: {}", dto.getPeriodName());
        
        // Check if period already exists
        if (financialPeriodRepository.existsByYearAndMonth(dto.getYear(), dto.getMonth())) {
            throw new RuntimeException("Financial period already exists for this month");
        }
        
        FinancialPeriod period = FinancialPeriod.builder()
                .year(dto.getYear())
                .month(dto.getMonth())
                .periodName(dto.getPeriodName())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status("OPEN")
                .build();
        
        period = financialPeriodRepository.save(period);
        
        log.info("Financial period created: {}", period.getPeriodName());
        
        return convertToFinancialPeriodDto(period);
    }

    @Override
    @Transactional
    public FinancialPeriodDto closeFinancialPeriod(Long id, User currentUser) {
        log.info("Closing financial period with ID: {}", id);
        
        FinancialPeriod period = financialPeriodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Financial period not found"));
        
        // Check if there are any unposted transactions
        // This would need additional logic based on your business rules
        
        period.setStatus("CLOSED");
        period.setClosedAt(LocalDateTime.now());
        period.setClosedBy(currentUser.getId());
        
        period = financialPeriodRepository.save(period);
        
        log.info("Financial period closed: {}", period.getPeriodName());
        
        return convertToFinancialPeriodDto(period);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialPeriodDto> getFinancialPeriods(Integer year, User currentUser) {
        List<FinancialPeriod> periods;
        if (year != null) {
            periods = financialPeriodRepository.findByYear(year);
        } else {
            periods = financialPeriodRepository.findAllByOrderByYearDescMonthDesc();
        }
        return periods.stream()
                .map(this::convertToFinancialPeriodDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialPeriodDto getCurrentFinancialPeriod(User currentUser) {
        FinancialPeriod period = getCurrentFinancialPeriodEntity();
        return convertToFinancialPeriodDto(period);
    }

    // Helper methods
    private String generateJournalNumber() {
        String prefix = "JNL";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = journalEntryRepository.count() + 1;
        String sequence = String.format("%04d", count);
        return prefix + "-" + datePart + "-" + sequence;
    }
    
    private FinancialPeriod getCurrentFinancialPeriodEntity() {
        LocalDate now = LocalDate.now();
        return financialPeriodRepository.findByYearAndMonth(now.getYear(), now.getMonthValue())
                .orElseThrow(() -> new RuntimeException("No open financial period found for current month"));
    }
    
    private void updateAccountBalance(Account account, String debitCredit, BigDecimal amount) {
        BigDecimal currentBalance = account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal newBalance;
        
        if ("DEBIT".equals(debitCredit)) {
            newBalance = currentBalance.add(amount);
        } else {
            newBalance = currentBalance.subtract(amount);
        }
        
        account.setCurrentBalance(newBalance);
        accountRepository.save(account);
    }
    
    // Conversion methods
    private JournalEntryDto convertToJournalEntryDto(JournalEntry entry) {
        List<JournalEntryLineDto> lineDtos = entry.getLines().stream()
                .map(line -> JournalEntryLineDto.builder()
                        .id(line.getId())
                        .accountId(line.getAccount().getId())
                        .accountCode(line.getAccountCode())
                        .accountName(line.getAccountName())
                        .debitCredit(line.getDebitCredit())
                        .amount(line.getAmount())
                        .description(line.getDescription())
                        .build())
                .collect(Collectors.toList());
        
        BigDecimal totalDebit = lineDtos.stream()
                .filter(l -> "DEBIT".equals(l.getDebitCredit()))
                .map(JournalEntryLineDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredit = lineDtos.stream()
                .filter(l -> "CREDIT".equals(l.getDebitCredit()))
                .map(JournalEntryLineDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return JournalEntryDto.builder()
                .id(entry.getId())
                .journalNumber(entry.getJournalNumber())
                .entryDate(entry.getEntryDate())
                .description(entry.getDescription())
                .journalType(entry.getJournalType())
                .status(entry.getStatus())
                .financialPeriodId(entry.getFinancialPeriod() != null ? entry.getFinancialPeriod().getId() : null)
                .periodName(entry.getFinancialPeriod() != null ? entry.getFinancialPeriod().getPeriodName() : null)
                .lines(lineDtos)
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .postedAt(entry.getPostedAt())
                .postedBy(entry.getPostedBy())
                .createdAt(entry.getCreatedAt())
                .build();
    }
    
    private GeneralLedgerDto convertToGeneralLedgerDto(GeneralLedger gl) {
        return GeneralLedgerDto.builder()
                .id(gl.getId())
                .journalId(gl.getJournalId())
                .transactionDate(gl.getTransactionDate())
                .accountId(gl.getAccount().getId())
                .accountCode(gl.getAccountCode())
                .accountName(gl.getAccountName())
                .debitCredit(gl.getDebitCredit())
                .amount(gl.getAmount())
                .description(gl.getDescription())
                .referenceNumber(gl.getReferenceNumber())
                .referenceType(gl.getReferenceType())
                .createdAt(gl.getCreatedAt())
                .build();
    }
    
    private FinancialPeriodDto convertToFinancialPeriodDto(FinancialPeriod period) {
        return FinancialPeriodDto.builder()
                .id(period.getId())
                .year(period.getYear())
                .month(period.getMonth())
                .periodName(period.getPeriodName())
                .startDate(period.getStartDate())
                .endDate(period.getEndDate())
                .status(period.getStatus())
                .closedAt(period.getClosedAt())
                .closedBy(period.getClosedBy())
                .build();
    }
}