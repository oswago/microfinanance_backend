package com.microfinance.base.service;

import com.microfinance.base.entity.RolePermission;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private static final Logger log = LoggerFactory.getLogger(RolePermissionService.class);

    public List<RolePermission> getPermissionsForRole(User.UserRole role) {
        return rolePermissionRepository.findByRole(role);
    }

    public boolean hasPermission(User.UserRole role, String permission) {
        return rolePermissionRepository.existsByRoleAndPermission(role, permission);
    }

    public RolePermission addPermissionToRole(User.UserRole role, String permission, String description) {
        // Validate inputs
        log.info(">>Permission: {} Role: {} Description: {}  ", permission,role,description);
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        if (permission == null || permission.trim().isEmpty()) {
            throw new IllegalArgumentException("Permission cannot be null or empty");
        }

        // Check if permission already exists for this role
        if (rolePermissionRepository.existsByRoleAndPermission(role, permission)) {
            throw new IllegalArgumentException("Permission " + permission + " already exists for role " + role);
        }

        // Extract module from permission code (ensure it's not null)
        String module = extractModuleFromPermission(permission);
        if (module == null || module.trim().isEmpty()) {
            module = "General"; // Default module
            log.warn("Could not extract module for permission: {}, using default 'General'", permission);
        }

        // Ensure description is not null
        String finalDescription = (description != null && !description.trim().isEmpty())
                ? description
                : "Permission: " + permission;

        RolePermission rolePermission = new RolePermission();
        rolePermission.setRole(User.UserRole.valueOf(role.name())); // Convert enum to string
        rolePermission.setPermission(permission);
        rolePermission.setDescription(finalDescription);
        rolePermission.setModule(module);
        rolePermission.setCreatedAt(LocalDateTime.now());
        rolePermission.setUpdatedAt(LocalDateTime.now());

        try {
            return rolePermissionRepository.save(rolePermission);
        } catch (DataIntegrityViolationException e) {
            log.error("Failed to save permission {} for role {}: {}", permission, role, e.getMessage());
            // Check again if it exists (might have been added by another transaction)
            if (rolePermissionRepository.existsByRoleAndPermission(role, permission)) {
                throw new IllegalArgumentException("Permission " + permission + " already exists for role " + role);
            }
            throw new RuntimeException("Database constraint violation: " + e.getMessage(), e);
        }
    }


    public void removePermissionFromRole(User.UserRole role, String permission) {
        rolePermissionRepository.findByRoleAndPermission(role, permission)
                .ifPresent(rolePermissionRepository::delete);
    }

    public List<RolePermission> getAllRolePermissions() {
        return rolePermissionRepository.findAll();
    }

    public List<String> getModulesForRole(User.UserRole role) {
        return rolePermissionRepository.findDistinctModulesByRole(role);
    }

    public List<RolePermission> getPermissionsByRoleAndModule(User.UserRole role, String module) {
        return rolePermissionRepository.findByRoleAndModule(role, module);
    }

    private String extractModuleFromPermission(String permission) {
        // Extract module from permission code (e.g., "USER_CREATE" -> "User Management")
        // This is a simple implementation - you might want a more sophisticated mapping
        if (permission.startsWith("USER_") || permission.startsWith("AUTH_")) {
            return "Authentication & User Management";
        } else if (permission.startsWith("BORROWER_")) {
            return "Borrower Management";
        } else if (permission.startsWith("PRODUCT_")) {
            return "Loan Products";
        } else if (permission.startsWith("APPLICATION_") || permission.startsWith("LOAN_")) {
            return "Loan Applications";
        } else if (permission.startsWith("REPAYMENT_") || permission.startsWith("SCHEDULE_") ||
                permission.startsWith("PENALTY_") || permission.startsWith("EARLY_")) {
            return "Repayment Management";
        } else if (permission.startsWith("SAVINGS_") || permission.startsWith("DEPOSIT_") ||
                permission.startsWith("WITHDRAWAL_") || permission.startsWith("INTEREST_")) {
            return "Savings Management";
        } else if (permission.startsWith("COLLECTION_") || permission.startsWith("OVERDUE_") ||
                permission.startsWith("RECOVERY_") || permission.startsWith("FIELD_")) {
            return "Collections & Recovery";
        } else if (permission.startsWith("TRANSACTION_") || permission.startsWith("JOURNAL_") ||
                permission.startsWith("FINANCIAL_") || permission.startsWith("ACCOUNTING_")) {
            return "Accounting & Financial Reporting";
        } else if (permission.startsWith("NOTIFICATION_")) {
            return "Notifications & Communications";
        } else if (permission.startsWith("DASHBOARD_") || permission.startsWith("KPI_")) {
            return "Dashboards & Analytics";
        } else if (permission.startsWith("AUDIT_") || permission.startsWith("COMPLIANCE_")) {
            return "Audit & Compliance";
        } else if (permission.startsWith("SYSTEM_") || permission.startsWith("BRANCH_") ||
                permission.startsWith("CURRENCY_") || permission.startsWith("HOLIDAY_")) {
            return "System Configuration";
        } else if (permission.startsWith("MOBILE_") || permission.startsWith("OFFLINE_") ||
                permission.startsWith("API_") || permission.startsWith("WEBHOOK_")) {
            return "Mobile & Integration";
        } else if (permission.startsWith("ROLE_") || permission.startsWith("PERMISSION_")) {
            return "Role & Permission Management";
        } else if (permission.startsWith("LEGAL_") || permission.startsWith("COURT") || permission.startsWith("ASSET")) {
              return "Legal & Recovery Management";
    }
        return "Other";
    }
}