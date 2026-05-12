package com.microfinance.borrower.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microfinance.base.entity.BaseEntity;
import com.microfinance.borrower.enums.KycWorkflowState;
import com.microfinance.borrower.enums.KycWorkflowStep;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "kyc_workflows")
@Data
@EqualsAndHashCode(callSuper = true)
public class KycWorkflow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    @JsonIgnore // Add this
    private Borrower borrower;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_state", nullable = false)
    private KycWorkflowState currentState = KycWorkflowState.NOT_STARTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_state")
    private KycWorkflowState previousState;

    @Column(name = "current_step")
    private String currentStep;

    @Column(name = "workflow_version")
    private String workflowVersion = "1.0";

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "estimated_completion_date")
    private LocalDateTime estimatedCompletionDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "assigned_officer_id")
    private Long assignedOfficerId;

    @Column(name = "assigned_officer_name")
    private String assignedOfficerName;

    @OneToMany(mappedBy = "kycWorkflow", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  // ADD THIS - CRITICAL
    private List<KycWorkflowHistory> history = new ArrayList<>();

    @OneToMany(mappedBy = "kycWorkflow", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  // ADD THIS - CRITICAL
    private List<KycWorkflowStepStatus> stepStatuses = new ArrayList<>();

    // Helper methods
    public boolean isCompleted() {
        return currentState.isTerminalState();
    }

    public boolean isInProgress() {
        return !currentState.isTerminalState() && currentState != KycWorkflowState.NOT_STARTED;
    }

    public long getDaysInProgress() {
        if (startedAt == null) return 0;
        LocalDateTime endDate = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, endDate).toDays();
    }
}