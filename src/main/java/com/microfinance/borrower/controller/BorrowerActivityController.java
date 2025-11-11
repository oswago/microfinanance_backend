package com.microfinance.borrower.controller;

import com.microfinance.borrower.dto.BorrowerActivityDto;
import com.microfinance.borrower.service.BorrowerActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/borrower-activities")
@RequiredArgsConstructor
public class BorrowerActivityController {

    private final BorrowerActivityService borrowerActivityService;

    @GetMapping("/borrower/{borrowerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<Page<BorrowerActivityDto>> getBorrowerActivities(
            @PathVariable Long borrowerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("activityDate").descending());
        Page<BorrowerActivityDto> activities = borrowerActivityService.getBorrowerActivities(borrowerId, pageable);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/borrower/{borrowerId}/recent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<BorrowerActivityDto>> getRecentActivities(
            @PathVariable Long borrowerId,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<BorrowerActivityDto> activities = borrowerActivityService.getRecentActivities(borrowerId, limit);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/borrower/{borrowerId}/timeline")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER', 'CREDIT_APPROVER')")
    public ResponseEntity<List<BorrowerActivityDto.TimelineGroup>> getActivityTimeline(
            @PathVariable Long borrowerId,
            @RequestParam(defaultValue = "30") int days) {
        
        List<BorrowerActivityDto.TimelineGroup> timeline = borrowerActivityService.getActivityTimeline(borrowerId, days);
        return ResponseEntity.ok(timeline);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<BorrowerActivityDto> logActivity(@RequestBody BorrowerActivityDto activityDto) {
        BorrowerActivityDto savedActivity = borrowerActivityService.logActivity(activityDto);
        return ResponseEntity.ok(savedActivity);
    }
}