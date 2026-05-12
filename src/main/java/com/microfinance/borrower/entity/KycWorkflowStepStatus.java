package com.microfinance.borrower.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microfinance.base.entity.BaseEntity;
import com.microfinance.borrower.enums.KycWorkflowStep;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_workflow_step_status")
@Data
@EqualsAndHashCode(callSuper = true)
public class KycWorkflowStepStatus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kyc_workflow_id", nullable = false)
    @JsonIgnore  // Add this
    private KycWorkflow kycWorkflow;

    @Enumerated(EnumType.STRING)
    @Column(name = "step", nullable = false)
    private KycWorkflowStep step;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StepStatus status = StepStatus.PENDING;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "completed_by_name")
    private String completedByName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "is_required")
    private Boolean isRequired = true;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    public enum StepStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        SKIPPED,
        CANCELLED
    }

    // Helper methods
    public boolean isCompleted() {
        return status == StepStatus.COMPLETED;
    }

    public boolean isPending() {
        return status == StepStatus.PENDING;
    }

    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) && !isCompleted();
    }
}