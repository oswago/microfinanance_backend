// service/impl/FinancialPeriodServiceImpl.java
package com.microfinance.financials.financialperiod.service;

import com.microfinance.base.entity.User;
import com.microfinance.base.service.UserService;
import com.microfinance.financials.financialperiod.dto.ClosePeriodDto;
import com.microfinance.financials.generalledger.dto.FinancialPeriodDto;
import com.microfinance.financials.generalledger.entity.FinancialPeriod;
import com.microfinance.financials.generalledger.repository.FinancialPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialPeriodServiceImpl implements FinancialPeriodService {

    private final FinancialPeriodRepository financialPeriodRepository;
    private final UserService userService;

    @Override
    @Transactional
    public FinancialPeriodDto createFinancialPeriod(FinancialPeriodDto dto, User currentUser) {
        log.info("Creating financial period: {} {}", dto.getYear(), dto.getMonth());
        
        // Check if period already exists
        if (financialPeriodRepository.existsByYearAndMonth(dto.getYear(), dto.getMonth())) {
            throw new RuntimeException("Financial period already exists for " + dto.getPeriodName());
        }
        
        // Validate dates
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new RuntimeException("Start date cannot be after end date");
        }
        
        FinancialPeriod period = FinancialPeriod.builder()
                .year(dto.getYear())
                .month(dto.getMonth())
                .periodName(dto.getPeriodName())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status("OPEN")
                .notes(dto.getNotes())
                .createdBy(currentUser.getId())
                .build();
        
        period = financialPeriodRepository.save(period);
        log.info("Financial period created: {}", period.getPeriodName());
        
        return convertToDto(period);
    }

    @Override
    @Transactional
    public FinancialPeriodDto updateFinancialPeriod(Long id, FinancialPeriodDto dto, User currentUser) {
        log.info("Updating financial period: {}", id);
        
        FinancialPeriod period = financialPeriodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Financial period not found"));
        
        if (!"OPEN".equals(period.getStatus())) {
            throw new RuntimeException("Only open periods can be modified");
        }
        
        period.setStartDate(dto.getStartDate());
        period.setEndDate(dto.getEndDate());
        period.setNotes(dto.getNotes());
        
        period = financialPeriodRepository.save(period);
        log.info("Financial period updated: {}", period.getPeriodName());
        
        return convertToDto(period);
    }

    @Override
    @Transactional
    public FinancialPeriodDto closeFinancialPeriod(Long id, ClosePeriodDto request, User currentUser) {
        log.info("Closing financial period: {}", id);
        
        FinancialPeriod period = financialPeriodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Financial period not found"));
        
        if (!"OPEN".equals(period.getStatus())) {
            throw new RuntimeException("Only open periods can be closed");
        }
        
        // TODO: Run pre-closing validations
        // - Check if all transactions are posted
        // - Check if all journals are approved
        // - Run accruals if requested
        // - Run provisions if requested
        
        period.setStatus("CLOSED");
        period.setClosedAt(LocalDateTime.now());
        period.setClosedBy(currentUser.getId());
        
        if (request.getNotes() != null) {
            period.setNotes(period.getNotes() != null ? 
                period.getNotes() + "\nClosing notes: " + request.getNotes() : 
                "Closing notes: " + request.getNotes());
        }
        
        period = financialPeriodRepository.save(period);
        log.info("Financial period closed: {}", period.getPeriodName());
        
        return convertToDto(period);
    }

    @Override
    @Transactional
    public FinancialPeriodDto lockFinancialPeriod(Long id, String reason, User currentUser) {
        log.info("Locking financial period: {}", id);
        
        FinancialPeriod period = financialPeriodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Financial period not found"));
        
        if ("LOCKED".equals(period.getStatus())) {
            throw new RuntimeException("Period is already locked");
        }
        
        period.setStatus("LOCKED");
        period.setLockedAt(LocalDateTime.now());
        period.setLockedBy(currentUser.getId());
        
        if (reason != null) {
            period.setNotes(period.getNotes() != null ? 
                period.getNotes() + "\nLocked: " + reason : 
                "Locked: " + reason);
        }
        
        period = financialPeriodRepository.save(period);
        log.info("Financial period locked: {}", period.getPeriodName());
        
        return convertToDto(period);
    }

    @Override
    @Transactional
    public FinancialPeriodDto unlockFinancialPeriod(Long id, String reason, User currentUser) {
        log.info("Unlocking financial period: {}", id);
        
        FinancialPeriod period = financialPeriodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Financial period not found"));
        
        if (!"LOCKED".equals(period.getStatus())) {
            throw new RuntimeException("Only locked periods can be unlocked");
        }
        
        period.setStatus("OPEN");
        period.setLockedAt(null);
        period.setLockedBy(null);
        
        if (reason != null) {
            period.setNotes(period.getNotes() != null ? 
                period.getNotes() + "\nUnlocked: " + reason : 
                "Unlocked: " + reason);
        }
        
        period = financialPeriodRepository.save(period);
        log.info("Financial period unlocked: {}", period.getPeriodName());
        
        return convertToDto(period);
    }

    @Override
    @Transactional
    public FinancialPeriodDto reopenFinancialPeriod(Long id, String reason, User currentUser) {
        log.info("Reopening financial period: {}", id);
        
        FinancialPeriod period = financialPeriodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Financial period not found"));
        
        if (!"CLOSED".equals(period.getStatus())) {
            throw new RuntimeException("Only closed periods can be reopened");
        }
        
        period.setStatus("OPEN");
        period.setClosedAt(null);
        period.setClosedBy(null);
        
        if (reason != null) {
            period.setNotes(period.getNotes() != null ? 
                period.getNotes() + "\nReopened: " + reason : 
                "Reopened: " + reason);
        }
        
        period = financialPeriodRepository.save(period);
        log.info("Financial period reopened: {}", period.getPeriodName());
        
        return convertToDto(period);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FinancialPeriodDto> getFinancialPeriods(Integer year, String status, Pageable pageable, User currentUser) {
        log.info("Fetching financial periods");
        
        List<FinancialPeriod> periods;
        
        if (year != null && status != null) {
            periods = financialPeriodRepository.findByYearOrderByMonthAsc(year).stream()
                    .filter(p -> p.getStatus().equals(status))
                    .collect(Collectors.toList());
        } else if (year != null) {
            periods = financialPeriodRepository.findByYearOrderByMonthAsc(year);
        } else if (status != null) {
            periods = financialPeriodRepository.findByStatus(status);
        } else {
            periods = financialPeriodRepository.findAllByOrderByYearDescMonthDesc();
        }
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), periods.size());
        List<FinancialPeriod> pagedList = periods.subList(start, end);
        
        return new PageImpl<>(pagedList.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList()), pageable, periods.size());
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialPeriodDto getFinancialPeriodById(Long id, User currentUser) {
        FinancialPeriod period = financialPeriodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Financial period not found"));
        return convertToDto(period);
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialPeriodDto getCurrentFinancialPeriod(User currentUser) {
        LocalDate today = LocalDate.now();
        FinancialPeriod period = financialPeriodRepository.findCurrentOpenPeriod(today)
                .orElseThrow(() -> new RuntimeException("No open financial period found for current date"));
        return convertToDto(period);
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialPeriodDto getNextFinancialPeriod(Long id, User currentUser) {
        FinancialPeriod current = financialPeriodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Financial period not found"));
        
        int nextMonth = current.getMonth() + 1;
        int nextYear = current.getYear();
        if (nextMonth > 12) {
            nextMonth = 1;
            nextYear++;
        }
        
        FinancialPeriod next = financialPeriodRepository.findByYearAndMonth(nextYear, nextMonth)
                .orElse(null);
        
        return next != null ? convertToDto(next) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialPeriodDto getPreviousFinancialPeriod(Long id, User currentUser) {
        FinancialPeriod current = financialPeriodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Financial period not found"));
        
        int prevMonth = current.getMonth() - 1;
        int prevYear = current.getYear();
        if (prevMonth < 1) {
            prevMonth = 12;
            prevYear--;
        }
        
        FinancialPeriod prev = financialPeriodRepository.findByYearAndMonth(prevYear, prevMonth)
                .orElse(null);
        
        return prev != null ? convertToDto(prev) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialPeriodDto> getPeriodSummary(Integer year, User currentUser) {
        log.info("Getting period summary for year: {}", year);
        
        List<FinancialPeriod> periods = financialPeriodRepository.findByYearOrderByMonthAsc(year);
        
        return periods.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void autoCreatePeriods(Integer year, User currentUser) {
        log.info("Auto-creating financial periods for year: {}", year);
        
        for (int month = 1; month <= 12; month++) {
            if (!financialPeriodRepository.existsByYearAndMonth(year, month)) {
                LocalDate startDate = LocalDate.of(year, month, 1);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
                String periodName = startDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
                
                FinancialPeriod period = FinancialPeriod.builder()
                        .year(year)
                        .month(month)
                        .periodName(periodName)
                        .startDate(startDate)
                        .endDate(endDate)
                        .status("OPEN")
                        .createdBy(currentUser.getId())
                        .build();
                
                financialPeriodRepository.save(period);
                log.info("Created financial period: {}", periodName);
            }
        }
    }
    
    private FinancialPeriodDto convertToDto(FinancialPeriod period) {
        FinancialPeriodDto.FinancialPeriodDtoBuilder builder = FinancialPeriodDto.builder()
                .id(period.getId())
                .year(period.getYear())
                .month(period.getMonth())
                .periodName(period.getPeriodName())
                .startDate(period.getStartDate())
                .endDate(period.getEndDate())
                .status(period.getStatus())
                .closedAt(period.getClosedAt())
                .lockedAt(period.getLockedAt())
                .notes(period.getNotes())
                .createdAt(period.getCreatedAt());
        
        if (period.getClosedBy() != null) {
            try {
                User closedBy = userService.getUserById(period.getClosedBy());
                builder.closedByName(closedBy.getFullName());
            } catch (Exception e) {
                builder.closedByName("Unknown");
            }
            builder.closedBy(period.getClosedBy());
        }
        
        if (period.getLockedBy() != null) {
            try {
                User lockedBy = userService.getUserById(period.getLockedBy());
                builder.lockedByName(lockedBy.getFullName());
            } catch (Exception e) {
                builder.lockedByName("Unknown");
            }
            builder.lockedBy(period.getLockedBy());
        }
        
        if (period.getCreatedBy() != null) {
            try {
                User createdBy = userService.getUserById(period.getCreatedBy());
                builder.createdByName(createdBy.getFullName());
            } catch (Exception e) {
                builder.createdByName("System");
            }
        }
        
        return builder.build();
    }
}