package com.microfinance.loanapplications.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_reminders",
       indexes = {
           @Index(name = "idx_approver_id", columnList = "approver_id"),
           @Index(name = "idx_application_id", columnList = "application_id"),
           @Index(name = "idx_due_date", columnList = "due_date"),
           @Index(name = "idx_status", columnList = "is_dismissed, due_date"),
           @Index(name = "idx_reminder_type", columnList = "reminder_type"),
           @Index(name = "idx_priority", columnList = "priority")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ApprovalReminder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    
    @Column(name = "application_number", length = 50)
    private String applicationNumber;
    
    @Column(name = "borrower_name", length = 200)
    private String borrowerName;
    
    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "approver_id", nullable = false)
    private Long approverId;
    
    @Column(name = "approver_name", length = 200)
    private String approverName;
    
    @Column(name = "approver_email", length = 100)
    private String approverEmail;
    
    @Column(name = "reminder_type", nullable = false, length = 50)
    private String reminderType; // SLA, OVERDUE, PENDING, ESCALATION, FOLLOW_UP
    
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private String priority = "MEDIUM"; // HIGH, MEDIUM, LOW
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;
    
    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;
    
    @Column(name = "is_dismissed", nullable = false)
    @Builder.Default
    private Boolean isDismissed = false;
    
    @Column(name = "dismissed_at")
    private LocalDateTime dismissedAt;
    
    @Column(name = "dismissed_by")
    private Long dismissedBy;
    
    @Column(name = "dismissal_reason", columnDefinition = "TEXT")
    private String dismissalReason;
    
    @Column(name = "reminder_count", nullable = false)
    @Builder.Default
    private Integer reminderCount = 0;
    
    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;
    
    @Column(name = "next_reminder_at")
    private LocalDateTime nextReminderAt;
    
    @Column(name = "notification_sent")
    @Builder.Default
    private Boolean notificationSent = false;
    
    @Column(name = "notification_channel", length = 50)
    private String notificationChannel; // EMAIL, SMS, IN_APP, PUSH
    
    @Column(name = "reference_id")
    private Long referenceId; // Can reference escalation_id, delegation_id, etc.
    
    @Column(name = "reference_type", length = 50)
    private String referenceType; // ESCALATION, DELEGATION, SLA_BREACH
    
    @Column(name = "created_by", updatable = false)
    @CreatedBy
    private Long createdBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "updated_by")
    private Long updatedBy;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON field for additional data
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}