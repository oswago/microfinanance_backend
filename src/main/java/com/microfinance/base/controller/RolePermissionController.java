package com.microfinance.base.controller;

import com.microfinance.base.dto.RolePermissionRequest;
import com.microfinance.base.entity.RolePermission;
import com.microfinance.base.entity.User;
import com.microfinance.base.service.RolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RolePermissionController {
    
    private final RolePermissionService rolePermissionService;

    @GetMapping
     @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<RolePermission>> getAllRolePermissions() {
        List<RolePermission> permissions = rolePermissionService.getAllRolePermissions();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/{role}")
    //@PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<RolePermission>> getPermissionsForRole(@PathVariable User.UserRole role) {
        List<RolePermission> permissions = rolePermissionService.getPermissionsForRole(role);
        return ResponseEntity.ok(permissions);
    }

    @PostMapping("/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RolePermission> addPermissionToRole(@Valid @RequestBody RolePermissionRequest request) {
        RolePermission permission = rolePermissionService.addPermissionToRole(
            request.getRole(), request.getPermission(), request.getDescription());
        return ResponseEntity.ok(permission);
    }

    @DeleteMapping("/{role}/permissions/{permission}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> removePermissionFromRole(
            @PathVariable User.UserRole role, @PathVariable String permission) {
        rolePermissionService.removePermissionFromRole(role, permission);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{role}/has-permission/{permission}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Boolean> hasPermission(
            @PathVariable User.UserRole role, @PathVariable String permission) {
        boolean hasPermission = rolePermissionService.hasPermission(role, permission);
        return ResponseEntity.ok(hasPermission);
    }
}