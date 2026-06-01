package com.microfinance.base.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Data
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String sessionId;
    private String ipAddress;
    private String userAgent;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    private LocalDateTime loginTime;
    private LocalDateTime lastActivity;
    private Boolean active = true;

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    @Column(name = "is_force_logout")
    private Boolean forceLogout = false;

    @Column(name = "forced_logout_by")
    private Long forcedLogoutBy;

    @Column(name = "forced_logout_reason")
    private String forcedLogoutReason;

    // For session expiry tracking
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // For tracking if token is revoked
    @Column(name = "token_revoked")
    private Boolean tokenRevoked = false;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

}