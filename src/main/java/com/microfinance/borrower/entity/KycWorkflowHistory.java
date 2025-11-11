package com.microfinance.borrower.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.borrower.enums.KycWorkflowState;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_workflow_history")
@Data
@EqualsAndHashCode(callSuper = true)
public class KycWorkflowHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kyc_workflow_id", nullable = false)
    private KycWorkflow kycWorkflow;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state")
    private KycWorkflowState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false)
    private KycWorkflowState toState;

    @Column(name = "action_performed", nullable = false)
    private String actionPerformed;

    @Column(name = "performed_by", nullable = false)
    private Long performedBy;

    @Column(name = "performed_by_name")
    private String performedByName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "transition_date", nullable = false)
    private LocalDateTime transitionDate = LocalDateTime.now();

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;
}