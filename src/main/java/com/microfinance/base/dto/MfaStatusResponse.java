package com.microfinance.base.dto;

import lombok.Data;

@Data
public class MfaStatusResponse {
    private boolean mfaEnabled;
    private boolean mfaConfigured;
    private String message;
    
    public MfaStatusResponse(boolean mfaEnabled, boolean mfaConfigured, String message) {
        this.mfaEnabled = mfaEnabled;
        this.mfaConfigured = mfaConfigured;
        this.message = message;
    }
    
    public MfaStatusResponse() {}
}