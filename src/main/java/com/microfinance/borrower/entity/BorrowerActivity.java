package com.microfinance.borrower.entity;

import com.microfinance.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "borrower_activities")
@Data
@EqualsAndHashCode(callSuper = true)
public class BorrowerActivity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

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

    public enum ActivityType {
        // Borrower Management Activities
        BORROWER_CREATED,
        BORROWER_UPDATED,
        BORROWER_STATUS_CHANGED,
        BORROWER_KYC_INITIATED,
        BORROWER_KYC_VERIFIED,
        BORROWER_KYC_REJECTED,
        BORROWER_KYC_EXPIRED,
        
        // Document Activities
        DOCUMENT_UPLOADED,
        DOCUMENT_VERIFIED,
        DOCUMENT_REJECTED,
        DOCUMENT_DELETED,
        
        // Group Management Activities
        GROUP_ASSIGNED,
        GROUP_REMOVED,
        GROUP_LEADER_ASSIGNED,
        
        // Loan Application Activities
        LOAN_APPLICATION_SUBMITTED,
        LOAN_APPLICATION_APPROVED,
        LOAN_APPLICATION_REJECTED,
        LOAN_APPLICATION_WITHDRAWN,
        
        // Loan Disbursement Activities
        LOAN_DISBURSED,
        LOAN_DISBURSEMENT_FAILED,
        
        // Repayment Activities
        REPAYMENT_MADE,
        REPAYMENT_SCHEDULED,
        REPAYMENT_OVERDUE,
        REPAYMENT_PARTIAL,
        REPAYMENT_BOUNCE,
        
        // Savings Activities
        SAVINGS_DEPOSIT,
        SAVINGS_WITHDRAWAL,
        SAVINGS_INTEREST_APPLIED,
        
        // Communication Activities
        SMS_SENT,
        EMAIL_SENT,
        NOTIFICATION_SENT,
        REMINDER_SENT,
        
        // System Activities
        PROFILE_VIEWED,
        PASSWORD_CHANGED,
        CONTACT_UPDATED,
        EMPLOYMENT_UPDATED,
        
        // Risk Management Activities
        RISK_RATING_UPDATED,
        CREDIT_SCORE_UPDATED,
        BLACKLISTED,
        BLACKLIST_REMOVED,
        
        // Guarantor Activities
        GUARANTOR_ADDED,
        GUARANTOR_REMOVED,
        GUARANTOR_VERIFIED,
        
        // Meeting Activities
        MEETING_ATTENDED,
        MEETING_MISSED,
        
        // Miscellaneous
        NOTE_ADDED,
        FILE_UPLOADED,
        SYSTEM_AUTO_UPDATE
    }
}