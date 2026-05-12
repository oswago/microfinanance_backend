// service/BudgetService.java
package com.microfinance.financials.budget.service;

import com.microfinance.base.entity.User;
import com.microfinance.financials.budget.dto.*;
import com.microfinance.financials.budget.entity.Budget;
import com.microfinance.financials.budget.entity.BudgetActual;
import com.microfinance.financials.budget.repository.BudgetActualRepository;
import com.microfinance.financials.budget.repository.BudgetRepository;
import com.microfinance.financials.generalledger.entity.GeneralLedger;
import com.microfinance.financials.generalledger.repository.GeneralLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetActualRepository budgetActualRepository;
    private final GeneralLedgerRepository generalLedgerRepository;

    private static final String COMPANY_NAME = "Finite Solutions Ltd";
    private static final String CURRENCY = "KES";

    // Budget CRUD Operations
    @Transactional
    public BudgetDTO createBudget(BudgetDTO dto, User currentUser) {
        log.info("User {} creating budget: {}", currentUser.getUsername(), dto.getBudgetName());

        String budgetCode = generateBudgetCode(dto.getFiscalYear(), dto.getCategory());
        
        Budget budget = Budget.builder()
                .budgetCode(budgetCode)
                .budgetName(dto.getBudgetName())
                .fiscalYear(dto.getFiscalYear())
                .periodType(dto.getPeriodType())
                .category(dto.getCategory())
                .subCategory(dto.getSubCategory())
                .accountCode(dto.getAccountCode())
                .accountName(dto.getAccountName())
                .amount(dto.getAmount())
                .january(dto.getJanuary())
                .february(dto.getFebruary())
                .march(dto.getMarch())
                .april(dto.getApril())
                .may(dto.getMay())
                .june(dto.getJune())
                .july(dto.getJuly())
                .august(dto.getAugust())
                .september(dto.getSeptember())
                .october(dto.getOctober())
                .november(dto.getNovember())
                .december(dto.getDecember())
                .isActive(true)
                .notes(dto.getNotes())
                .createdBy(currentUser.getId())
                .build();

        budget = budgetRepository.save(budget);
        return convertToDTO(budget);
    }

    @Transactional
    public BudgetDTO updateBudget(Long id, BudgetDTO dto, User currentUser) {
        log.info("User {} updating budget: {}", currentUser.getUsername(), id);

        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        budget.setBudgetName(dto.getBudgetName());
        budget.setAmount(dto.getAmount());
        budget.setJanuary(dto.getJanuary());
        budget.setFebruary(dto.getFebruary());
        budget.setMarch(dto.getMarch());
        budget.setApril(dto.getApril());
        budget.setMay(dto.getMay());
        budget.setJune(dto.getJune());
        budget.setJuly(dto.getJuly());
        budget.setAugust(dto.getAugust());
        budget.setSeptember(dto.getSeptember());
        budget.setOctober(dto.getOctober());
        budget.setNovember(dto.getNovember());
        budget.setDecember(dto.getDecember());
        budget.setNotes(dto.getNotes());
        budget.setUpdatedBy(currentUser.getId());
        budget.setVersion(budget.getVersion() + 1);

        budget = budgetRepository.save(budget);
        return convertToDTO(budget);
    }

    @Transactional(readOnly = true)
    public Page<BudgetDTO> getBudgets(Integer fiscalYear, String category, Pageable pageable) {
        Page<Budget> budgets;
        if (fiscalYear != null && category != null) {
            budgets = budgetRepository.findByFiscalYearAndCategory(fiscalYear, category, pageable);
        } else if (fiscalYear != null) {
            budgets = budgetRepository.findByFiscalYear(fiscalYear, pageable);
        } else {
            budgets = budgetRepository.findAll(pageable);
        }
        return budgets.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public BudgetDTO getBudgetById(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        return convertToDTO(budget);
    }

    @Transactional(readOnly = true)
    public List<Integer> getAvailableYears() {
        return budgetRepository.findDistinctFiscalYears();
    }

    // Budget vs Actual Report
    @Transactional(readOnly = true)
    public BudgetVsActualReportDTO generateBudgetVsActualReport(Integer fiscalYear, String category, User currentUser) {
        log.info("User {} generating Budget vs Actual report for year {} and category {}",
                currentUser.getUsername(), fiscalYear, category);

        // Use the non-paginated version that returns List<Budget>
        List<Budget> budgets = budgetRepository.findByFiscalYearAndCategory(fiscalYear, category);

        if (budgets.isEmpty()) {
            throw new RuntimeException("No budgets found for the specified criteria");
        }

        List<BudgetActual> actuals = getActualsForBudgets(budgets, fiscalYear);

        // Calculate summary
        BigDecimal totalBudget = budgets.stream()
                .map(Budget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalActual = actuals.stream()
                .map(BudgetActual::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVariance = totalActual.subtract(totalBudget);
        BigDecimal variancePercentage = calculatePercentage(totalVariance, totalBudget);

        // Categorize performance
        int onTrack = 0, belowTarget = 0, aboveTarget = 0;
        List<BudgetVsActualItem> items = new ArrayList<>();
        Map<String, MonthlyComparison> monthlyComparisons = new HashMap<>();

        for (Budget budget : budgets) {
            BigDecimal actualForBudget = actuals.stream()
                    .filter(a -> a.getBudgetId().equals(budget.getId()))
                    .map(BudgetActual::getActualAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal variance = actualForBudget.subtract(budget.getAmount());
            BigDecimal variancePct = calculatePercentage(variance, budget.getAmount());

            String status = determineStatus(variancePct);
            if ("ON_TRACK".equals(status)) onTrack++;
            else if ("BELOW_TARGET".equals(status)) belowTarget++;
            else aboveTarget++;

            items.add(BudgetVsActualItem.builder()
                    .category(budget.getCategory())
                    .subCategory(budget.getSubCategory())
                    .budgetAmount(budget.getAmount())
                    .actualAmount(actualForBudget)
                    .variance(variance)
                    .variancePercentage(variancePct)
                    .status(status)
                    .build());

            // Monthly comparisons
            addMonthlyComparisons(budget, actuals, monthlyComparisons);
        }

        BudgetSummary summary = BudgetSummary.builder()
                .totalBudget(totalBudget)
                .totalActual(totalActual)
                .totalVariance(totalVariance)
                .overallVariancePercentage(variancePercentage)
                .categoriesOnTrack(onTrack)
                .categoriesBelowTarget(belowTarget)
                .categoriesAboveTarget(aboveTarget)
                .build();

        ReportHeader header = buildReportHeader("Budget vs Actual Report", fiscalYear);

        return BudgetVsActualReportDTO.builder()
                .header(header)
                .summary(summary)
                .items(items)
                .monthlyComparisons(monthlyComparisons)
                .fiscalYear(fiscalYear)
                .category(category)
                .build();
    }

    private List<BudgetActual> getActualsForBudgets(List<Budget> budgets, Integer fiscalYear) {
        List<Long> budgetIds = budgets.stream().map(Budget::getId).collect(Collectors.toList());
        LocalDate startDate = LocalDate.of(fiscalYear, 1, 1);
        LocalDate endDate = LocalDate.of(fiscalYear, 12, 31);
        
        return budgetActualRepository.findByBudgetIdsAndDateRange(budgetIds, startDate, endDate);
    }

    private void addMonthlyComparisons(Budget budget, List<BudgetActual> actuals, 
                                       Map<String, MonthlyComparison> comparisons) {
        Map<Integer, BigDecimal> monthlyBudget = getMonthlyBudgetMap(budget);
        
        for (int month = 1; month <= 12; month++) {
            String monthName = getMonthName(month);
            BigDecimal budgetAmount = monthlyBudget.getOrDefault(month, BigDecimal.ZERO);
            
            LocalDate monthStart = LocalDate.of(budget.getFiscalYear(), month, 1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            
            BigDecimal actualAmount = actuals.stream()
                    .filter(a -> a.getPeriodDate().isAfter(monthStart.minusDays(1)) && 
                                a.getPeriodDate().isBefore(monthEnd.plusDays(1)))
                    .map(BudgetActual::getActualAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal variance = actualAmount.subtract(budgetAmount);
            BigDecimal variancePct = calculatePercentage(variance, budgetAmount);
            
            MonthlyComparison comparison = comparisons.getOrDefault(monthName, 
                    MonthlyComparison.builder().month(monthName).build());
            
            comparison.setBudget((comparison.getBudget() != null ? comparison.getBudget() : BigDecimal.ZERO).add(budgetAmount));
            comparison.setActual((comparison.getActual() != null ? comparison.getActual() : BigDecimal.ZERO).add(actualAmount));
            comparison.setVariance((comparison.getVariance() != null ? comparison.getVariance() : BigDecimal.ZERO).add(variance));
            
            comparisons.put(monthName, comparison);
        }
    }

    private Map<Integer, BigDecimal> getMonthlyBudgetMap(Budget budget) {
        Map<Integer, BigDecimal> monthlyMap = new HashMap<>();
        monthlyMap.put(1, budget.getJanuary() != null ? budget.getJanuary() : BigDecimal.ZERO);
        monthlyMap.put(2, budget.getFebruary() != null ? budget.getFebruary() : BigDecimal.ZERO);
        monthlyMap.put(3, budget.getMarch() != null ? budget.getMarch() : BigDecimal.ZERO);
        monthlyMap.put(4, budget.getApril() != null ? budget.getApril() : BigDecimal.ZERO);
        monthlyMap.put(5, budget.getMay() != null ? budget.getMay() : BigDecimal.ZERO);
        monthlyMap.put(6, budget.getJune() != null ? budget.getJune() : BigDecimal.ZERO);
        monthlyMap.put(7, budget.getJuly() != null ? budget.getJuly() : BigDecimal.ZERO);
        monthlyMap.put(8, budget.getAugust() != null ? budget.getAugust() : BigDecimal.ZERO);
        monthlyMap.put(9, budget.getSeptember() != null ? budget.getSeptember() : BigDecimal.ZERO);
        monthlyMap.put(10, budget.getOctober() != null ? budget.getOctober() : BigDecimal.ZERO);
        monthlyMap.put(11, budget.getNovember() != null ? budget.getNovember() : BigDecimal.ZERO);
        monthlyMap.put(12, budget.getDecember() != null ? budget.getDecember() : BigDecimal.ZERO);
        return monthlyMap;
    }

    private String determineStatus(BigDecimal variancePercentage) {
        if (variancePercentage.compareTo(BigDecimal.valueOf(-5)) >= 0 && 
            variancePercentage.compareTo(BigDecimal.valueOf(5)) <= 0) {
            return "ON_TRACK";
        } else if (variancePercentage.compareTo(BigDecimal.ZERO) < 0) {
            return "BELOW_TARGET";
        } else {
            return "ABOVE_TARGET";
        }
    }

    private String generateBudgetCode(Integer fiscalYear, String category) {
        String prefix = category.substring(0, Math.min(3, category.length()));
        return "BUD-" + fiscalYear + "-" + prefix + "-" + System.currentTimeMillis();
    }

    private BigDecimal calculatePercentage(BigDecimal part, BigDecimal whole) {
        if (whole.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(whole, 2, RoundingMode.HALF_UP);
    }

    private String getMonthName(int month) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        return months[month - 1];
    }

    private ReportHeader buildReportHeader(String reportName, Integer fiscalYear) {
        return ReportHeader.builder()
                .companyName(COMPANY_NAME)
                .reportName(reportName)
                .reportPeriod("Fiscal Year " + fiscalYear)
                .generatedDate(LocalDate.now())
                .currency(CURRENCY)
                .build();
    }

    private BudgetDTO convertToDTO(Budget budget) {
        return BudgetDTO.builder()
                .id(budget.getId())
                .budgetCode(budget.getBudgetCode())
                .budgetName(budget.getBudgetName())
                .fiscalYear(budget.getFiscalYear())
                .periodType(budget.getPeriodType())
                .category(budget.getCategory())
                .subCategory(budget.getSubCategory())
                .accountCode(budget.getAccountCode())
                .accountName(budget.getAccountName())
                .amount(budget.getAmount())
                .january(budget.getJanuary())
                .february(budget.getFebruary())
                .march(budget.getMarch())
                .april(budget.getApril())
                .may(budget.getMay())
                .june(budget.getJune())
                .july(budget.getJuly())
                .august(budget.getAugust())
                .september(budget.getSeptember())
                .october(budget.getOctober())
                .november(budget.getNovember())
                .december(budget.getDecember())
                .version(budget.getVersion())
                .isActive(budget.getIsActive())
                .notes(budget.getNotes())
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }
}

