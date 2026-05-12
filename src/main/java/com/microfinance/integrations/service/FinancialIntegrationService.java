// service/FinancialIntegrationService.java
package com.microfinance.integrations.service;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.financials.chartofaccounts.entity.Account;
import com.microfinance.financials.chartofaccounts.repository.AccountRepository;
import com.microfinance.financials.generalledger.entity.FinancialPeriod;
import com.microfinance.financials.generalledger.entity.GeneralLedger;
import com.microfinance.financials.generalledger.entity.JournalEntry;
import com.microfinance.financials.generalledger.entity.JournalEntryLine;
import com.microfinance.financials.generalledger.repository.FinancialPeriodRepository;
import com.microfinance.financials.generalledger.repository.GeneralLedgerRepository;
import com.microfinance.financials.generalledger.repository.JournalEntryRepository;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.entity.LoanRepayment;
import com.microfinance.loanapplications.entity.RepaymentSchedule;
import com.microfinance.loanapplications.repository.LoanRepository;
import com.microfinance.loanapplications.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialIntegrationService {

    private final JournalEntryRepository journalEntryRepository;
    private final GeneralLedgerRepository generalLedgerRepository;
    private final AccountRepository accountRepository;
    private final FinancialPeriodRepository financialPeriodRepository;
    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    // Account Codes - Reference from Chart of Accounts
    private static final String ACCOUNT_CASH_ON_HAND = "1010";
    private static final String ACCOUNT_BANK_OPERATING = "1020";
    private static final String ACCOUNT_BANK_COLLECTIONS = "1030";
    private static final String ACCOUNT_GROSS_LOAN_PORTFOLIO = "1110";
    private static final String ACCOUNT_INTEREST_RECEIVABLE = "1120";
    private static final String ACCOUNT_FEES_RECEIVABLE = "1130";
    private static final String ACCOUNT_LOAN_LOSS_PROVISION = "1310";
    private static final String ACCOUNT_INTEREST_INCOME = "4010";
    private static final String ACCOUNT_FEE_INCOME = "4030";
    private static final String ACCOUNT_PENALTY_INCOME = "4060";
    private static final String ACCOUNT_RECOVERY_INCOME = "4110";
    private static final String ACCOUNT_PROVISION_EXPENSE = "5210";
    private static final String ACCOUNT_WRITE_OFF_EXPENSE = "5220";

    /**
     * Create journal entry for loan disbursement
     * DR: Gross Loan Portfolio (Asset)
     * CR: Cash/Bank (Asset)
     */
    @Transactional
    public void recordLoanDisbursement(Loan loan, BigDecimal disbursedAmount, User currentUser) {
        log.info("Recording journal entry for loan disbursement: {}", loan.getLoanAccountNumber());

        try {
            // Get current financial period
            FinancialPeriod financialPeriod = getCurrentFinancialPeriod();
            // Generate journal number
            String journalNumber = generateJournalNumber("DISB");
            
            // Create journal entry
            JournalEntry journalEntry = JournalEntry.builder()
                    .journalNumber(journalNumber)
                    .entryDate(LocalDate.now())
                    .description(String.format("Loan Disbursement - %s for %s", 
                            loan.getLoanAccountNumber(), loan.getBorrower().getFullName()))
                    .journalType("DISBURSEMENT")
                    .status("POSTED")
                    .financialPeriod(financialPeriod)
                    .createdBy(currentUser != null ? currentUser.getId() : null)
                    .postedAt(LocalDateTime.now())
                    .postedBy(currentUser != null ? currentUser.getId() : null)
                    .referenceNumber(loan.getLoanAccountNumber())
                    .build();
            
            List<JournalEntryLine> lines = new ArrayList<>();
            
            // DR: Gross Loan Portfolio
            Account grossLoanPortfolio = accountRepository.findByCode(ACCOUNT_GROSS_LOAN_PORTFOLIO)
                    .orElseThrow(() -> new RuntimeException("Account not found: " + ACCOUNT_GROSS_LOAN_PORTFOLIO));
            
            JournalEntryLine drLine = JournalEntryLine.builder()
                    .journalEntry(journalEntry)
                    .account(grossLoanPortfolio)
                    .accountCode(grossLoanPortfolio.getCode())
                    .accountName(grossLoanPortfolio.getName())
                    .debitCredit("DEBIT")
                    .amount(disbursedAmount)
                    .description("Principal disbursed")
                    .build();
            lines.add(drLine);
            
            // CR: Cash/Bank (Operating account)
            Account bankAccount = accountRepository.findByCode(ACCOUNT_BANK_OPERATING)
                    .orElseThrow(() -> new RuntimeException("Account not found: " + ACCOUNT_BANK_OPERATING));
            
            JournalEntryLine crLine = JournalEntryLine.builder()
                    .journalEntry(journalEntry)
                    .account(bankAccount)
                    .accountCode(bankAccount.getCode())
                    .accountName(bankAccount.getName())
                    .debitCredit("CREDIT")
                    .amount(disbursedAmount)
                    .description("Cash disbursed")
                    .build();
            lines.add(crLine);
            
            journalEntry.setLines(lines);
            journalEntryRepository.save(journalEntry);
            
            // Post to General Ledger
            postToGeneralLedger(journalEntry, loan.getId(), "LOAN_DISBURSEMENT", loan.getLoanAccountNumber());
            
            log.info("Journal entry created for disbursement: {}", journalNumber);
            
        } catch (Exception e) {
            log.error("Error recording loan disbursement journal entry", e);
            throw new RuntimeException("Failed to record loan disbursement journal entry", e);
        }
    }

    /**
     * Create journal entry for loan repayment
     * DR: Cash/Bank (Asset)
     * CR: Interest Income (Income)
     * CR: Fee Income (Income)
     * CR: Penalty Income (Income)
     * CR: Gross Loan Portfolio (Asset - Principal)
     */
    @Transactional
    public void recordLoanRepayment(Loan loan, LoanRepayment repayment, User currentUser) {
        log.info("Recording journal entry for loan repayment: {} - Amount: {}", 
                loan.getLoanAccountNumber(), repayment.getAmountPaid());

        try {
            FinancialPeriod financialPeriod = getCurrentFinancialPeriod();
            String journalNumber = generateJournalNumber("REPAY");
            
            JournalEntry journalEntry = JournalEntry.builder()
                    .journalNumber(journalNumber)
                    .entryDate(repayment.getPaymentDate())
                    .description(String.format("Loan Repayment - %s from %s", 
                            loan.getLoanAccountNumber(), loan.getBorrower().getFullName()))
                    .journalType("REPAYMENT")
                    .status("POSTED")
                    .referenceNumber(loan.getLoanAccountNumber())
                    .financialPeriod(financialPeriod)
                    .createdBy(currentUser.getId())
                    .postedAt(LocalDateTime.now())
                    .postedBy(currentUser.getId())
                    .build();
            
            List<JournalEntryLine> lines = new ArrayList<>();
            
            // DR: Cash/Bank (Total amount received)
            Account bankAccount = accountRepository.findByCode(ACCOUNT_BANK_COLLECTIONS)
                    .orElseThrow(() -> new RuntimeException("Account not found: " + ACCOUNT_BANK_COLLECTIONS));
            
            JournalEntryLine drLine = JournalEntryLine.builder()
                    .journalEntry(journalEntry)
                    .account(bankAccount)
                    .accountCode(bankAccount.getCode())
                    .accountName(bankAccount.getName())
                    .debitCredit("DEBIT")
                    .amount(repayment.getAmountPaid())
                    .description("Cash received")
                    .build();
            lines.add(drLine);
            
            // CR: Interest Income
            if (repayment.getInterestAmount() != null && repayment.getInterestAmount().compareTo(BigDecimal.ZERO) > 0) {
                Account interestIncome = accountRepository.findByCode(ACCOUNT_INTEREST_INCOME)
                        .orElseThrow(() -> new RuntimeException("Account not found: " + ACCOUNT_INTEREST_INCOME));
                
                JournalEntryLine interestLine = JournalEntryLine.builder()
                        .journalEntry(journalEntry)
                        .account(interestIncome)
                        .accountCode(interestIncome.getCode())
                        .accountName(interestIncome.getName())
                        .debitCredit("CREDIT")
                        .amount(repayment.getInterestAmount())
                        .description("Interest received")
                        .build();
                lines.add(interestLine);
                
                // Also reverse interest receivable if it was accrued
               // reverseInterestReceivable(loan, repayment.getInterestAmount(), journalEntry, lines);
            }
            
            // CR: Fee Income
            if (repayment.getFeesAmount() != null && repayment.getFeesAmount().compareTo(BigDecimal.ZERO) > 0) {
                Account feeIncome = accountRepository.findByCode(ACCOUNT_FEE_INCOME)
                        .orElseThrow(() -> new RuntimeException("Account not found: " + ACCOUNT_FEE_INCOME));
                
                JournalEntryLine feeLine = JournalEntryLine.builder()
                        .journalEntry(journalEntry)
                        .account(feeIncome)
                        .accountCode(feeIncome.getCode())
                        .accountName(feeIncome.getName())
                        .debitCredit("CREDIT")
                        .amount(repayment.getFeesAmount())
                        .description("Fees received")
                        .build();
                lines.add(feeLine);
            }
            
            // CR: Penalty Income
            if (repayment.getPenaltyAmount() != null && repayment.getPenaltyAmount().compareTo(BigDecimal.ZERO) > 0) {
                Account penaltyIncome = accountRepository.findByCode(ACCOUNT_PENALTY_INCOME)
                        .orElseThrow(() -> new RuntimeException("Account not found: " + ACCOUNT_PENALTY_INCOME));
                
                JournalEntryLine penaltyLine = JournalEntryLine.builder()
                        .journalEntry(journalEntry)
                        .account(penaltyIncome)
                        .accountCode(penaltyIncome.getCode())
                        .accountName(penaltyIncome.getName())
                        .debitCredit("CREDIT")
                        .amount(repayment.getPenaltyAmount())
                        .description("Penalty received")
                        .build();
                lines.add(penaltyLine);
            }
            
            // CR: Gross Loan Portfolio (Principal)
            if (repayment.getPrincipalAmount() != null && repayment.getPrincipalAmount().compareTo(BigDecimal.ZERO) > 0) {
                Account loanPortfolio = accountRepository.findByCode(ACCOUNT_GROSS_LOAN_PORTFOLIO)
                        .orElseThrow(() -> new RuntimeException("Account not found: " + ACCOUNT_GROSS_LOAN_PORTFOLIO));
                
                JournalEntryLine principalLine = JournalEntryLine.builder()
                        .journalEntry(journalEntry)
                        .account(loanPortfolio)
                        .accountCode(loanPortfolio.getCode())
                        .accountName(loanPortfolio.getName())
                        .debitCredit("CREDIT")
                        .amount(repayment.getPrincipalAmount())
                        .description("Principal repayment")
                        .build();
                lines.add(principalLine);
            }
            
            journalEntry.setLines(lines);
            journalEntryRepository.save(journalEntry);
            
            // Post to General Ledger
            postToGeneralLedger(journalEntry, loan.getId(), "LOAN_REPAYMENT", repayment.getTransactionReference());
            
            log.info("Journal entry created for repayment: {}", journalNumber);
            
        } catch (Exception e) {
            log.error("Error recording loan repayment journal entry", e);
            throw new RuntimeException("Failed to record loan repayment journal entry", e);
        }
    }

    /**
     * Reverse interest receivable when payment is received
     */
    private void reverseInterestReceivable(Loan loan, BigDecimal interestAmount, 
                                           JournalEntry journalEntry, List<JournalEntryLine> lines) {
        Account interestReceivable = accountRepository.findByCode(ACCOUNT_INTEREST_RECEIVABLE)
                .orElse(null);
        
        if (interestReceivable != null && interestAmount.compareTo(BigDecimal.ZERO) > 0) {
            JournalEntryLine reverseLine = JournalEntryLine.builder()
                    .journalEntry(journalEntry)
                    .account(interestReceivable)
                    .accountCode(interestReceivable.getCode())
                    .accountName(interestReceivable.getName())
                    .debitCredit("CREDIT")
                    .amount(interestAmount)
                    .description("Reverse accrued interest")
                    .build();
            lines.add(reverseLine);
        }
    }

    /**
     * Create journal entry for loan write-off
     * DR: Loan Loss Provision (Contra Asset)
     * CR: Gross Loan Portfolio (Asset)
     * DR: Write-off Expense (if provision insufficient)
     */
    @Transactional
    public void recordLoanWriteOff(Loan loan, BigDecimal writeOffAmount, BigDecimal provisionAmount, User currentUser) {
        log.info("Recording journal entry for loan write-off: {}", loan.getLoanAccountNumber());

        try {
            FinancialPeriod financialPeriod = getCurrentFinancialPeriod();
            String journalNumber = generateJournalNumber("WRITE");
            
            JournalEntry journalEntry = JournalEntry.builder()
                    .journalNumber(journalNumber)
                    .entryDate(LocalDate.now())
                    .description(String.format("Loan Write-off - %s - %s", 
                            loan.getLoanAccountNumber(), loan.getBorrower().getFullName()))
                    .journalType("WRITE_OFF")
                    .status("POSTED")
                    .referenceNumber(loan.getLoanAccountNumber())
                    .financialPeriod(financialPeriod)
                    .createdBy(currentUser.getId())
                    .postedAt(LocalDateTime.now())
                    .postedBy(currentUser.getId())
                    .build();
            
            List<JournalEntryLine> lines = new ArrayList<>();
            
            // DR: Loan Loss Provision (up to provision amount)
            if (provisionAmount != null && provisionAmount.compareTo(BigDecimal.ZERO) > 0) {
                Account loanLossProvision = accountRepository.findByCode(ACCOUNT_LOAN_LOSS_PROVISION)
                        .orElseThrow(() -> new RuntimeException("Account not found: " + ACCOUNT_LOAN_LOSS_PROVISION));
                
                JournalEntryLine provisionLine = JournalEntryLine.builder()
                        .journalEntry(journalEntry)
                        .account(loanLossProvision)
                        .accountCode(loanLossProvision.getCode())
                        .accountName(loanLossProvision.getName())
                        .debitCredit("DEBIT")
                        .amount(provisionAmount.min(writeOffAmount))
                        .description("Use provision for write-off")
                        .build();
                lines.add(provisionLine);
            }
            
            // If write-off amount exceeds provision, DR Write-off Expense
            BigDecimal excessAmount = writeOffAmount.subtract(provisionAmount != null ? provisionAmount : BigDecimal.ZERO);
            if (excessAmount.compareTo(BigDecimal.ZERO) > 0) {
                Account writeOffExpense = accountRepository.findByCode(ACCOUNT_WRITE_OFF_EXPENSE)
                        .orElseThrow(() -> new RuntimeException("Account not found: " + ACCOUNT_WRITE_OFF_EXPENSE));
                
                JournalEntryLine expenseLine = JournalEntryLine.builder()
                        .journalEntry(journalEntry)
                        .account(writeOffExpense)
                        .accountCode(writeOffExpense.getCode())
                        .accountName(writeOffExpense.getName())
                        .debitCredit("DEBIT")
                        .amount(excessAmount)
                        .description("Write-off expense")
                        .build();
                lines.add(expenseLine);
            }
            
            // CR: Gross Loan Portfolio
            Account loanPortfolio = accountRepository.findByCode(ACCOUNT_GROSS_LOAN_PORTFOLIO)
                    .orElseThrow(() -> new RuntimeException("Account not found: " + ACCOUNT_GROSS_LOAN_PORTFOLIO));
            
            JournalEntryLine creditLine = JournalEntryLine.builder()
                    .journalEntry(journalEntry)
                    .account(loanPortfolio)
                    .accountCode(loanPortfolio.getCode())
                    .accountName(loanPortfolio.getName())
                    .debitCredit("CREDIT")
                    .amount(writeOffAmount)
                    .description("Remove loan from portfolio")
                    .build();
            lines.add(creditLine);
            
            journalEntry.setLines(lines);
            journalEntryRepository.save(journalEntry);
            
            // Post to General Ledger
            postToGeneralLedger(journalEntry, loan.getId(), "LOAN_WRITE_OFF", "Write-off");
            
            log.info("Journal entry created for write-off: {}", journalNumber);
            
        } catch (Exception e) {
            log.error("Error recording loan write-off journal entry", e);
            throw new RuntimeException("Failed to record loan write-off journal entry", e);
        }
    }

    /**
     * Create journal entry for loan recovery after write-off
     * DR: Cash/Bank (Asset)
     * CR: Recovery Income (Income)
     */
    @Transactional
    public void recordLoanRecovery(Loan loan, BigDecimal recoveredAmount, String referenceNumber, User currentUser) {
        log.info("Recording journal entry for loan recovery: {} - Amount: {}", 
                loan.getLoanAccountNumber(), recoveredAmount);

        try {
            FinancialPeriod financialPeriod = getCurrentFinancialPeriod();
            String journalNumber = generateJournalNumber("RECOV");
            
            JournalEntry journalEntry = JournalEntry.builder()
                    .journalNumber(journalNumber)
                    .entryDate(LocalDate.now())
                    .description(String.format("Loan Recovery - %s - %s", 
                            loan.getLoanAccountNumber(), loan.getBorrower().getFullName()))
                    .journalType("RECOVERY")
                    .status("POSTED")
                    .referenceNumber(loan.getLoanAccountNumber())
                    .financialPeriod(financialPeriod)
                    .createdBy(currentUser.getId())
                    .postedAt(LocalDateTime.now())
                    .postedBy(currentUser.getId())
                    .build();
            
            List<JournalEntryLine> lines = new ArrayList<>();
            
            // DR: Cash/Bank
            Account bankAccount = accountRepository.findByCode(ACCOUNT_BANK_COLLECTIONS)
                    .orElseThrow(() -> new RuntimeException("Account not found: " + ACCOUNT_BANK_COLLECTIONS));
            
            JournalEntryLine drLine = JournalEntryLine.builder()
                    .journalEntry(journalEntry)
                    .account(bankAccount)
                    .accountCode(bankAccount.getCode())
                    .accountName(bankAccount.getName())
                    .debitCredit("DEBIT")
                    .amount(recoveredAmount)
                    .description(String.format("Recovery received - Ref: %s", referenceNumber))
                    .build();
            lines.add(drLine);
            
            // CR: Recovery Income
            Account recoveryIncome = accountRepository.findByCode(ACCOUNT_RECOVERY_INCOME)
                    .orElseThrow(() -> new RuntimeException("Account not found: " + ACCOUNT_RECOVERY_INCOME));
            
            JournalEntryLine crLine = JournalEntryLine.builder()
                    .journalEntry(journalEntry)
                    .account(recoveryIncome)
                    .accountCode(recoveryIncome.getCode())
                    .accountName(recoveryIncome.getName())
                    .debitCredit("CREDIT")
                    .amount(recoveredAmount)
                    .description("Recovery income")
                    .build();
            lines.add(crLine);
            
            journalEntry.setLines(lines);
            journalEntryRepository.save(journalEntry);
            
            // Post to General Ledger
            postToGeneralLedger(journalEntry, loan.getId(), "LOAN_RECOVERY", referenceNumber);
            
            log.info("Journal entry created for recovery: {}", journalNumber);
            
        } catch (Exception e) {
            log.error("Error recording loan recovery journal entry", e);
            throw new RuntimeException("Failed to record loan recovery journal entry", e);
        }
    }

    /**
     * Run monthly interest accruals for all active loans
     */
    @Transactional
    public void runMonthlyInterestAccruals(LocalDate accrualDate, User currentUser) {
        log.info("Running monthly interest accruals as of: {}", accrualDate);

        try {
            List<Loan> activeLoans = loanRepository.findByStatus(GeneralConfig.LoanStatus.valueOf("ACTIVE"));
            int accrualCount = 0;
            
            for (Loan loan : activeLoans) {
                BigDecimal accruedInterest = calculateAccruedInterest(loan, accrualDate);
                
                if (accruedInterest.compareTo(BigDecimal.ZERO) > 0) {
                    createInterestAccrualJournal(loan, accruedInterest, accrualDate, currentUser);
                    accrualCount++;
                }
            }
            
            log.info("Completed interest accruals for {} loans", accrualCount);
            
        } catch (Exception e) {
            log.error("Error running monthly interest accruals", e);
            throw new RuntimeException("Failed to run monthly interest accruals", e);
        }
    }

    /**
     * Create interest accrual journal entry
     * DR: Interest Receivable (Asset)
     * CR: Interest Income (Income)
     */
    private void createInterestAccrualJournal(Loan loan, BigDecimal accruedInterest, 
                                               LocalDate accrualDate, User currentUser) {
        FinancialPeriod financialPeriod = getCurrentFinancialPeriod();
        String journalNumber = generateJournalNumber("ACCR");
        
        JournalEntry journalEntry = JournalEntry.builder()
                .journalNumber(journalNumber)
                .entryDate(accrualDate)
                .description(String.format("Interest Accrual - %s for %s", 
                        loan.getLoanAccountNumber(), loan.getBorrower().getFullName()))
                .journalType("ACCRUAL")
                .status("POSTED")
                .referenceNumber(loan.getLoanAccountNumber())
                .financialPeriod(financialPeriod)
                .createdBy(currentUser.getId())
                .postedAt(LocalDateTime.now())
                .postedBy(currentUser.getId())
                .build();
        
        List<JournalEntryLine> lines = new ArrayList<>();
        
        // DR: Interest Receivable
        Account interestReceivable = accountRepository.findByCode(ACCOUNT_INTEREST_RECEIVABLE)
                .orElseThrow(() -> new RuntimeException("Account not found: " + ACCOUNT_INTEREST_RECEIVABLE));
        
        JournalEntryLine drLine = JournalEntryLine.builder()
                .journalEntry(journalEntry)
                .account(interestReceivable)
                .accountCode(interestReceivable.getCode())
                .accountName(interestReceivable.getName())
                .debitCredit("DEBIT")
                .amount(accruedInterest)
                .description("Accrued interest")
                .build();
        lines.add(drLine);
        
        // CR: Interest Income
        Account interestIncome = accountRepository.findByCode(ACCOUNT_INTEREST_INCOME)
                .orElseThrow(() -> new RuntimeException("Account not found: " + ACCOUNT_INTEREST_INCOME));
        
        JournalEntryLine crLine = JournalEntryLine.builder()
                .journalEntry(journalEntry)
                .account(interestIncome)
                .accountCode(interestIncome.getCode())
                .accountName(interestIncome.getName())
                .debitCredit("CREDIT")
                .amount(accruedInterest)
                .description("Accrued interest income")
                .build();
        lines.add(crLine);
        
        journalEntry.setLines(lines);
        journalEntryRepository.save(journalEntry);
        
        // Post to General Ledger
        postToGeneralLedger(journalEntry, loan.getId(), "INTEREST_ACCRUAL", null);
    }


    /**
     * Calculate accrued interest for a loan since last accrual or last payment
     */
    private BigDecimal calculateAccruedInterest(Loan loan, LocalDate asOfDate) {
        // 1. Get principal outstanding
        BigDecimal principalOutstanding = loan.getPrincipalOutstanding();

        if (principalOutstanding == null || principalOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("No principal outstanding for loan: {}", loan.getLoanAccountNumber());
            return BigDecimal.ZERO;
        }

        // 2. Check if loan is active/written off/closed
        if (loan.getStatus() == GeneralConfig.LoanStatus.WRITTEN_OFF ||
                loan.getStatus() == GeneralConfig.LoanStatus.CLOSED) {
            log.debug("Loan is {} - no accrual: {}", loan.getStatus(), loan.getLoanAccountNumber());
            return BigDecimal.ZERO;
        }

        // 3. Determine the start date for accrual calculation
        LocalDate startDate = determineAccrualStartDate(loan);

        if (startDate == null) {
            log.warn("No start date found for accrual on loan: {}", loan.getLoanAccountNumber());
            return BigDecimal.ZERO;
        }

        // 4. Calculate days between start date and asOfDate
        LocalDate effectiveAsOfDate = asOfDate != null ? asOfDate : LocalDate.now();

        if (startDate.isAfter(effectiveAsOfDate)) {
            return BigDecimal.ZERO;
        }

        long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(startDate, effectiveAsOfDate);

        // 5. Get latest repayment date to adjust accrual after payments
        LocalDate lastRepaymentDate = getLastRepaymentDate(loan.getId());
        if (lastRepaymentDate != null && lastRepaymentDate.isAfter(startDate)) {
            daysDifference = java.time.temporal.ChronoUnit.DAYS.between(lastRepaymentDate, effectiveAsOfDate);
        }

        if (daysDifference <= 0) {
            return BigDecimal.ZERO;
        }

        // 6. Get the applicable interest rate (handle rate changes)
        BigDecimal applicableRate = getApplicableInterestRate(loan, effectiveAsOfDate);

        // 7. Calculate accrued interest using Actual/365 day count convention
        // Formula: Principal × Rate × (Days / 365)
        BigDecimal accruedInterest = principalOutstanding
                .multiply(applicableRate)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP) // Convert percentage to decimal
                .multiply(BigDecimal.valueOf(daysDifference))
                .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);

        // 8. Cap accrual to total interest due (prevent over-accrual)
        BigDecimal remainingInterest = loan.getTotalInterestDue() != null
                ? loan.getTotalInterestDue().subtract(loan.getInterestPaid() != null ? loan.getInterestPaid() : BigDecimal.ZERO)
                : accruedInterest;

        if (accruedInterest.compareTo(remainingInterest) > 0 && remainingInterest.compareTo(BigDecimal.ZERO) > 0) {
            accruedInterest = remainingInterest;
        }

        // 9. Ensure accrual is not negative
        accruedInterest = accruedInterest.max(BigDecimal.ZERO);

        log.info("Accrued interest for loan {}: Principal={}, Rate={}%, Days={}, Accrued={}",
                loan.getLoanAccountNumber(), principalOutstanding, applicableRate, daysDifference, accruedInterest);

        return accruedInterest;
    }

    /**
     * Determine the start date for accrual calculation
     * This could be:
     * - Last accrual date (if exists)
     * - Last repayment date (if exists)
     * - Disbursement date (if no previous activity)
     */
    private LocalDate determineAccrualStartDate(Loan loan) {
        // Check if we have a last accrual date from the loan entity
        if (loan.getLastAccrualDate() != null) {
            return loan.getLastAccrualDate();
        }
        // Check if we have a last repayment date
        LocalDate lastRepaymentDate = getLastRepaymentDate(loan.getId());
        if (lastRepaymentDate != null) {
            return lastRepaymentDate;
        }
        // Use disbursement date as fallback
        return loan.getDisbursementDate();
    }

    /**
     * Get the last repayment date for a loan
     */
    private LocalDate getLastRepaymentDate(Long loanId) {
        // Query the most recent repayment date
        return repaymentScheduleRepository.findLastPaymentDateByLoanIdNative(loanId)
                .orElse(null);
    }

    /**
     * Get applicable interest rate (handle tiered rates or rate changes)
     */
    private BigDecimal getApplicableInterestRate(Loan loan, LocalDate asOfDate) {
        // Default to loan's interest rate
        BigDecimal applicableRate = loan.getInterestRate();
        // Check for tiered rates based on loan age or balance
        // Example: Lower rates for older loans or larger balances
        if (loan.getPrincipalOutstanding().compareTo(BigDecimal.valueOf(100000)) > 0) {
            // Could have a different rate for large loans
            // applicableRate = loan.getInterestRate().subtract(BigDecimal.valueOf(2));
        }
        // Check for promotional rates based on date
        // if (asOfDate.isBefore(somePromotionEndDate)) {
        //     applicableRate = promotionalRate;
        // }
        return applicableRate;
    }

    /**
     * Post journal entry to general ledger
     */
    private void postToGeneralLedger(JournalEntry journalEntry, Long referenceId, 
                                      String referenceType, String referenceNumber) {
        for (JournalEntryLine line : journalEntry.getLines()) {
            GeneralLedger gl = GeneralLedger.builder()
                    .journalId(journalEntry.getId())
                    .transactionDate(journalEntry.getEntryDate())
                    .account(line.getAccount())
                    .accountCode(line.getAccountCode())
                    .accountName(line.getAccountName())
                    .debitCredit(line.getDebitCredit())
                    .amount(line.getAmount())
                    .description(line.getDescription())
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .referenceNumber(referenceNumber)
                    .financialPeriod(journalEntry.getFinancialPeriod())
                    .build();
            
            generalLedgerRepository.save(gl);
        }
    }

    /**
     * Get current financial period
     */
    private FinancialPeriod getCurrentFinancialPeriod() {
        return financialPeriodRepository.findCurrentOpenPeriod(LocalDate.now())
                .orElseThrow(() -> new RuntimeException("No open financial period found"));
    }

    /**
     * Generate journal number
     */
    private String generateJournalNumber(String prefix) {
        return prefix + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }
}