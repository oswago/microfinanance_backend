package com.microfinance.borrower.dto;

import com.microfinance.borrower.entity.Borrower;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class BorrowerDto {
    private Long id;
    
    @NotBlank
    private String firstName;
    
    @NotBlank
    private String lastName;
    
    private String middleName;
    
    @NotNull
    private Borrower.Gender gender;
    
    private LocalDate dateOfBirth;
    
    @NotBlank
    @Pattern(regexp = "\\d{10,15}")
    private String phoneNumber;
    
    @Email
    private String email;
    
    @NotBlank
    private String address;
    
    private String city;
    private String state;
    private String country;
    private String postalCode;
    
    @NotNull
    private Borrower.MaritalStatus maritalStatus;
    
    private String occupation;
    private String employer;
    private Double monthlyIncome;
    
    private Long branchId;
    private Long groupId;
    
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    
    private String nationality;
    private String identificationType;
    private String identificationNumber;
    private String notes;
    
    // Read-only fields
    private String borrowerNumber;
    private Borrower.BorrowerStatus status;
    private Borrower.KycStatus kycStatus;
    private LocalDateTime kycVerifiedAt;
    private String fullName;
    private String branchName;
    private String groupName;
    
    private List<BorrowerDocumentDto> documents;
    private List<BorrowerGuarantorDto> guarantors;

    public String getFullName() {
        if (middleName != null && !middleName.isEmpty()) {
            return firstName + " " + middleName + " " + lastName;
        }
        return firstName + " " + lastName;
    }
}