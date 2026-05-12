package com.microfinance.base.security;

import com.microfinance.base.entity.RolePermission;
import com.microfinance.base.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.management.relation.Role;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails {
    
    private final Long id;
    private final String username;
    private final String password;
    private final String email;
    private final User.UserRole role;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;
    private final User user;
    private final List<String> permissions;
    private final List<RolePermission> rolePermissions; // Optional: keep entities if needed

    public UserPrincipal(User user, List<RolePermission> rolePermissions) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.enabled = user.getActive() != null ? user.getActive() : true;
        this.user = user;
        this.rolePermissions = rolePermissions != null ? rolePermissions : new ArrayList<>();

        // Extract permission strings directly
        this.permissions = this.rolePermissions.stream()
                .map(RolePermission::getPermission)
                .collect(Collectors.toList());

        this.authorities = buildAuthorities(user.getRole(), this.permissions);
    }

    private Collection<? extends GrantedAuthority> buildAuthorities(User.UserRole role, List<String> permissions) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // Add role as authority
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        // Add all permissions as authorities (Spring Security will see these as authorities)
        permissions.forEach(permission ->
                authorities.add(new SimpleGrantedAuthority(permission))
        );
        return authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getAccountLockedUntil() == null || user.getAccountLockedUntil().isBefore(java.time.LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Long getUserId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    // Helper methods to check permissions
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean hasAnyPermission(String... permissionsToCheck) {
        for (String perm : permissionsToCheck) {
            if (permissions.contains(perm)) {
                return true;
            }
        }
        return false;
    }

    // For debugging
    @Override
    public String toString() {
        // Safe toString without object references
        return String.format("UserPrincipal{id=%d, username='%s', role=%s, permissions=%d}",
                id, username, role, permissions != null ? permissions.size() : 0);
    }


}