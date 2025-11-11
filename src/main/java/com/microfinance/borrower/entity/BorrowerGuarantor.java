package com.microfinance.borrower.entity;

import com.microfinance.base.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "borrower_guarantors")
@Data
@EqualsAndHashCode(callSuper = true)
public class BorrowerGuarantor extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;

    @NotBlank
    private String fullName;

    @NotBlank
    private String phoneNumber;

    private String email;

    @NotBlank
    private String address;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Relationship relationship;

    private String occupation;
    private String employer;
    private Double monthlyIncome;

    private String identificationType;
    private String identificationNumber;

    @Enumerated(EnumType.STRING)
    @NotNull
    private GuarantorStatus status = GuarantorStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum Relationship {
        SPOUSE, PARENT, SIBLING, FRIEND, BUSINESS_PARTNER, OTHER
    }

    public enum GuarantorStatus {
        ACTIVE, INACTIVE, BLACKLISTED
    }
}