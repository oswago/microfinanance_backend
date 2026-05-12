// service/FinancialReportService.java
package com.microfinance.financials.financialreports.service;

import com.microfinance.base.entity.User;
import com.microfinance.financials.chartofaccounts.entity.Account;
import com.microfinance.financials.chartofaccounts.enums.AccountType;
import com.microfinance.financials.chartofaccounts.repository.AccountRepository;
import com.microfinance.financials.generalledger.entity.GeneralLedger;
import com.microfinance.financials.generalledger.repository.FinancialPeriodRepository;
import com.microfinance.financials.generalledger.repository.GeneralLedgerRepository;
import com.microfinance.financials.financialreports.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialReportService {

    private final GeneralLedgerRepository generalLedgerRepository;
    private final AccountRepository accountRepository;
    private final FinancialPeriodRepository financialPeriodRepository;

    private static final String COMPANY_NAME = "Finite Solutions Ltd";
    private static final String CURRENCY = "KES";

    // Income Statement Generation
    @Transactional(readOnly = true)
    public IncomeStatementDTO generateIncomeStatement(IncomeStatementRequest request, User currentUser) {
        log.info("User {} generating Income Statement from {} to {}", 
                currentUser.getUsername(), request.getStartDate(), request.getEndDate());

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        // If financial period ID is provided, use its dates
        if (request.getFinancialPeriodId() != null) {
            var period = financialPeriodRepository.findById(request.getFinancialPeriodId()).orElse(null);
            if (period != null) {
                startDate = period.getStartDate();
                endDate = period.getEndDate();
            }
        }

        // Get all posted journal entries in the date range
        List<GeneralLedger> ledgerEntries = getLedgerEntriesForDateRange(startDate, endDate);

        // Separate revenue and expense accounts
        List<GeneralLedger> revenueEntries = ledgerEntries.stream()
                .filter(entry -> entry.getAccount().getAccountType() == AccountType.INCOME)
                .collect(Collectors.toList());

        List<GeneralLedger> expenseEntries = ledgerEntries.stream()
                .filter(entry -> entry.getAccount().getAccountType() == AccountType.EXPENSE)
                .collect(Collectors.toList());

        // Calculate revenues
        RevenueSection revenueSection = calculateRevenues(revenueEntries);
        
        // Calculate expenses
        ExpenseSection expenseSection = calculateExpenses(expenseEntries);
        
        // Calculate other income and expenses
        OtherIncomeSection otherIncomeSection = calculateOtherIncome(ledgerEntries);
        OtherExpenseSection otherExpenseSection = calculateOtherExpenses(ledgerEntries);
        
        // Calculate financial summary
        FinancialSummary summary = calculateFinancialSummary(
            revenueSection.getTotalRevenue(),
            expenseSection.getTotalExpenses(),
            otherIncomeSection.getTotalOtherIncome(),
            otherExpenseSection.getTotalOtherExpense()
        );
        
        // Build header
        ReportHeader header = buildReportHeader("Income Statement", startDate, endDate);
        
        return IncomeStatementDTO.builder()
                .header(header)
                .revenues(revenueSection)
                .expenses(expenseSection)
                .otherIncome(otherIncomeSection)
                .otherExpenses(otherExpenseSection)
                .financialSummary(summary)
                .reportDate(LocalDate.now())
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    // Balance Sheet Generation
    @Transactional(readOnly = true)
    public BalanceSheetDTO generateBalanceSheet(BalanceSheetRequest request, User currentUser) {
        log.info("User {} generating Balance Sheet as of {}", currentUser.getUsername(), request.getAsOfDate());

        LocalDate asOfDate = request.getAsOfDate();
        
        // If financial period ID is provided, use its end date
        if (request.getFinancialPeriodId() != null) {
            var period = financialPeriodRepository.findById(request.getFinancialPeriodId()).orElse(null);
            if (period != null) {
                asOfDate = period.getEndDate();
            }
        }
        
        // Get all active accounts
        List<Account> allAccounts = accountRepository.findByIsActiveTrueOrderByCodeAsc();
        
        // Get ledger entries up to asOfDate
        List<GeneralLedger> ledgerEntries = getLedgerEntriesUpToDate(asOfDate);
        
        // Calculate balances for each account
        Map<Long, BigDecimal> accountBalances = calculateAccountBalances(ledgerEntries, asOfDate);
        
        // Build asset section
        AssetSection assetSection = buildAssetSection(allAccounts, accountBalances);
        
        // Build liability section
        LiabilitySection liabilitySection = buildLiabilitySection(allAccounts, accountBalances);
        
        // Build equity section
        EquitySection equitySection = buildEquitySection(allAccounts, accountBalances);
        
        // Build header
        ReportHeader header = buildReportHeader("Balance Sheet", asOfDate);
        
        return BalanceSheetDTO.builder()
                .header(header)
                .assets(assetSection)
                .liabilities(liabilitySection)
                .equity(equitySection)
                .asOfDate(asOfDate)
                .build();
    }

    // Cash Flow Statement Generation
    @Transactional(readOnly = true)
    public CashFlowStatementDTO generateCashFlowStatement(CashFlowRequest request, User currentUser) {
        log.info("User {} generating Cash Flow Statement from {} to {} using {} method", 
                currentUser.getUsername(), request.getStartDate(), request.getEndDate(), request.getMethod());

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        
        // Get all ledger entries for the period
        List<GeneralLedger> ledgerEntries = getLedgerEntriesForDateRange(startDate, endDate);
        
        // Calculate beginning cash balance
        BigDecimal beginningCashBalance = getCashBalanceAsOfDate(startDate.minusDays(1));
        
        // Calculate operating cash flow based on method
        OperatingCashFlow operatingCashFlow;
        if ("DIRECT".equalsIgnoreCase(request.getMethod())) {
            operatingCashFlow = calculateOperatingCashFlowDirect(ledgerEntries);
        } else {
            operatingCashFlow = calculateOperatingCashFlowIndirect(ledgerEntries, startDate, endDate,currentUser);
        }
        
        // Calculate investing cash flow
        InvestingCashFlow investingCashFlow = calculateInvestingCashFlow(ledgerEntries);
        
        // Calculate financing cash flow
        FinancingCashFlow financingCashFlow = calculateFinancingCashFlow(ledgerEntries);
        
        // Calculate summary
        BigDecimal netCashIncrease = operatingCashFlow.getNetCashProvided()
                .add(investingCashFlow.getNetCashUsed())
                .add(financingCashFlow.getNetCashProvided());
        
        BigDecimal endingCashBalance = beginningCashBalance.add(netCashIncrease);
        
        CashFlowSummary summary = CashFlowSummary.builder()
                .netCashIncrease(netCashIncrease)
                .beginningCashBalance(beginningCashBalance)
                .endingCashBalance(endingCashBalance)
                .build();
        
        // Build header
        ReportHeader header = buildReportHeader("Cash Flow Statement", startDate, endDate);
        
        return CashFlowStatementDTO.builder()
                .header(header)
                .operatingCashFlow(operatingCashFlow)
                .investingCashFlow(investingCashFlow)
                .financingCashFlow(financingCashFlow)
                .summary(summary)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    // General Ledger Report Generation
    @Transactional(readOnly = true)
    public GeneralLedgerReportDTO generateGeneralLedgerReport(GeneralLedgerRequest request, User currentUser) {
        log.info("User {} generating General Ledger Report from {} to {}", 
                currentUser.getUsername(), request.getStartDate(), request.getEndDate());

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        
        // Get ledger entries
        List<GeneralLedger> ledgerEntries = getLedgerEntriesForDateRange(startDate, endDate);
        
        // Apply filters
        if (request.getAccountId() != null) {
            ledgerEntries = ledgerEntries.stream()
                    .filter(gl -> gl.getAccount().getId().equals(request.getAccountId()))
                    .collect(Collectors.toList());
        }
        
        if (request.getAccountCode() != null && !request.getAccountCode().isEmpty()) {
            ledgerEntries = ledgerEntries.stream()
                    .filter(gl -> gl.getAccountCode().equals(request.getAccountCode()))
                    .collect(Collectors.toList());
        }
        
        if (request.getJournalNumber() != null && !request.getJournalNumber().isEmpty()) {
            ledgerEntries = ledgerEntries.stream()
                    .filter(gl -> gl.getJournalId() != null)
                    .collect(Collectors.toList());
        }
        
        // Build ledger entries
        List<LedgerEntry> entries = ledgerEntries.stream()
                .map(this::convertToLedgerEntry)
                .sorted(Comparator.comparing(LedgerEntry::getTransactionDate))
                .collect(Collectors.toList());
        
        // Calculate account summaries
        Map<String, AccountSummary> accountSummaries = calculateAccountSummaries(ledgerEntries);
        
        // Calculate totals
        BigDecimal totalDebit = ledgerEntries.stream()
                .filter(gl -> gl.getDebitCredit().equals("DEBIT"))
                .map(GeneralLedger::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredit = ledgerEntries.stream()
                .filter(gl -> gl.getDebitCredit().equals("CREDIT"))
                .map(GeneralLedger::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Build header
        ReportHeader header = buildReportHeader("General Ledger Report", startDate, endDate);
        
        return GeneralLedgerReportDTO.builder()
                .header(header)
                .entries(entries)
                .accountSummaries(accountSummaries)
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    // Trial Balance Generation
    @Transactional(readOnly = true)
    public TrialBalanceDTO generateTrialBalance(TrialBalanceRequest request, User currentUser) {
        log.info("User {} generating Trial Balance as of {}", currentUser.getUsername(), request.getAsOfDate());

        LocalDate asOfDate = request.getAsOfDate();
        
        // If financial period ID is provided, use its end date
        if (request.getFinancialPeriodId() != null) {
            var period = financialPeriodRepository.findById(request.getFinancialPeriodId()).orElse(null);
            if (period != null) {
                asOfDate = period.getEndDate();
            }
        }
        
        // Get all active accounts
        List<Account> allAccounts = accountRepository.findByIsActiveTrueOrderByCodeAsc();
        
        // Get ledger entries up to asOfDate
        List<GeneralLedger> ledgerEntries = getLedgerEntriesUpToDate(asOfDate);
        
        // Calculate balances for each account
        Map<Long, BigDecimal> accountBalances = calculateAccountBalances(ledgerEntries, asOfDate);
        
        List<TrialBalanceEntry> entries = new ArrayList<>();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        
        for (Account account : allAccounts) {
            BigDecimal balance = accountBalances.getOrDefault(account.getId(), BigDecimal.ZERO);
            
            if (balance.compareTo(BigDecimal.ZERO) != 0) {
                TrialBalanceEntry entry = TrialBalanceEntry.builder()
                        .accountCode(account.getCode())
                        .accountName(account.getName())
                        .debit(account.getNormalBalance().toString().equals("DEBIT") ? balance : BigDecimal.ZERO)
                        .credit(account.getNormalBalance().toString().equals("CREDIT") ? balance : BigDecimal.ZERO)
                        .balance(balance)
                        .build();
                
                entries.add(entry);
                
                if (account.getNormalBalance().toString().equals("DEBIT")) {
                    totalDebits = totalDebits.add(balance);
                } else {
                    totalCredits = totalCredits.add(balance);
                }
            }
        }
        
        // Add total row
        entries.add(TrialBalanceEntry.builder()
                .accountCode("TOTAL")
                .accountName("TOTAL")
                .debit(totalDebits)
                .credit(totalCredits)
                .balance(totalDebits.subtract(totalCredits))
                .isTotal(true)
                .build());
        
        ReportHeader header = buildReportHeader("Trial Balance", asOfDate);
        
        return TrialBalanceDTO.builder()
                .header(header)
                .entries(entries)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .isBalanced(totalDebits.equals(totalCredits))
                .asOfDate(asOfDate)
                .build();
    }

    // Export Methods (Placeholders for actual implementation)
    public byte[] exportReportAsPdf(String reportType, LocalDate startDate, LocalDate endDate, User currentUser) {
        log.info("User {} exporting {} as PDF", currentUser.getUsername(), reportType);
        // TODO: Implement PDF export using JasperReports or iText
        // This is a placeholder - return empty byte array
        return new byte[0];
    }

    public byte[] exportReportAsExcel(String reportType, LocalDate startDate, LocalDate endDate, User currentUser) {
        log.info("User {} exporting {} as Excel", currentUser.getUsername(), reportType);
        // TODO: Implement Excel export using Apache POI
        // This is a placeholder - return empty byte array
        return new byte[0];
    }

    // ==================== Helper Methods ====================

    private List<GeneralLedger> getLedgerEntriesForDateRange(LocalDate startDate, LocalDate endDate) {
        return generalLedgerRepository.findByTransactionDateBetween(startDate, endDate, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
    }

    private List<GeneralLedger> getLedgerEntriesUpToDate(LocalDate date) {
        return generalLedgerRepository.findByTransactionDateBetween(date.minusYears(10), date, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
    }

    private Map<Long, BigDecimal> calculateAccountBalances(List<GeneralLedger> entries, LocalDate asOfDate) {
        Map<Long, BigDecimal> balances = new HashMap<>();
        
        for (GeneralLedger entry : entries) {
            Long accountId = entry.getAccount().getId();
            BigDecimal currentBalance = balances.getOrDefault(accountId, BigDecimal.ZERO);
            
            if (entry.getDebitCredit().equals("DEBIT")) {
                balances.put(accountId, currentBalance.add(entry.getAmount()));
            } else {
                balances.put(accountId, currentBalance.subtract(entry.getAmount()));
            }
        }
        
        return balances;
    }

    private RevenueSection calculateRevenues(List<GeneralLedger> revenueEntries) {
        Map<String, List<GeneralLedger>> revenueByAccount = revenueEntries.stream()
                .collect(Collectors.groupingBy(e -> e.getAccount().getCode()));

        List<AccountBalance> operatingRevenues = new ArrayList<>();
        List<AccountBalance> otherRevenues = new ArrayList<>();
        BigDecimal totalOperatingRevenue = BigDecimal.ZERO;
        BigDecimal totalOtherRevenue = BigDecimal.ZERO;

        for (Map.Entry<String, List<GeneralLedger>> entry : revenueByAccount.entrySet()) {
            Account account = entry.getValue().get(0).getAccount();
            BigDecimal total = entry.getValue().stream()
                    .map(gl -> gl.getDebitCredit().equals("CREDIT") ? gl.getAmount() : gl.getAmount().negate())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            AccountBalance balance = AccountBalance.builder()
                    .accountCode(account.getCode())
                    .accountName(account.getName())
                    .balance(total)
                    .normalBalance(account.getNormalBalance().toString())
                    .accountType(account.getAccountType().toString())
                    .build();

            if (isOperatingRevenue(account)) {
                operatingRevenues.add(balance);
                totalOperatingRevenue = totalOperatingRevenue.add(total);
            } else {
                otherRevenues.add(balance);
                totalOtherRevenue = totalOtherRevenue.add(total);
            }
        }

        return RevenueSection.builder()
                .operatingRevenues(operatingRevenues)
                .totalOperatingRevenue(totalOperatingRevenue)
                .otherRevenues(otherRevenues)
                .totalOtherRevenue(totalOtherRevenue)
                .totalRevenue(totalOperatingRevenue.add(totalOtherRevenue))
                .build();
    }

    private ExpenseSection calculateExpenses(List<GeneralLedger> expenseEntries) {
        Map<String, List<GeneralLedger>> expenseByAccount = expenseEntries.stream()
                .collect(Collectors.groupingBy(e -> e.getAccount().getCode()));

        List<AccountBalance> operatingExpenses = new ArrayList<>();
        List<AccountBalance> administrativeExpenses = new ArrayList<>();
        List<AccountBalance> sellingExpenses = new ArrayList<>();
        
        BigDecimal totalOperatingExpense = BigDecimal.ZERO;
        BigDecimal totalAdministrativeExpense = BigDecimal.ZERO;
        BigDecimal totalSellingExpense = BigDecimal.ZERO;

        for (Map.Entry<String, List<GeneralLedger>> entry : expenseByAccount.entrySet()) {
            Account account = entry.getValue().get(0).getAccount();
            BigDecimal total = entry.getValue().stream()
                    .map(gl -> gl.getDebitCredit().equals("DEBIT") ? gl.getAmount() : gl.getAmount().negate())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            AccountBalance balance = AccountBalance.builder()
                    .accountCode(account.getCode())
                    .accountName(account.getName())
                    .balance(total)
                    .normalBalance(account.getNormalBalance().toString())
                    .accountType(account.getAccountType().toString())
                    .build();

            if (isOperatingExpense(account)) {
                operatingExpenses.add(balance);
                totalOperatingExpense = totalOperatingExpense.add(total);
            } else if (isAdministrativeExpense(account)) {
                administrativeExpenses.add(balance);
                totalAdministrativeExpense = totalAdministrativeExpense.add(total);
            } else if (isSellingExpense(account)) {
                sellingExpenses.add(balance);
                totalSellingExpense = totalSellingExpense.add(total);
            }
        }

        BigDecimal totalExpenses = totalOperatingExpense
                .add(totalAdministrativeExpense)
                .add(totalSellingExpense);

        return ExpenseSection.builder()
                .operatingExpenses(operatingExpenses)
                .totalOperatingExpense(totalOperatingExpense)
                .administrativeExpenses(administrativeExpenses)
                .totalAdministrativeExpense(totalAdministrativeExpense)
                .sellingExpenses(sellingExpenses)
                .totalSellingExpense(totalSellingExpense)
                .totalExpenses(totalExpenses)
                .build();
    }

    private OtherIncomeSection calculateOtherIncome(List<GeneralLedger> ledgerEntries) {
        return OtherIncomeSection.builder()
                .otherIncomes(new ArrayList<>())
                .totalOtherIncome(BigDecimal.ZERO)
                .build();
    }

    private OtherExpenseSection calculateOtherExpenses(List<GeneralLedger> ledgerEntries) {
        return OtherExpenseSection.builder()
                .otherExpenses(new ArrayList<>())
                .totalOtherExpense(BigDecimal.ZERO)
                .build();
    }

    private FinancialSummary calculateFinancialSummary(
            BigDecimal totalRevenue,
            BigDecimal totalExpenses,
            BigDecimal otherIncome,
            BigDecimal otherExpenses) {
        
        BigDecimal grossProfit = totalRevenue.subtract(totalExpenses);
        BigDecimal operatingIncome = grossProfit.add(otherIncome);
        BigDecimal netIncomeBeforeTax = operatingIncome.subtract(otherExpenses);
        BigDecimal taxExpense = calculateTaxExpense(netIncomeBeforeTax);
        BigDecimal netIncomeAfterTax = netIncomeBeforeTax.subtract(taxExpense);

        return FinancialSummary.builder()
                .grossProfit(grossProfit)
                .operatingIncome(operatingIncome)
                .netIncomeBeforeTax(netIncomeBeforeTax)
                .taxExpense(taxExpense)
                .netIncomeAfterTax(netIncomeAfterTax)
                .build();
    }

    private AssetSection buildAssetSection(List<Account> accounts, Map<Long, BigDecimal> balances) {
        List<AccountBalance> currentAssets = new ArrayList<>();
        List<AccountBalance> fixedAssets = new ArrayList<>();
        List<AccountBalance> otherAssets = new ArrayList<>();
        
        BigDecimal totalCurrentAssets = BigDecimal.ZERO;
        BigDecimal totalFixedAssets = BigDecimal.ZERO;
        BigDecimal totalOtherAssets = BigDecimal.ZERO;

        for (Account account : accounts) {
            if (account.getAccountType() == AccountType.ASSET) {
                BigDecimal balance = balances.getOrDefault(account.getId(), BigDecimal.ZERO);
                AccountBalance accountBalance = AccountBalance.builder()
                        .accountCode(account.getCode())
                        .accountName(account.getName())
                        .balance(balance)
                        .build();

                if (isCurrentAsset(account)) {
                    currentAssets.add(accountBalance);
                    totalCurrentAssets = totalCurrentAssets.add(balance);
                } else if (isFixedAsset(account)) {
                    fixedAssets.add(accountBalance);
                    totalFixedAssets = totalFixedAssets.add(balance);
                } else {
                    otherAssets.add(accountBalance);
                    totalOtherAssets = totalOtherAssets.add(balance);
                }
            }
        }

        BigDecimal totalAssets = totalCurrentAssets.add(totalFixedAssets).add(totalOtherAssets);

        return AssetSection.builder()
                .currentAssets(currentAssets)
                .totalCurrentAssets(totalCurrentAssets)
                .fixedAssets(fixedAssets)
                .totalFixedAssets(totalFixedAssets)
                .otherAssets(otherAssets)
                .totalOtherAssets(totalOtherAssets)
                .totalAssets(totalAssets)
                .build();
    }

    private LiabilitySection buildLiabilitySection(List<Account> accounts, Map<Long, BigDecimal> balances) {
        List<AccountBalance> currentLiabilities = new ArrayList<>();
        List<AccountBalance> longTermLiabilities = new ArrayList<>();
        
        BigDecimal totalCurrentLiabilities = BigDecimal.ZERO;
        BigDecimal totalLongTermLiabilities = BigDecimal.ZERO;

        for (Account account : accounts) {
            if (account.getAccountType() == AccountType.LIABILITY) {
                BigDecimal balance = balances.getOrDefault(account.getId(), BigDecimal.ZERO);
                AccountBalance accountBalance = AccountBalance.builder()
                        .accountCode(account.getCode())
                        .accountName(account.getName())
                        .balance(balance)
                        .build();

                if (isCurrentLiability(account)) {
                    currentLiabilities.add(accountBalance);
                    totalCurrentLiabilities = totalCurrentLiabilities.add(balance);
                } else {
                    longTermLiabilities.add(accountBalance);
                    totalLongTermLiabilities = totalLongTermLiabilities.add(balance);
                }
            }
        }

        BigDecimal totalLiabilities = totalCurrentLiabilities.add(totalLongTermLiabilities);

        return LiabilitySection.builder()
                .currentLiabilities(currentLiabilities)
                .totalCurrentLiabilities(totalCurrentLiabilities)
                .longTermLiabilities(longTermLiabilities)
                .totalLongTermLiabilities(totalLongTermLiabilities)
                .totalLiabilities(totalLiabilities)
                .build();
    }

    private EquitySection buildEquitySection(List<Account> accounts, Map<Long, BigDecimal> balances) {
        List<AccountBalance> equityAccounts = new ArrayList<>();
        BigDecimal totalEquity = BigDecimal.ZERO;

        for (Account account : accounts) {
            if (account.getAccountType() == AccountType.EQUITY) {
                BigDecimal balance = balances.getOrDefault(account.getId(), BigDecimal.ZERO);
                equityAccounts.add(AccountBalance.builder()
                        .accountCode(account.getCode())
                        .accountName(account.getName())
                        .balance(balance)
                        .build());
                totalEquity = totalEquity.add(balance);
            }
        }

        return EquitySection.builder()
                .equityAccounts(equityAccounts)
                .totalEquity(totalEquity)
                .build();
    }

    private OperatingCashFlow calculateOperatingCashFlowIndirect(List<GeneralLedger> entries, LocalDate startDate, LocalDate endDate,User currentUser) {
        // Net Income from income statement
        IncomeStatementRequest request = new IncomeStatementRequest();
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        BigDecimal netIncome = generateIncomeStatement(request, currentUser).getFinancialSummary().getNetIncomeAfterTax();
        
        // Adjustments for non-cash items
        List<CashFlowAdjustment> adjustments = new ArrayList<>();
        BigDecimal depreciationExpense = getDepreciationExpense(entries);
        adjustments.add(CashFlowAdjustment.builder()
                .description("Depreciation Expense")
                .amount(depreciationExpense)
                .type("ADDITION")
                .build());
        
        // Changes in working capital
        List<CashFlowItem> workingCapitalChanges = calculateWorkingCapitalChanges(startDate, endDate);
        
        BigDecimal netCashFromOperations = netIncome.add(depreciationExpense);
        BigDecimal netCashProvided = netCashFromOperations;
        
        for (CashFlowItem item : workingCapitalChanges) {
            netCashProvided = netCashProvided.add(item.getAmount());
        }
        
        return OperatingCashFlow.builder()
                .netIncome(netIncome)
                .adjustments(adjustments)
                .netCashFromOperations(netCashFromOperations)
                .changesInWorkingCapital(workingCapitalChanges)
                .netCashProvided(netCashProvided)
                .build();
    }

    private OperatingCashFlow calculateOperatingCashFlowDirect(List<GeneralLedger> entries) {
        // Direct method implementation
        List<CashFlowItem> cashReceipts = new ArrayList<>();
        List<CashFlowItem> cashPayments = new ArrayList<>();
        BigDecimal netCashProvided = BigDecimal.ZERO;
        
        // Group by transaction type for direct method
        Map<String, List<GeneralLedger>> cashTransactions = entries.stream()
                .filter(gl -> gl.getAccount().getCode().startsWith("101")) // Cash accounts
                .collect(Collectors.groupingBy(gl -> gl.getReferenceType() != null ? gl.getReferenceType() : "OTHER"));
        
        for (Map.Entry<String, List<GeneralLedger>> entry : cashTransactions.entrySet()) {
            BigDecimal total = entry.getValue().stream()
                    .map(gl -> gl.getDebitCredit().equals("DEBIT") ? gl.getAmount() : gl.getAmount().negate())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            CashFlowItem item = CashFlowItem.builder()
                    .description(getCashFlowDescription(entry.getKey()))
                    .amount(total)
                    .build();
            
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                cashReceipts.add(item);
            } else {
                cashPayments.add(item);
                total = total.abs();
            }
            netCashProvided = netCashProvided.add(total);
        }
        
        return OperatingCashFlow.builder()
                .netIncome(BigDecimal.ZERO)
                .adjustments(new ArrayList<>())
                .netCashFromOperations(netCashProvided)
                .changesInWorkingCapital(new ArrayList<>())
                .netCashProvided(netCashProvided)
                .build();
    }

    private InvestingCashFlow calculateInvestingCashFlow(List<GeneralLedger> entries) {
        List<CashFlowItem> investingActivities = new ArrayList<>();
        BigDecimal netCashUsed = BigDecimal.ZERO;
        
        // Get all asset purchase and sale transactions
        Map<String, List<GeneralLedger>> assetTransactions = entries.stream()
                .filter(gl -> gl.getAccount().getAccountType() == AccountType.ASSET)
                .filter(gl -> gl.getReferenceType() != null && 
                        (gl.getReferenceType().equals("ASSET_PURCHASE") || 
                         gl.getReferenceType().equals("ASSET_SALE")))
                .collect(Collectors.groupingBy(gl -> gl.getReferenceType()));
        
        for (Map.Entry<String, List<GeneralLedger>> entry : assetTransactions.entrySet()) {
            BigDecimal total = entry.getValue().stream()
                    .map(gl -> gl.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            CashFlowItem item = CashFlowItem.builder()
                    .description(entry.getKey().equals("ASSET_PURCHASE") ? "Purchase of Fixed Assets" : "Sale of Fixed Assets")
                    .amount(entry.getKey().equals("ASSET_PURCHASE") ? total.negate() : total)
                    .build();
            
            investingActivities.add(item);
            netCashUsed = netCashUsed.add(item.getAmount());
        }
        
        return InvestingCashFlow.builder()
                .investingActivities(investingActivities)
                .netCashUsed(netCashUsed)
                .build();
    }

    private FinancingCashFlow calculateFinancingCashFlow(List<GeneralLedger> entries) {
        List<CashFlowItem> financingActivities = new ArrayList<>();
        BigDecimal netCashProvided = BigDecimal.ZERO;
        
        // Get loan disbursements and repayments
        Map<String, List<GeneralLedger>> loanTransactions = entries.stream()
                .filter(gl -> gl.getReferenceType() != null && 
                        (gl.getReferenceType().equals("LOAN_DISBURSEMENT") || 
                         gl.getReferenceType().equals("LOAN_REPAYMENT") ||
                         gl.getReferenceType().equals("CAPITAL_CONTRIBUTION") ||
                         gl.getReferenceType().equals("DIVIDEND_PAYMENT")))
                .collect(Collectors.groupingBy(gl -> gl.getReferenceType()));
        
        for (Map.Entry<String, List<GeneralLedger>> entry : loanTransactions.entrySet()) {
            BigDecimal total = entry.getValue().stream()
                    .map(gl -> gl.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            String description;
            BigDecimal amount;
            
            switch (entry.getKey()) {
                case "LOAN_DISBURSEMENT":
                    description = "Proceeds from Loans";
                    amount = total;
                    break;
                case "LOAN_REPAYMENT":
                    description = "Loan Repayments";
                    amount = total.negate();
                    break;
                case "CAPITAL_CONTRIBUTION":
                    description = "Capital Contributions";
                    amount = total;
                    break;
                case "DIVIDEND_PAYMENT":
                    description = "Dividends Paid";
                    amount = total.negate();
                    break;
                default:
                    description = entry.getKey();
                    amount = total;
            }
            
            CashFlowItem item = CashFlowItem.builder()
                    .description(description)
                    .amount(amount)
                    .build();
            
            financingActivities.add(item);
            netCashProvided = netCashProvided.add(amount);
        }
        
        return FinancingCashFlow.builder()
                .financingActivities(financingActivities)
                .netCashProvided(netCashProvided)
                .build();
    }

    private String getCashFlowDescription(String referenceType) {
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("LOAN_DISBURSEMENT", "Cash received from loans");
        descriptions.put("LOAN_REPAYMENT", "Cash paid for loan repayments");
        descriptions.put("INTEREST_INCOME", "Interest received");
        descriptions.put("INTEREST_EXPENSE", "Interest paid");
        descriptions.put("FEE_INCOME", "Fees received");
        descriptions.put("OPERATING_EXPENSE", "Operating expenses paid");
        descriptions.put("SALARY_EXPENSE", "Salaries paid");
        return descriptions.getOrDefault(referenceType, "Other cash flow");
    }

    private BigDecimal getCashBalanceAsOfDate(LocalDate date) {
        List<GeneralLedger> entries = getLedgerEntriesUpToDate(date);
        Map<Long, BigDecimal> balances = calculateAccountBalances(entries, date);
        
        // Find cash account (assuming code starts with "101" for cash)
        Optional<Account> cashAccount = accountRepository.findByCode("1010");
        if (cashAccount.isPresent()) {
            return balances.getOrDefault(cashAccount.get().getId(), BigDecimal.ZERO);
        }
        
        return BigDecimal.ZERO;
    }

    private BigDecimal getDepreciationExpense(List<GeneralLedger> entries) {
        return entries.stream()
                .filter(gl -> gl.getAccount().getName().toLowerCase().contains("depreciation"))
                .filter(gl -> gl.getDebitCredit().equals("DEBIT"))
                .map(GeneralLedger::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<CashFlowItem> calculateWorkingCapitalChanges(LocalDate startDate, LocalDate endDate) {
        List<CashFlowItem> changes = new ArrayList<>();
        
        // Get current assets and liabilities balances at start and end
        Map<String, BigDecimal> startBalances = getWorkingCapitalBalances(startDate.minusDays(1));
        Map<String, BigDecimal> endBalances = getWorkingCapitalBalances(endDate);
        
        // Calculate changes
        for (String accountType : startBalances.keySet()) {
            BigDecimal start = startBalances.getOrDefault(accountType, BigDecimal.ZERO);
            BigDecimal end = endBalances.getOrDefault(accountType, BigDecimal.ZERO);
            BigDecimal change = end.subtract(start);
            
            if (change.compareTo(BigDecimal.ZERO) != 0) {
                CashFlowItem item = CashFlowItem.builder()
                        .description("Change in " + accountType)
                        .amount(change.negate()) // Negate because increase in assets decreases cash
                        .build();
                changes.add(item);
            }
        }
        
        return changes;
    }

    private Map<String, BigDecimal> getWorkingCapitalBalances(LocalDate date) {
        Map<String, BigDecimal> balances = new HashMap<>();
        List<GeneralLedger> entries = getLedgerEntriesUpToDate(date);
        Map<Long, BigDecimal> accountBalances = calculateAccountBalances(entries, date);
        
        List<Account> workingCapitalAccounts = accountRepository.findAll().stream()
                .filter(a -> a.getAccountType() == AccountType.ASSET || a.getAccountType() == AccountType.LIABILITY)
                .collect(Collectors.toList());
        
        for (Account account : workingCapitalAccounts) {
            if (isCurrentAsset(account)) {
                balances.put("Current Assets", 
                    balances.getOrDefault("Current Assets", BigDecimal.ZERO)
                    .add(accountBalances.getOrDefault(account.getId(), BigDecimal.ZERO)));
            } else if (isCurrentLiability(account)) {
                balances.put("Current Liabilities", 
                    balances.getOrDefault("Current Liabilities", BigDecimal.ZERO)
                    .add(accountBalances.getOrDefault(account.getId(), BigDecimal.ZERO)));
            }
        }
        
        return balances;
    }

    private LedgerEntry convertToLedgerEntry(GeneralLedger gl) {
        return LedgerEntry.builder()
                .transactionDate(gl.getTransactionDate())
                .journalNumber(gl.getJournalId() != null ? "JN-" + gl.getJournalId() : null)
                .accountCode(gl.getAccountCode())
                .accountName(gl.getAccountName())
                .description(gl.getDescription())
                .debitCredit(gl.getDebitCredit())
                .amount(gl.getAmount())
                .referenceType(gl.getReferenceType())
                .referenceNumber(gl.getReferenceNumber())
                .build();
    }

    private Map<String, AccountSummary> calculateAccountSummaries(List<GeneralLedger> entries) {
        Map<String, AccountSummary> summaries = new HashMap<>();
        
        Map<String, List<GeneralLedger>> entriesByAccount = entries.stream()
                .collect(Collectors.groupingBy(GeneralLedger::getAccountCode));
        
        for (Map.Entry<String, List<GeneralLedger>> entry : entriesByAccount.entrySet()) {
            String accountCode = entry.getKey();
            List<GeneralLedger> accountEntries = entry.getValue();
            
            BigDecimal totalDebit = accountEntries.stream()
                    .filter(gl -> gl.getDebitCredit().equals("DEBIT"))
                    .map(GeneralLedger::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalCredit = accountEntries.stream()
                    .filter(gl -> gl.getDebitCredit().equals("CREDIT"))
                    .map(GeneralLedger::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            AccountSummary summary = AccountSummary.builder()
                    .accountCode(accountCode)
                    .accountName(accountEntries.get(0).getAccountName())
                    .totalDebit(totalDebit)
                    .totalCredit(totalCredit)
                    .closingBalance(totalDebit.subtract(totalCredit))
                    .build();
            
            summaries.put(accountCode, summary);
        }
        
        return summaries;
    }

    private BigDecimal calculateTaxExpense(BigDecimal netIncomeBeforeTax) {
        BigDecimal taxRate = new BigDecimal("0.30");
        return netIncomeBeforeTax.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isOperatingRevenue(Account account) {
        String code = account.getCode();
        return code.startsWith("4") && !code.startsWith("49");
    }

    private boolean isOperatingExpense(Account account) {
        String code = account.getCode();
        return code.startsWith("5") && code.startsWith("51");
    }

    private boolean isAdministrativeExpense(Account account) {
        String code = account.getCode();
        return code.startsWith("52");
    }

    private boolean isSellingExpense(Account account) {
        String code = account.getCode();
        return code.startsWith("53");
    }

    private boolean isCurrentAsset(Account account) {
        String code = account.getCode();
        return code.startsWith("11") || code.startsWith("12");
    }

    private boolean isFixedAsset(Account account) {
        String code = account.getCode();
        return code.startsWith("13") || code.startsWith("14") || code.startsWith("15");
    }

    private boolean isCurrentLiability(Account account) {
        String code = account.getCode();
        return code.startsWith("21") || code.startsWith("22");
    }

    private ReportHeader buildReportHeader(String reportName, LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        String period = startDate.format(formatter) + " to " + endDate.format(formatter);
        
        return ReportHeader.builder()
                .companyName(COMPANY_NAME)
                .reportName(reportName)
                .reportPeriod(period)
                .generatedDate(LocalDate.now())
                .currency(CURRENCY)
                .build();
    }

    private ReportHeader buildReportHeader(String reportName, LocalDate asOfDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        String period = "As of " + asOfDate.format(formatter);
        
        return ReportHeader.builder()
                .companyName(COMPANY_NAME)
                .reportName(reportName)
                .reportPeriod(period)
                .generatedDate(LocalDate.now())
                .currency(CURRENCY)
                .build();
    }
}