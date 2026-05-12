package com.microfinance.common.entity;

import com.microfinance.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "in_app_notifications", 
       indexes = {
           @Index(name = "idx_user_id", columnList = "user_id"),
           @Index(name = "idx_is_read", columnList = "is_read"),
           @Index(name = "idx_created_at", columnList = "created_at"),
           @Index(name = "idx_type", columnList = "type"),
           @Index(name = "idx_reference_number", columnList = "reference_number"),
           @Index(name = "idx_user_read", columnList = "user_id, is_read")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditingEntityListener.class)
public class InAppNotification extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "user_name")
    private String userName;
    
    @Column(name = "user_role")
    private String userRole;
    
    @Column(name = "type", nullable = false, length = 50)
    private String type; // APPROVAL_REQUEST, APPROVAL_COMPLETED, DOCUMENT_VERIFIED, LOAN_DISBURSED, etc.
    
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "reference_type", length = 50)
    private String referenceType; // LOAN_APPLICATION, DOCUMENT, BORROWER, PAYMENT
    
    @Column(name = "reference_id")
    private Long referenceId;
    
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;
    
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "read_by")
    private Long readBy;
    
    @Column(name = "action_url")
    private String actionUrl;
    
    @Column(name = "action_label", length = 50)
    private String actionLabel;
    
    @Column(name = "priority", length = 20)
    @Builder.Default
    private String priority = "MEDIUM"; // HIGH, MEDIUM, LOW
    
    @Column(name = "icon", length = 50)
    private String icon;
    
    @Column(name = "color", length = 20)
    private String color;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON field for additional data
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private Long createdBy;
}