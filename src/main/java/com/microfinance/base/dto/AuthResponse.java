package com.microfinance.base.dto;

import com.microfinance.base.entity.User;
import lombok.Data;

@Data
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private User user;
    private boolean requiresMfa;
    private String mfaToken; // For MFA verification step

    public AuthResponse(String accessToken, String refreshToken, Long expiresIn, User user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
        this.requiresMfa = false;
    }


    public AuthResponse(String mfaToken, Long expiresIn, User user) {
        this.mfaToken = mfaToken;
        this.expiresIn = expiresIn;
        this.user = user;
        this.requiresMfa = true;
        this.tokenType = "MFA";
    }

    // Constructor for MFA verification success
    public AuthResponse(String accessToken, String refreshToken, Long expiresIn, User user, boolean requiresMfa) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
        this.requiresMfa = requiresMfa;
    }
}