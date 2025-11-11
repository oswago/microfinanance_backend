package com.microfinance.borrower.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.system.entity.Branch;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "borrowers")
@Data
@EqualsAndHashCode(callSuper = true)
public class Borrower extends BaseEntity {

    @NotBlank
    @Column(name = "borrower_number", unique = true)
    private String borrowerNumber;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String middleName;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @NotBlank
    @Pattern(regexp = "\\d{10,15}", message = "Phone number must be 10-15 digits")
    private String phoneNumber;

    @Email
    private String email;

    @NotBlank
    private String address;

    private String city;
    private String state;
    private String country;
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @NotNull
    private MaritalStatus maritalStatus;

    private String occupation;
    private String employer;
    private Double monthlyIncome;

    @Enumerated(EnumType.STRING)
    @NotNull
    private BorrowerStatus status = BorrowerStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @NotNull
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt;

    @Column(name = "kyc_verified_by")
    private Long kycVerifiedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private BorrowerGroup group;

    @Column(name = "created_by")
    private Long createdBy;

    // Emergency contact
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    // Additional fields
    private String nationality;
    private String identificationType; // PASSPORT, NATIONAL_ID, DRIVERS_LICENSE
    private String identificationNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "borrower", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BorrowerDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "borrower", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BorrowerGuarantor> guarantors = new ArrayList<>();

    public String getFullName() {
        if (middleName != null && !middleName.isEmpty()) {
            return firstName + " " + middleName + " " + lastName;
        }
        return firstName + " " + lastName;
    }

    @Enumerated(EnumType.STRING)
    private ClientType clientType = ClientType.INDIVIDUAL; // INDIVIDUAL, GROUP, SME

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    private String referralSource; // How they heard about MFI
    private String businessType; // For business loans
    private String businessAddress;
    private Integer dependents;

    // Credit scoring fields
    private Integer creditScore;
    private LocalDate creditScoreUpdatedAt;

    // Group membership tracking
    private Boolean isGroupLeader = false;

    // Risk assessment
    @Enumerated(EnumType.STRING)
    private RiskRating riskRating = RiskRating.LOW;

    // Additional contact info
    private String alternatePhoneNumber;

    public enum ClientType {
        INDIVIDUAL, GROUP_MEMBER, SME, CORPORATE
    }

    public enum RiskRating {
        LOW, MEDIUM, HIGH, VERY_HIGH
    }

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum MaritalStatus {
        SINGLE, MARRIED, DIVORCED, WIDOWED
    }

    public enum BorrowerStatus {
        ACTIVE, INACTIVE, BLACKLISTED, DECEASED
    }

    public enum KycStatus {
        PENDING, VERIFIED, REJECTED, EXPIRED
    }
}