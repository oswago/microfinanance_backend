package com.microfinance.loanapplications.mapper;

import com.microfinance.loanapplications.entity.ApplicationApproval;
import com.microfinance.loanapplications.dto.ApplicationApprovalDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ApprovalMapper {

    ApprovalMapper INSTANCE = Mappers.getMapper(ApprovalMapper.class);

    @Mapping(source = "loanApplication.id", target = "loanApplicationId")
    @Mapping(source = "loanApplication.applicationNumber", target = "applicationNumber")
    @Mapping(source = "approver.id", target = "approverId")
    @Mapping(source = "approver.firstName", target = "approverName")
    @Mapping(source = "approver.username", target = "approverUsername")
    @Mapping(source = "decision", target = "decision")
    @Mapping(source = "approvalRole", target = "approvalRole")
    @Mapping(source = "comments", target = "comments")
    @Mapping(source = "approvalLevel", target = "approvalLevel")
    @Mapping(source = "decisionDate", target = "decisionDate")
    ApplicationApprovalDto toDto(ApplicationApproval approval);

    List<ApplicationApprovalDto> toDtoList(List<ApplicationApproval> approvals);
}