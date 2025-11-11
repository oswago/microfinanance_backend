package com.microfinance.base.dto;

import lombok.Data;

@Data
public class MfaSetupResponse {
    private String secret;
    private String qrCodeImageUri;
    private boolean mfaEnabled;
    
    public MfaSetupResponse(String secret, String qrCodeImageUri, boolean mfaEnabled) {
        this.secret = secret;
        this.qrCodeImageUri = qrCodeImageUri;
        this.mfaEnabled = mfaEnabled;
    }
}