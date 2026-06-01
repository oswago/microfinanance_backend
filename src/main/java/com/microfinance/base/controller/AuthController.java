package com.microfinance.base.controller;

import com.microfinance.base.dto.*;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.base.service.AuthService;
import com.microfinance.base.service.JwtService;
import com.microfinance.base.service.MfaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MfaService mfaService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

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

    @PostMapping("/logoutORG")
    public ResponseEntity<Void> logoutORG(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token,
                                       HttpServletRequest request) {
        authService.logout(token, request);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/me")
    public ResponseEntity<String> getCurrentUser(HttpServletRequest request) {
        // This will be implemented with user details from security context
        return ResponseEntity.ok("Current user endpoint");
    }


    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(HttpServletRequest request) {
        log.info("=== VERIFY TOKEN CALLED ===");

        // Extract authorization from request
        String authorization = request.getHeader("Authorization");
        log.info("Authorization header: {}", authorization);

        if (authorization == null || authorization.trim().isEmpty()) {
            log.warn("No Authorization header");
            return ResponseEntity.status(401).body(Map.of("error", "No token provided"));
        }

        if (!authorization.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header format: {}", authorization);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token format"));
        }

        try {
            User user = authService.verifyAndGetUser(authorization);

            if (user != null) {
                log.info("Token verified for user: {}", user.getUsername());

                Map<String, Object> userMap = Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "template/email", user.getEmail(),
                        "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                        "lastName", user.getLastName() != null ? user.getLastName() : "",
                        "role", user.getRole() != null ? user.getRole().name() : "UNKNOWN",
                        "branchId", user.getBranchId() != null ? user.getBranchId() : 0,
                        "active", user.getActive() != null ? user.getActive() : false
                );

                return ResponseEntity.ok(Map.of("user", userMap));
            } else {
                log.warn("Token verification failed - user not found or invalid");
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

        } catch (Exception e) {
            log.error("Token verification error: {}", e.getMessage(), e);
            return ResponseEntity.status(401).body(Map.of("error", "Verification failed: " + e.getMessage()));
        }
    }



}