package com.microfinance.loanapplications.mapper;

import com.microfinance.borrower.entity.Borrower;
import com.microfinance.loanapplications.dto.LoanApplicationDto;
import com.microfinance.loanapplications.entity.LoanApplication;
import com.microfinance.loanproducts.entity.LoanProduct;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-26T23:37:47+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.13 (Oracle Corporation)"
)
@Component
public class LoanApplicationMapperImpl implements LoanApplicationMapper {

    @Override
    public LoanApplicationDto toDto(LoanApplication application) {
        if ( application == null ) {
            return null;
        }

        LoanApplicationDto loanApplicationDto = new LoanApplicationDto();

        loanApplicationDto.setId( application.getId() );
        loanApplicationDto.setApplicationNumber( application.getApplicationNumber() );
        if ( application.getStatus() != null ) {
            loanApplicationDto.setStatus( application.getStatus().name() );
        }
        if ( application.getStage() != null ) {
            loanApplicationDto.setStage( application.getStage().name() );
        }
        loanApplicationDto.setAppliedAmount( application.getAppliedAmount() );
        loanApplicationDto.setTenureMonths( application.getTenureMonths() );
        loanApplicationDto.setPurpose( application.getPurpose() );
        loanApplicationDto.setSubmittedDate( application.getSubmittedDate() );
        loanApplicationDto.setApprovedDate( application.getApprovedDate() );
        loanApplicationDto.setRejectedDate( application.getRejectedDate() );
        loanApplicationDto.setCreatedAt( application.getCreatedAt() );
        loanApplicationDto.setUpdatedAt( application.getUpdatedAt() );
        loanApplicationDto.setBorrowerId( applicationBorrowerId( application ) );
        loanApplicationDto.setBorrowerNumber( applicationBorrowerBorrowerNumber( application ) );
        loanApplicationDto.setLoanProductId( applicationLoanProductId( application ) );
        loanApplicationDto.setLoanProductName( applicationLoanProductName( application ) );
        loanApplicationDto.setRejectionReason( application.getRejectionReason() );
        loanApplicationDto.setOfficerComments( application.getOfficerComments() );
        loanApplicationDto.setPurposeCategory( application.getPurposeCategory() );
        loanApplicationDto.setProcessingFee( application.getProcessingFee() );
        loanApplicationDto.setInsuranceFee( application.getInsuranceFee() );
        loanApplicationDto.setReturnedDate( application.getReturnedDate() );
        loanApplicationDto.setRiskScore( application.getRiskScore() );
        loanApplicationDto.setRiskLevel( application.getRiskLevel() );
        loanApplicationDto.setTermsAccepted( application.getTermsAccepted() );
        loanApplicationDto.setCreatedById( application.getCreatedById() );
        if ( application.getCurrentApprovalLevel() != null ) {
            loanApplicationDto.setCurrentApprovalLevel( Integer.parseInt( application.getCurrentApprovalLevel() ) );
        }
        loanApplicationDto.setNextApprovalRole( application.getNextApprovalRole() );

        loanApplicationDto.setBorrowerName( getBorrowerFullName(application) );

        return loanApplicationDto;
    }

    @Override
    public List<LoanApplicationDto> toDtoList(List<LoanApplication> applications) {
        if ( applications == null ) {
            return null;
        }

        List<LoanApplicationDto> list = new ArrayList<LoanApplicationDto>( applications.size() );
        for ( LoanApplication loanApplication : applications ) {
            list.add( toDto( loanApplication ) );
        }

        return list;
    }

    private Long applicationBorrowerId(LoanApplication loanApplication) {
        if ( loanApplication == null ) {
            return null;
        }
        Borrower borrower = loanApplication.getBorrower();
        if ( borrower == null ) {
            return null;
        }
        Long id = borrower.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String applicationBorrowerBorrowerNumber(LoanApplication loanApplication) {
        if ( loanApplication == null ) {
            return null;
        }
        Borrower borrower = loanApplication.getBorrower();
        if ( borrower == null ) {
            return null;
        }
        String borrowerNumber = borrower.getBorrowerNumber();
        if ( borrowerNumber == null ) {
            return null;
        }
        return borrowerNumber;
    }

    private Long applicationLoanProductId(LoanApplication loanApplication) {
        if ( loanApplication == null ) {
            return null;
        }
        LoanProduct loanProduct = loanApplication.getLoanProduct();
        if ( loanProduct == null ) {
            return null;
        }
        Long id = loanProduct.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String applicationLoanProductName(LoanApplication loanApplication) {
        if ( loanApplication == null ) {
            return null;
        }
        LoanProduct loanProduct = loanApplication.getLoanProduct();
        if ( loanProduct == null ) {
            return null;
        }
        String name = loanProduct.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
