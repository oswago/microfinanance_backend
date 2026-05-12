package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.User;
import com.microfinance.common.config.GeneralConfig;
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
@Table(name = "approval_comments", indexes = {
    @Index(name = "idx_application_id", columnList = "application_id"),
    @Index(name = "idx_commenter_id", columnList = "commenter_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ApprovalComment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication loanApplication;
    
    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commenter_id", nullable = false)
    private User commenter;
    
    @Column(name = "commenter_role")
    private String commenterRole;
    
    @Column(name = "is_internal")
    private boolean isInternal;

    @Column(name = "is_deleted")
    private boolean isDeleted;
    
    @Column(name = "parent_comment_id")
    private Long parentCommentId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    @CreatedBy
    private Long createdBy;


    @Column(name = "updated_by")
    private Long updatedBy;


}