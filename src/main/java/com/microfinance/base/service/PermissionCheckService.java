package com.microfinance.base.service;

import com.microfinance.base.entity.User;
import com.microfinance.base.repository.RolePermissionRepository;
import com.microfinance.base.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionCheckService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository; // Your service with hasCurrentUserPermission method

    /**
     * Check if current user has a specific permission
     * SUPER_ADMIN automatically gets all permissions
     */
    public boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found while checking permission: {}", permission);
            return false;
        }

        String username = authentication.getName();
        log.debug("Checking permission '{}' for user: {}", permission, username);

        try {
            // First check if user is SUPER_ADMIN using the existing method or directly
            User currentUser = getCurrentUser();
            
            if (currentUser == null) {
                log.warn("Could not retrieve current user: {}", username);
                return false;
            }

            // SUPER_ADMIN has all permissions
            if (currentUser.getRole() == User.UserRole.SUPER_ADMIN) {
                log.debug("SUPER_ADMIN user '{}' granted permission: {}", username, permission);
                return true;
            }

            // For other roles, check the specific permission using your existing method
            boolean hasPermission = hasCurrentUserPermission(permission);
            
            log.debug("Permission check for user '{}' (role: {}) on '{}': {}", 
                username, currentUser.getRole(), permission, hasPermission);
            
            return hasPermission;

        } catch (Exception e) {
            log.error("Error checking permission for user {}: {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Check if current user has ANY of the specified permissions
     */
    public boolean hasAnyPermission(String... permissions) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found while checking any permissions");
            return false;
        }

        String username = authentication.getName();
        log.debug("Checking any permission from [{}] for user: {}", String.join(", ", permissions), username);

        try {
            User currentUser = getCurrentUser();
            
            if (currentUser == null) {
                log.warn("Could not retrieve current user: {}", username);
                return false;
            }

            // SUPER_ADMIN has all permissions
            if (currentUser.getRole() == User.UserRole.SUPER_ADMIN) {
                log.debug("SUPER_ADMIN user '{}' granted any permission", username);
                return true;
            }

            // Check each permission using your existing method
            for (String permission : permissions) {
                if (hasCurrentUserPermission(permission)) {
                    log.debug("User '{}' granted permission: {}", username, permission);
                    return true;
                }
            }

            log.debug("User '{}' denied all requested permissions", username);
            return false;

        } catch (Exception e) {
            log.error("Error checking any permission for user {}: {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Check if current user has ALL of the specified permissions
     */
    public boolean hasAllPermissions(String... permissions) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found while checking all permissions");
            return false;
        }

        String username = authentication.getName();
        log.debug("Checking all permissions [{}] for user: {}", String.join(", ", permissions), username);

        try {
            User currentUser = getCurrentUser();
            
            if (currentUser == null) {
                log.warn("Could not retrieve current user: {}", username);
                return false;
            }

            // SUPER_ADMIN has all permissions
            if (currentUser.getRole() == User.UserRole.SUPER_ADMIN) {
                log.debug("SUPER_ADMIN user '{}' granted all permissions", username);
                return true;
            }

            // Check all permissions using your existing method
            for (String permission : permissions) {
                if (!hasCurrentUserPermission(permission)) {
                    log.debug("User '{}' missing required permission: {}", username, permission);
                    return false;
                }
            }

            log.debug("User '{}' granted all {} permissions", username, permissions.length);
            return true;

        } catch (Exception e) {
            log.error("Error checking all permissions for user {}: {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Check if current user has a specific role (including SUPER_ADMIN)
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        try {
            User currentUser = getCurrentUser();
            
            if (currentUser == null || currentUser.getRole() == null) {
                return false;
            }

            return currentUser.getRole().name().equals(role);

        } catch (Exception e) {
            log.error("Error checking role {}: {}", role, e.getMessage());
            return false;
        }
    }





    /**
     * Get the currently authenticated user
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("No authenticated user found");
            return null;
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Check if current user has a specific permission
     */
    public boolean hasCurrentUserPermission(String permission) {
        if (permission == null) {
            return false;
        }

        try {
            User currentUser = getCurrentUser();

            if (currentUser == null) {
                log.warn("No current user found");
                return false;
            }

            User.UserRole userRole = currentUser.getRole();

            if (userRole == null) {
                log.warn("User {} has no role assigned", currentUser.getUsername());
                return false;
            }

            // SUPER_ADMIN has all permissions
            if (userRole == User.UserRole.SUPER_ADMIN) {
                log.debug("SUPER_ADMIN user {} automatically granted permission {}",
                        currentUser.getUsername(), permission);
                return true;
            }

            boolean hasPermission = rolePermissionRepository.existsByRoleAndPermission(userRole, permission);

            log.debug("Current user {} with role {} has permission {}: {}",
                    currentUser.getUsername(), userRole, permission, hasPermission);

            return hasPermission;

        } catch (Exception e) {
            log.error("Error checking permission {}: {}", permission, e.getMessage());
            return false;
        }
    }


}