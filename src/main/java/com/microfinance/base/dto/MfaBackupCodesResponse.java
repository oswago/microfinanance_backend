package com.microfinance.base.dto;

import lombok.Data;
import java.util.List;

@Data
public class MfaBackupCodesResponse {
    private List<String> backupCodes;
    private boolean generated;
    private String message;
    
    public MfaBackupCodesResponse(List<String> backupCodes, boolean generated, String message) {
        this.backupCodes = backupCodes;
        this.generated = generated;
        this.message = message;
    }
    
    public MfaBackupCodesResponse() {}
}