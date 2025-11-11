package com.microfinance.base.service;

import com.microfinance.base.dto.*;
import com.microfinance.base.entity.MfaBackupCode;
import com.microfinance.base.entity.RefreshToken;
import com.microfinance.base.entity.User;
import com.microfinance.base.entity.UserSession;
import com.microfinance.base.repository.MfaBackupCodeRepository;
import com.microfinance.base.repository.RefreshTokenRepository;
import com.microfinance.base.repository.UserRepository;
import com.microfinance.base.repository.UserSessionRepository;
import com.microfinance.base.security.UserPrincipal;
import com.microfinance.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RateLimitService rateLimitService;
    private final MfaService mfaService;
    private final MfaBackupCodeRepository mfaBackupCodeRepository;
    private final UserSessionRepository userSessionRepository;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    public AuthResponse authenticate(LoginRequest loginRequest, HttpServletRequest request) {
        // Check rate limiting
        if (rateLimitService.isRateLimited(loginRequest.getUsername())) {
            throw new TooManyRequestsException("Too many login attempts. Please try again later.");
        }

        try {
            // Attempt authentication
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();

            // Check if MFA is enabled
            if (Boolean.TRUE.equals(user.getMfaEnabled())) {
                // Return temporary token for MFA verification
                String mfaToken = jwtService.generateMfaToken(userPrincipal);
                return new AuthResponse(mfaToken, jwtService.getMfaExpiration(), user);
            }

            // Generate JWT and refresh token
            String jwt = jwtService.generateToken(userPrincipal);
            String refreshToken = generateRefreshToken(userPrincipal);

            // Update last login and reset failed attempts
            user.setLastLogin(LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            userRepository.save(user);

            // CREATE SESSION RECORD - ADD THIS
            createUserSession(user, request, jwt);

            // Reset rate limit on successful login
            rateLimitService.resetRateLimit(loginRequest.getUsername());

            return new AuthResponse(jwt, refreshToken, jwtService.getJwtExpiration(), user);

        } catch (BadCredentialsException e) {
            // Increment failed login attempts safely
            userRepository.findByUsername(loginRequest.getUsername()).ifPresent(user -> {
                user.incrementFailedLoginAttempts();

                // Lock account after 5 failed attempts for 30 minutes
                if (user.getFailedLoginAttempts() >= 5) {
                    user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
                }

                userRepository.save(user);
            });

            // Re-throw BadCredentialsException to trigger 401
            throw e;
        } catch (AccessDeniedException e) {
            // Forward to exception handler to trigger 403
            throw e;
        } catch (Exception e) {
            // Catch other exceptions for 500 Internal Server Error
            throw new RuntimeException("An unexpected error occurred during login", e);
        }
    }

    // Add this method to create user sessions
    private void createUserSession(User user, HttpServletRequest request, String jwtToken) {
        try {
            UserSession session = new UserSession();
            session.setSessionId(generateSessionIdFromToken(jwtToken));
            session.setUser(user);
            session.setIpAddress(getClientIP(request));
            session.setUserAgent(request.getHeader("User-Agent"));
            session.setLoginTime(LocalDateTime.now());
            session.setLastActivity(LocalDateTime.now());
            session.setActive(true);
            userSessionRepository.save(session);
        } catch (Exception e) {
            // Log error but don't break login flow if session creation fails
            logger.error("Failed to create user session for user: " + user.getUsername(), e);
        }
    }

    private String generateSessionIdFromToken(String jwtToken) {
        // Extract a unique session ID from JWT or generate one
        return "session_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }


    @Transactional
    public AuthResponse verifyMfaOrg(String mfaToken, MfaVerificationRequest request) {
        if (!jwtService.validateMfaToken(mfaToken)) {
            throw new RuntimeException("Invalid or expired MFA token");
        }

        String username = jwtService.extractUsername(mfaToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isValid = false;

        if (request.isBackupCode()) {
            // Verify backup code
            List<MfaBackupCode> backupCodes = mfaBackupCodeRepository.findByUserAndUsed(user, false);
            isValid = backupCodes.stream()
                    .anyMatch(code -> code.getCode().equals(request.getCode()));

            if (isValid) {
                // Mark backup code as used
                MfaBackupCode usedCode = backupCodes.stream()
                        .filter(code -> code.getCode().equals(request.getCode()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Backup code not found"));
                usedCode.setUsed(true);
                usedCode.setUsedAt(LocalDateTime.now());
                mfaBackupCodeRepository.save(usedCode);
            }
        } else {
            // Verify TOTP code
            isValid = mfaService.isCodeValid(request.getCode(), user.getMfaSecret());
        }

        if (!isValid) {
            throw new RuntimeException("Invalid MFA code");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String jwt = jwtService.generateToken(userDetails);
        String refreshToken = generateRefreshToken(userDetails);

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);


        return new AuthResponse(jwt, refreshToken, jwtService.getJwtExpiration(), user, false);
    }

    @Transactional
    public AuthResponse verifyMfa(String mfaToken, MfaVerificationRequest request, HttpServletRequest httpRequest) {
        if (!jwtService.validateMfaToken(mfaToken)) {
            throw new RuntimeException("Invalid or expired MFA token");
        }

        String username = jwtService.extractUsername(mfaToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isValid = false;

        if (request.isBackupCode()) {
            // Verify backup code
            List<MfaBackupCode> backupCodes = mfaBackupCodeRepository.findByUserAndUsed(user, false);
            isValid = backupCodes.stream()
                    .anyMatch(code -> code.getCode().equals(request.getCode()));

            if (isValid) {
                // Mark backup code as used
                MfaBackupCode usedCode = backupCodes.stream()
                        .filter(code -> code.getCode().equals(request.getCode()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Backup code not found"));
                usedCode.setUsed(true);
                usedCode.setUsedAt(LocalDateTime.now());
                mfaBackupCodeRepository.save(usedCode);
            }
        } else {
            // Verify TOTP code
            isValid = mfaService.isCodeValid(request.getCode(), user.getMfaSecret());
        }

        if (!isValid) {
            throw new RuntimeException("Invalid MFA code");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String jwt = jwtService.generateToken(userDetails);
        String refreshToken = generateRefreshToken(userDetails);

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // CREATE SESSION RECORD AFTER SUCCESSFUL MFA VERIFICATION
        createUserSession(user, httpRequest, jwt);

        return new AuthResponse(jwt, refreshToken, jwtService.getJwtExpiration(), user, false);
    }


    @Transactional
    public MfaSetupResponse setupMfa(String token) {
        String username = jwtService.extractUsername(token.substring(7));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String secret = mfaService.generateSecret();
        String qrCodeImageUri = mfaService.generateQrCodeImageUri(secret, user.getUsername());

        user.setMfaSecret(secret);
        userRepository.save(user);

        return new MfaSetupResponse(secret, qrCodeImageUri, user.getMfaEnabled() != null && user.getMfaEnabled());
    }

    @Transactional
    public MfaSetupResponse enableMfa(String token, MfaVerificationRequest request) {
        String username = jwtService.extractUsername(token.substring(7));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!mfaService.isCodeValid(request.getCode(), user.getMfaSecret())) {
            throw new RuntimeException("Invalid MFA code");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);

        // Generate backup codes when enabling MFA
        generateBackupCodesForUser(user);

        return new MfaSetupResponse(null, null, true);
    }



    @Transactional
    public MfaSetupResponse disableMfa(String token, MfaVerificationRequest request) {
        String username = jwtService.extractUsername(token.substring(7));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify code before disabling
        if (!mfaService.isCodeValid(request.getCode(), user.getMfaSecret())) {
            throw new RuntimeException("Invalid MFA code");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);

        // Delete all backup codes
        mfaBackupCodeRepository.deleteByUser(user);

        return new MfaSetupResponse(null, null, false);
    }

    @Transactional(readOnly = true)
    public MfaStatusResponse getMfaStatus(String token) {
        String username = jwtService.extractUsername(token.substring(7));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean mfaEnabled = user.getMfaEnabled() != null && user.getMfaEnabled();
        boolean mfaConfigured = user.getMfaSecret() != null && !user.getMfaSecret().isEmpty();

        String message = mfaEnabled ? "MFA is enabled" : "MFA is disabled";
        if (mfaConfigured && !mfaEnabled) {
            message = "MFA is configured but not enabled";
        }

        return new MfaStatusResponse(mfaEnabled, mfaConfigured, message);
    }

    @Transactional
    public MfaBackupCodesResponse generateBackupCodes(String token) {
        String username = jwtService.extractUsername(token.substring(7));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getMfaEnabled() == null || !user.getMfaEnabled()) {
            throw new RuntimeException("MFA is not enabled for this user");
        }

        // Delete existing backup codes
        mfaBackupCodeRepository.deleteByUser(user);

        // Generate new backup codes
        List<String> backupCodes = mfaService.generateBackupCodes();
        List<MfaBackupCode> mfaBackupCodes = backupCodes.stream()
                .map(code -> {
                    MfaBackupCode backupCode = new MfaBackupCode();
                    backupCode.setUser(user);
                    backupCode.setCode(code);
                    backupCode.setUsed(false);
                    return backupCode;
                })
                .collect(Collectors.toList());

        mfaBackupCodeRepository.saveAll(mfaBackupCodes);

        return new MfaBackupCodesResponse(backupCodes, true, "Backup codes generated successfully");
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Verify refresh token exists and is not revoked
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (storedToken.getRevoked() || storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token is expired or revoked");
        }

        // Generate new tokens
        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = generateRefreshToken(userDetails);

        // Revoke old refresh token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new AuthResponse(newAccessToken, newRefreshToken, jwtService.getJwtExpiration(), user);
    }

    @Transactional
    public void logout(String token) {
        String username = jwtService.extractUsername(token.substring(7));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Revoke all refresh tokens for this user
        refreshTokenRepository.revokeAllUserTokens(user);

        SecurityContextHolder.clearContext();
    }


    private String generateRefreshToken(UserDetails userDetails) {
        String token = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")));
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(jwtService.getRefreshExpiration() / 1000));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);

        return token;
    }

    private void generateBackupCodesForUser(User user) {
        List<String> backupCodes = mfaService.generateBackupCodes();
        List<MfaBackupCode> mfaBackupCodes = backupCodes.stream()
                .map(code -> {
                    MfaBackupCode backupCode = new MfaBackupCode();
                    backupCode.setUser(user);
                    backupCode.setCode(code);
                    backupCode.setUsed(false);
                    return backupCode;
                })
                .collect(Collectors.toList());

        mfaBackupCodeRepository.saveAll(mfaBackupCodes);
    }
}