package com.microfinance.integrations.service;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// component/FinancialScheduledTasks.java
@Component
@RequiredArgsConstructor
@Slf4j
public class FinancialScheduledTasks {

    private final FinancialIntegrationService financialIntegrationService;
    private final UserService userService;

    @Scheduled(cron = "0 0 0 1 * ?") // Run on 1st day of each month at midnight
    @Transactional
    public void runMonthlyFinancialProcesses() {
        log.info("Running monthly financial processes");
        User systemUser = userService.getSystemUser();
        LocalDate accrualDate = LocalDate.now().minusDays(1); // Last day of previous month
        // Run interest accruals
        financialIntegrationService.runMonthlyInterestAccruals(accrualDate, systemUser);
        
        log.info(">>>>> MONTHLY FINANCIAL PROCESS COMPLETED >>>>");
    }
}