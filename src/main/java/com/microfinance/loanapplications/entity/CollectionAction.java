package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.base.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "collection_actions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionAction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovery_case_id")
    private RecoveryCase recoveryCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private GeneralConfig.ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_status", nullable = false, length = 50)
    private GeneralConfig.ActionStatus actionStatus;

    @Column(name = "action_date", nullable = false)
    private LocalDate actionDate;

    @Column(name = "action_time")
    private LocalTime actionTime;

    // Contact Information
    @Column(name = "contact_person", length = 255)
    private String contactPerson;

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_method", length = 50)
    private GeneralConfig.ContactMethod contactMethod;

    // Outcome Details
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 100)
    private GeneralConfig.Outcome outcome;

    @Column(name = "notes.txt", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "follow_up_time")
    private LocalTime followUpTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "follow_up_action", length = 100)
    private GeneralConfig.FollowUpAction followUpAction;

    // Promise to Pay
    @Column(name = "promise_amount", precision = 15, scale = 2)
    private BigDecimal promiseAmount;

    @Column(name = "promise_date")
    private LocalDate promiseDate;

    @Column(name = "payment_confirmed")
    private Boolean paymentConfirmed = false;

    // Assignment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id", nullable = false)
    private User performedBy;

    // Location (for field visits)
    @Column(name = "visit_latitude", precision = 10, scale = 8)
    private BigDecimal visitLatitude;

    @Column(name = "visit_longitude", precision = 11, scale = 8)
    private BigDecimal visitLongitude;

    @Column(name = "visit_address", columnDefinition = "TEXT")
    private String visitAddress;

    // Attachments
    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "recording_url", length = 500)
    private String recordingUrl;

    private LocalDate completedDate;

}