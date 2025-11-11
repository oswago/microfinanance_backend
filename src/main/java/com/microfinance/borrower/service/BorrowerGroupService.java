package com.microfinance.borrower.service;

import com.microfinance.borrower.dto.*;
import com.microfinance.borrower.entity.BorrowerGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface BorrowerGroupService {
    
    Page<BorrowerGroupDto> getAllGroups(Pageable pageable);
    
    Page<BorrowerGroupDto> getGroupsByBranch(Long branchId, Pageable pageable);
    
    Page<BorrowerGroupDto> searchGroups(String search, Pageable pageable);
    
    BorrowerGroupDto getGroupById(Long id);
    
    BorrowerGroupDto getGroupByCode(String groupCode);
    
    BorrowerGroupDto createGroup(BorrowerGroupDto groupDto, Long createdBy);
    
    BorrowerGroupDto updateGroup(Long id, BorrowerGroupDto groupDto);
    
    void deleteGroup(Long id);
    
    BorrowerGroupDto updateGroupStatus(Long id, BorrowerGroup.GroupStatus status);

    List<BorrowerSummaryDto> getGroupMembers(Long groupId);
    
    BorrowerGroupDto addMemberToGroup(Long groupId, Long borrowerId);
    
    BorrowerGroupDto removeMemberFromGroup(Long groupId, Long borrowerId);
    
    Long getGroupCountByBranch(Long branchId);

    // Group performance
    GroupPerformanceDto getGroupPerformance(Long groupId);
    List<GroupMemberSummaryDto> getMemberSummaries(Long groupId);

    // Group loan eligibility
    Boolean isGroupEligibleForLoan(Long groupId, Long loanProductId);
    GroupLoanEligibilityDto checkGroupLoanEligibility(Long groupId, Long loanProductId);

    // Meeting management
    GroupMeetingDto scheduleMeeting(Long groupId, GroupMeetingDto meetingDto);
    List<GroupMeetingDto> getGroupMeetings(Long groupId, LocalDate from, LocalDate to);

}