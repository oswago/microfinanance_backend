package com.microfinance.loanapplications.mapper;

import com.microfinance.loanapplications.entity.LoanApplication;
import com.microfinance.loanapplications.dto.LoanApplicationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LoanApplicationMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "applicationNumber", source = "applicationNumber")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "stage", source = "stage")
    @Mapping(target = "appliedAmount", source = "appliedAmount")
    @Mapping(target = "tenureMonths", source = "tenureMonths")
    @Mapping(target = "purpose", source = "purpose")
    @Mapping(target = "submittedDate", source = "submittedDate")
    @Mapping(target = "approvedDate", source = "approvedDate")
    @Mapping(target = "rejectedDate", source = "rejectedDate")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    
    // Borrower fields - flattened
    @Mapping(target = "borrowerId", source = "borrower.id")
    @Mapping(target = "borrowerNumber", source = "borrower.borrowerNumber")
    @Mapping(target = "borrowerName", expression = "java(getBorrowerFullName(application))")
    
    // Loan product fields - flattened (NO loanProduct object)
    @Mapping(target = "loanProductId", source = "loanProduct.id")
    @Mapping(target = "loanProductName", source = "loanProduct.name")
    
    // Ignore fields that are set separately
    @Mapping(target = "approvalHistory", ignore = true)
    @Mapping(target = "approvalConditions", ignore = true)
    @Mapping(target = "approvalWorkflow", ignore = true)
    @Mapping(target = "borrowerKycSummary", ignore = true)
    @Mapping(target = "borrowerDocuments", ignore = true)
    @Mapping(target = "documentCompliance", ignore = true)
    
    LoanApplicationDto toDto(LoanApplication application);

    List<LoanApplicationDto> toDtoList(List<LoanApplication> applications);
    
    @Named("getBorrowerFullName")
    default String getBorrowerFullName(LoanApplication application) {
        if (application == null || application.getBorrower() == null) {
            return null;
        }
        return application.getBorrower().getFirstName() + " " + 
               (application.getBorrower().getLastName() != null ? 
                application.getBorrower().getLastName() : "");
    }
}