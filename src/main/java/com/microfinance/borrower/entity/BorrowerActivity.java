package com.microfinance.borrower.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.common.config.GeneralConfig;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "borrower_activities")
@Data
@EqualsAndHashCode(callSuper = true)
public class BorrowerActivity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private GeneralConfig.BorrowerActivityType activityType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "activity_date", nullable = false)
    private LocalDateTime activityDate;

    @Column(name = "performed_by")
    private Long performedBy;

    @Column(name = "performed_by_name")
    private String performedByName;

    @Column(name = "reference_type")
    private String referenceType; // LOAN, SAVINGS, KYC, etc.

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_number")
    private String referenceNumber;

    // Additional metadata
    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "session_id")
    private String sessionId;
}