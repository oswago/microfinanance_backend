// Create BranchRequest.java
package com.microfinance.system.dto;

import com.microfinance.system.entity.Branch;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class BranchRequest {
    @NotNull
    private String code;
    
    @NotNull
    private String name;
    
    private String address;
    private String phone;
    private String email;
    
    private Long parentBranchId;  // Use ID instead of object
    
    @NotNull
    private Branch.BranchType type;
    
    private boolean active = true;
}