package com.microfinance.borrower.controller;

import com.microfinance.base.utils.SecurityUtils;
import com.microfinance.borrower.dto.BorrowerGroupDto;
import com.microfinance.borrower.dto.BorrowerSummaryDto;
import com.microfinance.borrower.dto.GroupLeaderResponseDto;
import com.microfinance.borrower.entity.BorrowerGroup;
import com.microfinance.borrower.service.BorrowerGroupService;
import com.microfinance.common.config.GeneralConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/borrower-groups")
@RequiredArgsConstructor
public class BorrowerGroupController {

    private final BorrowerGroupService borrowerGroupService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<Page<BorrowerGroupDto>> getAllGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<BorrowerGroupDto> groups = borrowerGroupService.getAllGroups(pageable);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<Page<BorrowerGroupDto>> searchGroups(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BorrowerGroupDto> groups = borrowerGroupService.searchGroups(query, pageable);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<Page<BorrowerGroupDto>> getGroupsByBranch(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BorrowerGroupDto> groups = borrowerGroupService.getGroupsByBranch(branchId, pageable);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<BorrowerGroupDto> getGroupById(@PathVariable Long id) {
        BorrowerGroupDto group = borrowerGroupService.getGroupById(id);
        return ResponseEntity.ok(group);
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER', 'BRANCH_MANAGER')")
    public ResponseEntity<List<BorrowerSummaryDto>> getGroupMembers(@PathVariable Long id) {
        List<BorrowerSummaryDto> members = borrowerGroupService.getGroupMembers(id);
        return ResponseEntity.ok(members);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<BorrowerGroupDto> createGroup(
            @Valid @RequestBody BorrowerGroupDto groupDto,
            Authentication authentication) {

        Long createdBy=securityUtils.getCurrentUserId();
        BorrowerGroupDto createdGroup = borrowerGroupService.createGroup(groupDto, createdBy);
        return ResponseEntity.ok(createdGroup);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<BorrowerGroupDto> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody BorrowerGroupDto groupDto) {
        
        BorrowerGroupDto updatedGroup = borrowerGroupService.updateGroup(id, groupDto);
        return ResponseEntity.ok(updatedGroup);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        borrowerGroupService.deleteGroup(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<BorrowerGroupDto> updateGroupStatus(
            @PathVariable Long id,
            @RequestParam GeneralConfig.GroupStatus status) {
        
        BorrowerGroupDto updatedGroup = borrowerGroupService.updateGroupStatus(id, status);
        return ResponseEntity.ok(updatedGroup);
    }

    @PostMapping("/{groupId}/members/{borrowerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<BorrowerGroupDto> addMemberToGroup(
            @PathVariable Long groupId,
            @PathVariable Long borrowerId) {
        
        BorrowerGroupDto updatedGroup = borrowerGroupService.addMemberToGroup(groupId, borrowerId);
        return ResponseEntity.ok(updatedGroup);
    }

    @DeleteMapping("/{groupId}/members/{borrowerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<BorrowerGroupDto> removeMemberFromGroup(
            @PathVariable Long groupId,
            @PathVariable Long borrowerId) {
        
        BorrowerGroupDto updatedGroup = borrowerGroupService.removeMemberFromGroup(groupId, borrowerId);
        return ResponseEntity.ok(updatedGroup);
    }

    @GetMapping("/branch/{branchId}/count")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<Long> getGroupCountByBranch(@PathVariable Long branchId) {
        Long count = borrowerGroupService.getGroupCountByBranch(branchId);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/{groupId}/leader/{borrowerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<GroupLeaderResponseDto> setGroupLeader(
            @PathVariable Long groupId,
            @PathVariable Long borrowerId) {

        GroupLeaderResponseDto response = borrowerGroupService.setGroupLeader(groupId, borrowerId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{groupId}/leader")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LOAN_OFFICER')")
    public ResponseEntity<BorrowerGroupDto> removeGroupLeader(@PathVariable Long groupId) {
        BorrowerGroupDto updatedGroup = borrowerGroupService.removeGroupLeader(groupId);
        return ResponseEntity.ok(updatedGroup);
    }


    private Long getUserIdFromAuthentication(Authentication authentication) {
        // Extract user ID from authentication principal
        return 1L; // Replace with actual implementation
    }
}