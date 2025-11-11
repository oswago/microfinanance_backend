package com.microfinance.borrower.dto;

import com.microfinance.borrower.entity.BorrowerGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BorrowerGroupDto {
    private Long id;
    
    @NotBlank
    private String groupCode;
    
    @NotBlank
    private String groupName;
    
    private String description;
    
    @NotNull
    private BorrowerGroup.GroupType groupType;
    
    private Long branchId;
    private Integer maxMembers;
    private String meetingSchedule;
    private String meetingLocation;
    private BorrowerGroup.JointLiabilityType jointLiabilityType;
    
    // Read-only fields
    private BorrowerGroup.GroupStatus status;
    private Integer currentMemberCount;
    private String branchName;
    private List<BorrowerDto> members;
}