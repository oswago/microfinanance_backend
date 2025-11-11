package com.microfinance.base.controller;

import com.microfinance.base.dto.*;
import com.microfinance.base.service.AuthService;
import com.microfinance.base.service.MfaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MfaService mfaService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                              HttpServletRequest request) {
        AuthResponse response = authService.authenticate(loginRequest, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-mfa")
    public ResponseEntity<AuthResponse> verifyMfa(
            @RequestHeader("Authorization") String mfaToken,
            @Valid @RequestBody MfaVerificationRequest request,
            HttpServletRequest httpRequest) {  // Add this parameter

        AuthResponse response = authService.verifyMfa(mfaToken.substring(7), request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/setup-mfa")
    public ResponseEntity<MfaSetupResponse> setupMfa(@RequestHeader("Authorization") String token) {
        MfaSetupResponse response = authService.setupMfa(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/enable-mfa")
    public ResponseEntity<MfaSetupResponse> enableMfa(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody MfaVerificationRequest request) {
        MfaSetupResponse response = authService.enableMfa(token, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/disable-mfa")
    public ResponseEntity<MfaSetupResponse> disableMfa(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody MfaVerificationRequest request) {
        MfaSetupResponse response = authService.disableMfa(token, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mfa-status")
    public ResponseEntity<MfaStatusResponse> getMfaStatus(@RequestHeader("Authorization") String token) {
        MfaStatusResponse response = authService.getMfaStatus(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate-backup-codes")
    public ResponseEntity<MfaBackupCodesResponse> generateBackupCodes(@RequestHeader("Authorization") String token) {
        MfaBackupCodesResponse response = authService.generateBackupCodes(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        AuthResponse response = authService.refreshToken(refreshToken.substring(7));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<String> getCurrentUser(HttpServletRequest request) {
        // This will be implemented with user details from security context
        return ResponseEntity.ok("Current user endpoint");
    }
}