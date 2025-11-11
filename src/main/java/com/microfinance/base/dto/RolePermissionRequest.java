package com.microfinance.base.dto;

import com.microfinance.base.entity.User;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RolePermissionRequest {
    private User.UserRole role;
    
    @NotBlank(message = "Permission is required")
    private String permission;
    
    private String description;
}