package com.microfinance.borrower.dto;

import com.microfinance.borrower.entity.Borrower;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BorrowerSummaryDto {
    private Long id;
    private String borrowerNumber;
    private String firstName;
    private String lastName;
    private String middleName;
    private String fullName;
    private String phoneNumber;
    private String email;
    private Borrower.BorrowerStatus status;
    private Borrower.KycStatus kycStatus;
    private LocalDateTime kycVerifiedAt;
    private String branchName;
    private String groupName;
    private LocalDate dateOfBirth;
    private String occupation;
    private Double monthlyIncome;
    private String identificationNumber;
    private LocalDateTime createdAt;

    public String getFullName() {
        if (middleName != null && !middleName.isEmpty()) {
            return firstName + " " + middleName + " " + lastName;
        }
        return firstName + " " + lastName;
    }
}