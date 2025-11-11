package com.microfinance.base.service;

import com.microfinance.base.entity.RolePermission;
import com.microfinance.base.entity.User;
import com.microfinance.base.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;

    public List<RolePermission> getPermissionsForRole(User.UserRole role) {
        return rolePermissionRepository.findByRole(role);
    }

    public boolean hasPermission(User.UserRole role, String permission) {
        return rolePermissionRepository.existsByRoleAndPermission(role, permission);
    }

    public RolePermission addPermissionToRole(User.UserRole role, String permission, String description) {
        // Check if permission already exists for this role
        if (rolePermissionRepository.existsByRoleAndPermission(role, permission)) {
            throw new IllegalArgumentException("Permission " + permission + " already exists for role " + role);
        }

        RolePermission rolePermission = new RolePermission();
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);
        rolePermission.setDescription(description);
        // You might want to extract module from permission code or pass it as parameter
        rolePermission.setModule(extractModuleFromPermission(permission));

        return rolePermissionRepository.save(rolePermission);
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
        }
        return "Other";
    }
}