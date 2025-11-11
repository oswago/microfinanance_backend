package com.microfinance.borrower.dto;

import com.microfinance.borrower.entity.BorrowerGuarantor;
import lombok.Data;

@Data
public class BorrowerGuarantorDto {
    private Long id;
    private Long borrowerId;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String address;
    private BorrowerGuarantor.Relationship relationship;
    private String occupation;
    private String employer;
    private Double monthlyIncome;
    private String identificationType;
    private String identificationNumber;
    private BorrowerGuarantor.GuarantorStatus status;
    private String notes;
    private String borrowerName;
}