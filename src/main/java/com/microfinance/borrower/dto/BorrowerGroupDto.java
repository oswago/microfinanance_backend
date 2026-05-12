package com.microfinance.borrower.dto;

import com.microfinance.borrower.entity.BorrowerGroup;
import com.microfinance.common.config.GeneralConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private GeneralConfig.GroupType groupType;
    
    private Long branchId;
    private Integer maxMembers;
    private String meetingLocation;
    private String meetingFrequency;
    private LocalDateTime meetingTime;
    private LocalDateTime formationDate;
    private String meetingDay;
    private GeneralConfig.JointLiabilityType jointLiabilityType;
    private Long groupLeaderId;
    private String groupLeaderName;
    private String groupLeaderPhone;
    private BorrowerSummaryDto groupLeader;
    
    // Read-only fields
    private GeneralConfig.GroupStatus status;
    private Integer currentMemberCount;
    private String branchName;
    private List<BorrowerDto> members;


}