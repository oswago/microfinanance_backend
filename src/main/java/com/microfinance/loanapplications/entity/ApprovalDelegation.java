package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_delegations", indexes = {
    @Index(name = "idx_application_id", columnList = "application_id"),
    @Index(name = "idx_delegator_id", columnList = "delegator_id"),
    @Index(name = "idx_delegate_id", columnList = "delegate_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ApprovalDelegation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private LoanApplication loanApplication;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegator_id", nullable = false)
    private User delegator;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegate_id", nullable = false)
    private User delegate;
    
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DelegationStatus status;
    
    @Column(name = "delegated_at", nullable = false)
    @CreatedDate
    private LocalDateTime delegatedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    @Column(name = "revoked_by")
    private Long revokedBy;
    
    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;
    
    @Column(name = "keep_permissions")
    private boolean keepPermissions;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    @CreatedBy
    private Long createdBy;
    
    public enum DelegationStatus {
        ACTIVE, EXPIRED, REVOKED, COMPLETED
    }
}