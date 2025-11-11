// BranchResponse.java
package com.microfinance.system.dto;

import com.microfinance.system.entity.Branch;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BranchResponse {
    private Long id;
    private String code;
    private String name;
    private String address;
    private String phone;
    private String email;
    private BranchResponse parentBranch; // Simplified, no children
    private Branch.BranchType type;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Static constructor method
    public static BranchResponse fromEntity(Branch branch) {
        BranchResponse response = new BranchResponse();
        response.setId(branch.getId());
        response.setCode(branch.getCode());
        response.setName(branch.getName());
        response.setAddress(branch.getAddress());
        response.setPhone(branch.getPhone());
        response.setEmail(branch.getEmail());
        response.setType(branch.getType());
        response.setActive(branch.isActive());
        response.setCreatedAt(branch.getCreatedAt());
        response.setUpdatedAt(branch.getUpdatedAt());
        
        // Handle parent branch without circular reference
        if (branch.getParentBranch() != null) {
            BranchResponse parentResponse = new BranchResponse();
            parentResponse.setId(branch.getParentBranch().getId());
            parentResponse.setCode(branch.getParentBranch().getCode());
            parentResponse.setName(branch.getParentBranch().getName());
            parentResponse.setType(branch.getParentBranch().getType());
            response.setParentBranch(parentResponse);
        }
        
        return response;
    }
}