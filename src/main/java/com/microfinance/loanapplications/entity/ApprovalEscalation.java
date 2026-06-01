package com.microfinance.loanapplications.entity;

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
@Table(name = "approval_escalations", indexes = {
    @Index(name = "idx_application_id_aprvlesc", columnList = "application_id"),
    @Index(name = "idx_escalated_by", columnList = "escalated_by"),
    @Index(name = "idx_status_aprvescl", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ApprovalEscalation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication loanApplication;
    
    @Column(name = "escalated_by", nullable = false)
    private Long escalatedBy;
    
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "priority")
    private String priority;
    
    @Column(name = "escalated_to_role")
    private String escalatedToRole;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private EscalationStatus status;
    
    @Column(name = "escalated_at", nullable = false)
    @CreatedDate
    private LocalDateTime escalatedAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "resolved_by")
    private Long resolvedBy;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    @CreatedBy
    private Long createdBy;
    
    public enum EscalationStatus {
        PENDING, APPROVED, REJECTED, COMPLETED
    }
}