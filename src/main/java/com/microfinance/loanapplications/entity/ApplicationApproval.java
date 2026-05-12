package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.base.entity.User;
import com.microfinance.common.config.GeneralConfig;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "application_approvals")
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)  // Add public no-args constructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)  // Add protected all-args constructor
@EqualsAndHashCode(callSuper = true)
public class ApplicationApproval extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GeneralConfig.ApprovalDecision decision;
    
    @Column(columnDefinition = "TEXT")
    private String comments;
    
    @Column(nullable = false)
    private Integer approvalLevel;
    
    private LocalDateTime decisionDate;
    private LocalDateTime createdAt;

    private String overrideLimits;
    private String overrideReason;
    
    @Column(length = 50)
    private String approvalRole; // e.g., "BRANCH_MANAGER", "CREDIT_COMMITTEE"
    
    // Enhanced methods
    public boolean isApproved() {
        return decision == GeneralConfig.ApprovalDecision.APPROVED;
    }
    
    public boolean isRejected() {
        return decision == GeneralConfig.ApprovalDecision.REJECTED;
    }

}

