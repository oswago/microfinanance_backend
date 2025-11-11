package com.microfinance.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MfaVerificationRequest {

    @NotBlank(message = "MFA code is required")
    @Size(min = 6, max = 6, message = "MFA code must be exactly 6 digits")
    @Pattern(regexp = "\\d{6}", message = "MFA code must contain only digits")
    private String code;

    private boolean rememberDevice = false;

    // For backup code usage
    private boolean isBackupCode = false;

    // Device identifier for remember me functionality
    private String deviceId;
}