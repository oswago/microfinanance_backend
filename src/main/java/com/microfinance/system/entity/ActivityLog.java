// src/main/java/com/microfinance/system/entity/ActivityLog.java
package com.microfinance.system.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.common.config.GeneralConfig;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "borrower_id", nullable = false)
    private Long borrowerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private GeneralConfig.BorrowerActivityType activityType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "performed_by")
    private Long performedBy; // User ID who performed the action

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "activity_date", nullable = false)
    private LocalDateTime activityDate;

    // Additional fields for context
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @PrePersist
    protected void onCreate() {
        if (activityDate == null) {
            activityDate = LocalDateTime.now();
        }
    }
}