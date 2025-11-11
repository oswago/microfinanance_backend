package com.microfinance.base.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_permissions")
@Data
public class RolePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private User.UserRole role;

    @Column(name = "permission", nullable = false, length = 100)
    private String permission;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "module", length = 50)
    private String module;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public RolePermission() {}

    public RolePermission(User.UserRole role, String permission, String description, String module) {
        this.role = role;
        this.permission = permission;
        this.description = description;
        this.module = module;
    }
}