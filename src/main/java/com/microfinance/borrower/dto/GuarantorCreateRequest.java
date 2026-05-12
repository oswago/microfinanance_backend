package com.microfinance.borrower.dto;

import com.microfinance.common.config.GeneralConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GuarantorCreateRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    private String email;

    @NotNull(message = "Relationship is required")
    private GeneralConfig.Relationship relationship;

    private String occupation;
    private String employer;
    private Double monthlyIncome;

    @NotBlank(message = "Address is required")
    private String address;

    private String identificationType;
    private String identificationNumber;
    private GeneralConfig.GuarantorStatus status = GeneralConfig.GuarantorStatus.ACTIVE;
    private String notes;
}