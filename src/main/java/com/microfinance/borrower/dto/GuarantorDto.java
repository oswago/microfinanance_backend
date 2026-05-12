package com.microfinance.borrower.dto;

import com.microfinance.common.config.GeneralConfig;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GuarantorDto {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String email;
    private GeneralConfig.Relationship relationship;
    private String occupation;
    private String employer;
    private Double monthlyIncome;
    private String address;
    private String identificationType;
    private String identificationNumber;
    private GeneralConfig.GuarantorStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}