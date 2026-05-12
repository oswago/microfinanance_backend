package com.microfinance.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microfinance.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "branches")
@Data
public class Branch extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String code;
    
    @Column(nullable = false)
    private String name;
    
    private String address;
    private String phone;
    private String email;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_branch_id")
    @JsonIgnore
    private Branch parentBranch;
    
    @OneToMany(mappedBy = "parentBranch", cascade = CascadeType.ALL)
    @JsonIgnore // Add this to break circular reference
    private List<Branch> childBranches = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    private BranchType type;
    
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum BranchType {
        HEAD_OFFICE, BRANCH, UNIT
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }



    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}