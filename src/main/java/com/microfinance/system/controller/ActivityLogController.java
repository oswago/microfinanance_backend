// src/main/java/com/microfinance/system/controller/ActivityLogController.java
package com.microfinance.system.controller;

import com.microfinance.common.config.GeneralConfig;
import com.microfinance.system.dto.ActivityLogDto;
import com.microfinance.system.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/activity-logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping("/borrower/{borrowerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<Page<ActivityLogDto>> getBorrowerActivityLogs(
            @PathVariable Long borrowerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "activityDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ActivityLogDto> activityLogs = activityLogService.getBorrowerActivityLogs(borrowerId, pageable);
        return ResponseEntity.ok(activityLogs);
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<Page<ActivityLogDto>> getGroupActivityLogs(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("activityDate").descending());
        Page<ActivityLogDto> activityLogs = activityLogService.getGroupActivityLogs(groupId, pageable);
        return ResponseEntity.ok(activityLogs);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<Page<ActivityLogDto>> getUserActivityLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("activityDate").descending());
        Page<ActivityLogDto> activityLogs = activityLogService.getUserActivityLogs(userId, pageable);
        return ResponseEntity.ok(activityLogs);
    }

    @GetMapping("/type/{activityType}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<Page<ActivityLogDto>> getActivityLogsByType(
            @PathVariable GeneralConfig.BorrowerActivityType activityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("activityDate").descending());
        Page<ActivityLogDto> activityLogs = activityLogService.getActivityLogsByType(activityType, pageable);
        return ResponseEntity.ok(activityLogs);
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<Page<ActivityLogDto>> getActivityLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("activityDate").descending());
        Page<ActivityLogDto> activityLogs = activityLogService.getActivityLogsByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(activityLogs);
    }

    @GetMapping("/borrower/{borrowerId}/recent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<List<ActivityLogDto>> getRecentBorrowerActivities(
            @PathVariable Long borrowerId,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<ActivityLogDto> activityLogs = activityLogService.getRecentBorrowerActivities(borrowerId, limit);
        return ResponseEntity.ok(activityLogs);
    }

    @GetMapping("/borrower/{borrowerId}/count")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<Long> getBorrowerActivityCount(@PathVariable Long borrowerId) {
        Long count = activityLogService.getBorrowerActivityCount(borrowerId);
        return ResponseEntity.ok(count);
    }
}