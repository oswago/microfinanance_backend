package com.microfinance.base.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

    @NotBlank
    @Column(unique = true)
    private String username;

    @NotBlank
    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    private String password;

    private String firstName;
    private String lastName;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private Boolean active = true;
    private LocalDateTime lastLogin;
    private Long branchId;

    private Integer failedLoginAttempts = 0;
    private LocalDateTime accountLockedUntil;

    // MFA Fields
    private Boolean mfaEnabled = false;
    private String mfaSecret;

    // Add constructor to ensure proper initialization
    public User() {
        this.failedLoginAttempts = 0;
        this.active = true;
        this.mfaEnabled = false;
    }

    // Helper method to safely get failed login attempts
    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts != null ? failedLoginAttempts : 0;
    }

    // Helper method to safely increment failed login attempts
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts = getFailedLoginAttempts() + 1;
    }

    public enum UserRole {
        SUPER_ADMIN,
        CREDIT_APPROVER,
        LOAN_OFFICER,
        BRANCH_MANAGER,
        CASHIER,
        COLLECTION_OFFICER,
        ACCOUNTANT,
        AUDITOR,
       CUSTOMER_SERVICE,
        FIELD_AGENT,
       USER
    }

}