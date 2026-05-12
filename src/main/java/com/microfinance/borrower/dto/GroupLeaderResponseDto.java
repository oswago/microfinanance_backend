package com.microfinance.borrower.dto;

import com.microfinance.borrower.entity.BorrowerGroup;
import lombok.Data;

import java.time.LocalDateTime;

// File: GroupLeaderResponseDto.java
@Data
public class GroupLeaderResponseDto {
    private Long groupId;
    private String groupName;
    private String groupCode;
    private Long leaderId;
    private String leaderName;
    private String leaderPhone;
    private String message;
    private LocalDateTime updatedAt;
    
    public GroupLeaderResponseDto(BorrowerGroup group) {
        this.groupId = group.getId();
        this.groupName = group.getGroupName();
        this.groupCode = group.getGroupCode();
        this.leaderId = group.getGroupLeader() != null ? group.getGroupLeader().getId() : null;
        this.leaderName = group.getGroupLeaderName();
        this.leaderPhone = group.getGroupLeaderPhone();
        this.message = "Group leader assigned successfully";
        this.updatedAt = group.getUpdatedAt();
    }
}