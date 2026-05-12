package com.microfinance.borrower.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microfinance.base.entity.BaseEntity;
import com.microfinance.common.config.GeneralConfig;
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

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    @JsonIgnore
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
    private GeneralConfig.Relationship relationship;

    private String occupation;
    private String employer;
    private Double monthlyIncome;

    private String identificationType;
    private String identificationNumber;

    @Enumerated(EnumType.STRING)
    @NotNull
    private GeneralConfig.GuarantorStatus status = GeneralConfig.GuarantorStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;
}