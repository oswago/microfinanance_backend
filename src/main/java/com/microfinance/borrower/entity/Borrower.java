package com.microfinance.borrower.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microfinance.base.entity.BaseEntity;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.system.entity.Branch;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "borrowers")
@Getter
@Setter
@ToString(exclude = {
        "documents", "guarantors", "kycWorkflows", "documentVerifications",
        "eligibleLoanProducts", "preferredLoanProducts", "productPreferences"
})
@EqualsAndHashCode(callSuper = true, exclude = {
        "documents", "guarantors", "kycWorkflows", "documentVerifications",
        "eligibleLoanProducts", "preferredLoanProducts", "productPreferences"
})

public class Borrower extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

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
    private GeneralConfig.Gender gender;

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
    private GeneralConfig.MaritalStatus maritalStatus;

    private String occupation;
    private String employer;
    private Double monthlyIncome;

    @Enumerated(EnumType.STRING)
    @NotNull
    private GeneralConfig.BorrowerStatus status = GeneralConfig.BorrowerStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @NotNull
    private GeneralConfig.KycStatus kycStatus = GeneralConfig.KycStatus.PENDING;

    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt;

    @Column(name = "kyc_verified_by")
    private Long kycVerifiedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    @JsonIgnore
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    @JsonIgnore
    private BorrowerGroup group;

    // === ADD PRODUCT TYPE RELATIONSHIP ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id")
    @JsonIgnore
    private LoanProduct loanProduct; // Default/preferred product type

    // Alternatively, if you want to store just the ID (no relationship):
    // @Column(name = "product_type_id")
    // private Long productTypeId;

    private String idNumber;

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
    @JsonIgnore
    private List<BorrowerDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "borrower", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<BorrowerGuarantor> guarantors = new ArrayList<>();

    // === NEW: LOAN PRODUCT RELATIONSHIPS ===

    // Many-to-Many: Borrower eligibility for loan products
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "borrower_eligible_products",
            joinColumns = @JoinColumn(name = "borrower_id"),
            inverseJoinColumns = @JoinColumn(name = "loan_product_id")
    )
    @JsonIgnore
    private List<LoanProduct> eligibleLoanProducts = new ArrayList<>();

    // Many-to-Many: Borrower's preferred loan products
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "borrower_preferred_products",
            joinColumns = @JoinColumn(name = "borrower_id"),
            inverseJoinColumns = @JoinColumn(name = "loan_product_id")
    )
    @JsonIgnore
    private List<LoanProduct> preferredLoanProducts = new ArrayList<>();

    // One-to-Many: Borrower's product preferences/history
    @OneToMany(mappedBy = "borrower", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<BorrowerProductPreference> productPreferences = new ArrayList<>();

    // === EXISTING RELATIONSHIPS ===
    @OneToMany(mappedBy = "borrower", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<KycWorkflow> kycWorkflows = new ArrayList<>();

    @OneToMany(mappedBy = "borrower", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<DocumentVerification> documentVerifications = new ArrayList<>();

    // === HELPER METHODS FOR LOAN PRODUCTS ===

    public boolean isEligibleForProduct(LoanProduct product) {
        return eligibleLoanProducts != null && eligibleLoanProducts.contains(product);
    }

    public boolean hasPreferredProduct(LoanProduct product) {
        return preferredLoanProducts != null && preferredLoanProducts.contains(product);
    }

    public void addEligibleProduct(LoanProduct product) {
        if (eligibleLoanProducts == null) {
            eligibleLoanProducts = new ArrayList<>();
        }
        if (!eligibleLoanProducts.contains(product)) {
            eligibleLoanProducts.add(product);
        }
    }

    public void addPreferredProduct(LoanProduct product) {
        if (preferredLoanProducts == null) {
            preferredLoanProducts = new ArrayList<>();
        }
        if (!preferredLoanProducts.contains(product)) {
            preferredLoanProducts.add(product);
        }
    }

    public void removeEligibleProduct(LoanProduct product) {
        if (eligibleLoanProducts != null) {
            eligibleLoanProducts.remove(product);
        }
    }

    public void removePreferredProduct(LoanProduct product) {
        if (preferredLoanProducts != null) {
            preferredLoanProducts.remove(product);
        }
    }

    // === HELPER METHOD FOR PRODUCT TYPE ===
    public Long getLoanProductId() {
        return loanProduct != null ? loanProduct.getId() : null;
    }

    public String getLoanProductName() {
        return loanProduct != null ? loanProduct.getName() : null;
    }

    public String getLoanProductCode() {
        return loanProduct != null ? loanProduct.getProductCode() : null;
    }

    // Get recommended products based on borrower profile and product type
    public List<LoanProduct> getRecommendedProducts() {
        List<LoanProduct> recommended = new ArrayList<>();

        if (eligibleLoanProducts != null) {
            // Filter based on borrower profile, preferences, and product type
            for (LoanProduct product : eligibleLoanProducts) {
                if (isProductRecommended(product)) {
                    recommended.add(product);
                }
            }
        }

        return recommended;
    }

    private boolean isProductRecommended(LoanProduct product) {
        // Business logic for product recommendation
        if (loanProduct != null && product.getProductType() != null) {
            // First check if product matches the preferred product type
            if (!product.getProductType().getId().equals(loanProduct.getId())) {
                return false;
            }
        }

        if (monthlyIncome != null && product.getMinLoanAmount() != null) {
            // Recommend products where loan amount is affordable
            double affordableAmount = monthlyIncome * 0.3; // 30% of monthly income
            return product.getMinLoanAmount().doubleValue() <= affordableAmount;
        }
        return true;
    }


    @Enumerated(EnumType.STRING)
    private GeneralConfig.ClientType clientType = GeneralConfig.ClientType.INDIVIDUAL;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    private Long assignedOfficerId;

    private String referralSource;
    private String businessType;
    private String businessAddress;
    private Integer dependents;


    // NEW FIELD - KYC Expiry Date
    @Column(name = "kyc_expiry_date")
    private LocalDate kycExpiryDate;


    // Credit scoring fields
    private Integer creditScore;
    private LocalDate creditScoreUpdatedAt;

    // Group membership tracking
    private Boolean isGroupLeader = false;

    private String alternatePhone;

    private String physicalAddress;

    // Risk assessment
    @Enumerated(EnumType.STRING)
    private GeneralConfig.RiskRating riskRating = GeneralConfig.RiskRating.LOW;

    // Additional contact info
    private String alternatePhoneNumber;

    // === ADD SAFE toString() METHOD ===
    @Override
    public String toString() {
        return "Borrower{" +
                "id=" + id +
                ", borrowerNumber='" + borrowerNumber + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", status=" + status +
                ", kycStatus=" + kycStatus +
                '}';
    }

    // === ADD SAFE getFullName() METHOD ===
    public String getFullName() {
        // Direct field access, no relationship traversal
        if (middleName != null && !middleName.isEmpty()) {
            return firstName + " " + middleName + " " + lastName;
        }
        return firstName + " " + lastName;
    }



}