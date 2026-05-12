package com.microfinance.borrower.service;

import com.microfinance.borrower.dto.*;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.entity.BorrowerGroup;
import com.microfinance.borrower.repository.BorrowerGroupRepository;
import com.microfinance.borrower.repository.BorrowerRepository;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.exception.BusinessException;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.system.entity.Branch;
import com.microfinance.system.repository.BranchRepository;
import com.microfinance.system.service.ActivityLogService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.microfinance.base.utils.SecurityUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BorrowerGroupServiceImpl implements BorrowerGroupService {
    
    private final BorrowerGroupRepository groupRepository;
    private final BorrowerRepository borrowerRepository;
    private final BranchRepository branchRepository;
    private final ActivityLogService activityLogService;
    private final SecurityUtils securityUtils; // Inject SecurityUtils
    private final EntityManager entityManager; // Add this injection


    @Override
    @Transactional(readOnly = true)
    public Page<BorrowerGroupDto> getAllGroups(Pageable pageable) {
        return groupRepository.findAll(pageable).map(this::convertToDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<BorrowerGroupDto> getGroupsByBranch(Long branchId, Pageable pageable) {
        return groupRepository.findByBranchId(branchId, pageable).map(this::convertToDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<BorrowerGroupDto> searchGroups(String search, Pageable pageable) {
        return groupRepository.searchGroups(search, pageable).map(this::convertToDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BorrowerGroupDto getGroupById(Long id) {
        BorrowerGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + id));
        return convertToDto(group);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BorrowerGroupDto getGroupByCode(String groupCode) {
        BorrowerGroup group = groupRepository.findByGroupCode(groupCode)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with code: " + groupCode));
        return convertToDto(group);
    }
    
    @Override
    @Transactional
    public BorrowerGroupDto createGroup(BorrowerGroupDto groupDto, Long createdBy) {
        // Validate unique group code
        if (groupRepository.findByGroupCode(groupDto.getGroupCode()).isPresent()) {
            throw new IllegalArgumentException("Group code already exists: " + groupDto.getGroupCode());
        }
        
        BorrowerGroup group = convertToEntity(groupDto);
        
        // Set branch
        if (groupDto.getBranchId() != null) {
            Branch branch = branchRepository.findById(groupDto.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Branch not found with id: " + groupDto.getBranchId()));
            group.setBranch(branch);
        }
        
        group.setCreatedBy(createdBy);
        group.setStatus(GeneralConfig.GroupStatus.ACTIVE);
        
        BorrowerGroup savedGroup = groupRepository.save(group);
        log.info("Created new borrower group: {} with code: {}", savedGroup.getGroupName(), savedGroup.getGroupCode());
        
        return convertToDto(savedGroup);
    }
    
    @Override
    @Transactional
    public BorrowerGroupDto updateGroup(Long id, BorrowerGroupDto groupDto) {
        BorrowerGroup existingGroup = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + id));
        // Update fields
        existingGroup.setGroupName(groupDto.getGroupName());
        existingGroup.setDescription(groupDto.getDescription());
        existingGroup.setGroupType(groupDto.getGroupType());
        existingGroup.setMaxMembers(groupDto.getMaxMembers());
        existingGroup.setMeetingDay(DayOfWeek.valueOf(groupDto.getMeetingDay()));
        existingGroup.setMeetingLocation(groupDto.getMeetingLocation());
        existingGroup.setJointLiabilityType(groupDto.getJointLiabilityType());
        
        // Update branch if changed
        if (groupDto.getBranchId() != null && 
            (existingGroup.getBranch() == null || !existingGroup.getBranch().getId().equals(groupDto.getBranchId()))) {
            Branch branch = branchRepository.findById(groupDto.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Branch not found with id: " + groupDto.getBranchId()));
            existingGroup.setBranch(branch);
        }
        
        BorrowerGroup updatedGroup = groupRepository.save(existingGroup);
        log.info("Updated borrower group: {} with id: {}", updatedGroup.getGroupName(), id);
        
        return convertToDto(updatedGroup);
    }
    
    @Override
    @Transactional
    public void deleteGroup(Long id) {
        BorrowerGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + id));
        
        // Soft delete - set status to DISSOLVED
        group.setStatus(GeneralConfig.GroupStatus.DISSOLVED);
        groupRepository.save(group);
        
        log.info("Soft deleted borrower group: {} with id: {}", group.getGroupName(), id);
    }
    
    @Override
    @Transactional
    public BorrowerGroupDto updateGroupStatus(Long id, GeneralConfig.GroupStatus status) {
        BorrowerGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + id));
        
        group.setStatus(status);
        BorrowerGroup updatedGroup = groupRepository.save(group);
        
        log.info("Updated group status to {} for group: {} with id: {}", status, group.getGroupName(), id);
        
        return convertToDto(updatedGroup);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowerSummaryDto> getGroupMembers(Long groupId) {
        BorrowerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + groupId));

        return group.getMembers().stream()
                .map(this::convertBorrowerToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BorrowerGroupDto addMemberToGroup(Long groupId, Long borrowerId) {
        BorrowerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + groupId));


        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + borrowerId));
        
        // Check if borrower is already in the group
        if (group.getMembers().contains(borrower)) {
            throw new IllegalArgumentException("Borrower is already a member of this group");
        }

        // Check group member limit using current count + 1
        int currentCount = borrowerRepository.countByGroupId(groupId);
        // Check group member limit
        if (group.getMaxMembers() != null && group.getMembers().size() >= group.getMaxMembers()) {
            throw new IllegalArgumentException("Group has reached maximum member limit");
        }
        
        borrower.setGroup(group);
        borrowerRepository.save(borrower);

        // Update member count - use repository count for accuracy
        int newMemberCount = currentCount + 1;
        group.setMemberCount(newMemberCount);
        groupRepository.save(group);
        
        log.info("Added borrower {} to group {}", borrower.getFullName(), group.getGroupName());
        
        return convertToDto(group);
    }
    
    @Override
    @Transactional
    public BorrowerGroupDto removeMemberFromGroup(Long groupId, Long borrowerId) {
        BorrowerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + groupId));
        
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new EntityNotFoundException("Borrower not found with id: " + borrowerId));
        
        if (!group.getMembers().contains(borrower)) {
            throw new IllegalArgumentException("Borrower is not a member of this group");
        }
        
        borrower.setGroup(null);
        borrowerRepository.save(borrower);
        
        log.info("Removed borrower {} from group {}", borrower.getFullName(), group.getGroupName());
        
        return convertToDto(group);
    }

    public GroupLeaderResponseDto setGroupLeader(Long groupId, Long borrowerId) {
        BorrowerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));

        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found with id: " + borrowerId));

        // Check if borrower is a member of the group
        boolean isMember = borrowerRepository.isBorrowerInGroup(borrowerId, groupId);
        if (!isMember) {
            throw new BusinessException("Borrower is not a member of this group");
        }

        // Update group leader
        group.setGroupLeader(borrower);
        group.setGroupLeaderName(borrower.getFirstName() + " " + borrower.getLastName());
        group.setGroupLeaderPhone(borrower.getPhoneNumber());

        BorrowerGroup savedGroup = groupRepository.save(group);

        // Log the activity
        activityLogService.logBorrowerActivity(
                borrowerId,
                GeneralConfig.BorrowerActivityType.GROUP_LEADER_ASSIGNED,
                "Assigned as group leader for group: " + group.getGroupName(),
                securityUtils.getCurrentUserId()
        );

        return new GroupLeaderResponseDto(savedGroup);
    }

    public BorrowerGroupDto removeGroupLeader(Long groupId) {
        BorrowerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));



        // Store the leader ID BEFORE removing it
        Long previousLeaderId = null;
        String previousLeaderName = null;

        if (group.getGroupLeader() != null) {
            previousLeaderId = group.getGroupLeader().getId();
            previousLeaderName = group.getGroupLeaderName(); // Use the stored name
        }


        group.setGroupLeader(null);
        group.setGroupLeaderName(null);
        group.setGroupLeaderPhone(null);

        BorrowerGroup savedGroup = groupRepository.save(group);

        // Log the activity
        activityLogService.logBorrowerActivity(
                previousLeaderId,
                GeneralConfig.BorrowerActivityType.GROUP_LEADER_REMOVED,
                "Removed as group leader for group: " + group.getGroupName(),
                securityUtils.getCurrentUserId()
        );

        return convertToDto(savedGroup);
    }

    
    @Override
    @Transactional(readOnly = true)
    public Long getGroupCountByBranch(Long branchId) {
        return groupRepository.countActiveGroupsByBranch(branchId);
    }

    @Override
    public GroupPerformanceDto getGroupPerformance(Long groupId) {
        return null;
    }

    @Override
    public List<GroupMemberSummaryDto> getMemberSummaries(Long groupId) {
        return List.of();
    }

    @Override
    public Boolean isGroupEligibleForLoan(Long groupId, Long loanProductId) {
        return null;
    }

    @Override
    public GroupLoanEligibilityDto checkGroupLoanEligibility(Long groupId, Long loanProductId) {
        return null;
    }

    @Override
    public GroupMeetingDto scheduleMeeting(Long groupId, GroupMeetingDto meetingDto) {
        return null;
    }

    @Override
    public List<GroupMeetingDto> getGroupMeetings(Long groupId, LocalDate from, LocalDate to) {
        return List.of();
    }

    // Helper methods for entity-DTO conversion
    private BorrowerGroupDto convertToDto(BorrowerGroup group) {
        BorrowerGroupDto dto = new BorrowerGroupDto();
        dto.setId(group.getId());
        dto.setGroupCode(group.getGroupCode());
        dto.setGroupName(group.getGroupName());
        dto.setDescription(group.getDescription());
        dto.setGroupType(group.getGroupType());
        dto.setStatus(group.getStatus());
        dto.setMaxMembers(group.getMaxMembers());
        dto.setMeetingDay(String.valueOf(group.getMeetingDay()));
        dto.setMeetingTime(group.getMeetingTime());
        dto.setMeetingFrequency(group.getMeetingFrequency());
        dto.setMeetingLocation(group.getMeetingLocation());
        dto.setFormationDate(group.getFormationDate());
        dto.setJointLiabilityType(group.getJointLiabilityType());
        dto.setCurrentMemberCount(group.getCurrentMemberCount());
        dto.setGroupLeaderPhone(group.getGroupLeaderPhone());
        dto.setGroupLeaderName(group.getGroupLeaderName());
        //dto.setGroupLeaderId(group.getGroupLeader().getId());

        if (group.getBranch() != null) {
            dto.setBranchId(group.getBranch().getId());
            dto.setBranchName(group.getBranch().getName());
        }
        
        return dto;
    }
    
    private BorrowerGroup convertToEntity(BorrowerGroupDto dto) {
        BorrowerGroup group = new BorrowerGroup();
        group.setGroupCode(dto.getGroupCode());
        group.setGroupName(dto.getGroupName());
        group.setDescription(dto.getDescription());
        group.setGroupType(dto.getGroupType());
        group.setMaxMembers(dto.getMaxMembers());
        group.setMeetingDay(DayOfWeek.valueOf(dto.getMeetingDay()));
        group.setMeetingTime(dto.getMeetingTime());
        group.setMeetingFrequency(dto.getMeetingFrequency());
        group.setMeetingLocation(dto.getMeetingLocation());
        group.setFormationDate(dto.getFormationDate());
        group.setJointLiabilityType(dto.getJointLiabilityType());
        group.setGroupLeaderPhone(dto.getGroupLeaderPhone());

        
        return group;
    }


    private BorrowerSummaryDto convertBorrowerToDto(Borrower borrower) {
        BorrowerSummaryDto dto = new BorrowerSummaryDto();
        dto.setId(borrower.getId());
        dto.setBorrowerNumber(borrower.getBorrowerNumber());
        dto.setFirstName(borrower.getFirstName());
        dto.setLastName(borrower.getLastName());
        dto.setMiddleName(borrower.getMiddleName());
        dto.setFullName(borrower.getFullName());
        dto.setPhoneNumber(borrower.getPhoneNumber());
        dto.setEmail(borrower.getEmail());
        dto.setStatus(borrower.getStatus());
        dto.setKycStatus(borrower.getKycStatus());
        dto.setKycVerifiedAt(borrower.getKycVerifiedAt());
        dto.setDateOfBirth(borrower.getDateOfBirth());
        dto.setOccupation(borrower.getOccupation());
        dto.setMonthlyIncome(borrower.getMonthlyIncome());
        dto.setIdentificationNumber(borrower.getIdentificationNumber());
        dto.setCreatedAt(borrower.getCreatedAt());

        if (borrower.getBranch() != null) {
            dto.setBranchName(borrower.getBranch().getName());
        }

        if (borrower.getGroup() != null) {
            dto.setGroupName(borrower.getGroup().getGroupName());
        }

        return dto;
    }



}