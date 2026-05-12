package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.base.entity.User;
import com.microfinance.common.config.GeneralConfig;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_conditions")
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)  // Add public no-args constructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)  // Add protected all-args constructor
@EqualsAndHashCode(callSuper = true)
public class ApprovalCondition extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;
    
    @Column(nullable = false, length = 50)
    private String conditionType; // DOCUMENT_REQUIRED, GUARANTOR_NEEDED, etc.
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Boolean mandatory = true;
    
    private LocalDateTime dueDate;

    private LocalDateTime createdAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GeneralConfig.ConditionStatus status = GeneralConfig.ConditionStatus.PENDING;
    
    private LocalDateTime completedDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private User completedBy;
    
    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) && status != GeneralConfig.ConditionStatus.COMPLETED;
    }
}