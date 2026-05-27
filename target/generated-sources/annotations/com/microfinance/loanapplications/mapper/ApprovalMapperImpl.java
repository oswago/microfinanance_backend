package com.microfinance.loanapplications.mapper;

import com.microfinance.base.entity.User;
import com.microfinance.loanapplications.dto.ApplicationApprovalDto;
import com.microfinance.loanapplications.entity.ApplicationApproval;
import com.microfinance.loanapplications.entity.LoanApplication;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-26T23:37:46+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.13 (Oracle Corporation)"
)
@Component
public class ApprovalMapperImpl implements ApprovalMapper {

    @Override
    public ApplicationApprovalDto toDto(ApplicationApproval approval) {
        if ( approval == null ) {
            return null;
        }

        ApplicationApprovalDto.ApplicationApprovalDtoBuilder applicationApprovalDto = ApplicationApprovalDto.builder();

        applicationApprovalDto.loanApplicationId( approvalLoanApplicationId( approval ) );
        applicationApprovalDto.applicationNumber( approvalLoanApplicationApplicationNumber( approval ) );
        applicationApprovalDto.approverId( approvalApproverId( approval ) );
        applicationApprovalDto.approverName( approvalApproverFirstName( approval ) );
        applicationApprovalDto.approverUsername( approvalApproverUsername( approval ) );
        if ( approval.getDecision() != null ) {
            applicationApprovalDto.decision( approval.getDecision().name() );
        }
        applicationApprovalDto.approvalRole( approval.getApprovalRole() );
        applicationApprovalDto.comments( approval.getComments() );
        applicationApprovalDto.approvalLevel( approval.getApprovalLevel() );
        applicationApprovalDto.decisionDate( approval.getDecisionDate() );
        applicationApprovalDto.id( approval.getId() );
        applicationApprovalDto.createdAt( approval.getCreatedAt() );
        applicationApprovalDto.updatedAt( approval.getUpdatedAt() );

        return applicationApprovalDto.build();
    }

    @Override
    public List<ApplicationApprovalDto> toDtoList(List<ApplicationApproval> approvals) {
        if ( approvals == null ) {
            return null;
        }

        List<ApplicationApprovalDto> list = new ArrayList<ApplicationApprovalDto>( approvals.size() );
        for ( ApplicationApproval applicationApproval : approvals ) {
            list.add( toDto( applicationApproval ) );
        }

        return list;
    }

    private Long approvalLoanApplicationId(ApplicationApproval applicationApproval) {
        if ( applicationApproval == null ) {
            return null;
        }
        LoanApplication loanApplication = applicationApproval.getLoanApplication();
        if ( loanApplication == null ) {
            return null;
        }
        Long id = loanApplication.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String approvalLoanApplicationApplicationNumber(ApplicationApproval applicationApproval) {
        if ( applicationApproval == null ) {
            return null;
        }
        LoanApplication loanApplication = applicationApproval.getLoanApplication();
        if ( loanApplication == null ) {
            return null;
        }
        String applicationNumber = loanApplication.getApplicationNumber();
        if ( applicationNumber == null ) {
            return null;
        }
        return applicationNumber;
    }

    private Long approvalApproverId(ApplicationApproval applicationApproval) {
        if ( applicationApproval == null ) {
            return null;
        }
        User approver = applicationApproval.getApprover();
        if ( approver == null ) {
            return null;
        }
        Long id = approver.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String approvalApproverFirstName(ApplicationApproval applicationApproval) {
        if ( applicationApproval == null ) {
            return null;
        }
        User approver = applicationApproval.getApprover();
        if ( approver == null ) {
            return null;
        }
        String firstName = approver.getFirstName();
        if ( firstName == null ) {
            return null;
        }
        return firstName;
    }

    private String approvalApproverUsername(ApplicationApproval applicationApproval) {
        if ( applicationApproval == null ) {
            return null;
        }
        User approver = applicationApproval.getApprover();
        if ( approver == null ) {
            return null;
        }
        String username = approver.getUsername();
        if ( username == null ) {
            return null;
        }
        return username;
    }
}
