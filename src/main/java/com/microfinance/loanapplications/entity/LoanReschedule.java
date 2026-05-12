package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.base.entity.User;
import com.microfinance.common.config.GeneralConfig;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loan_reschedules")
@Data
@EqualsAndHashCode(callSuper = true)
public class LoanReschedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private LocalDate requestDate;

    private LocalDate approvalDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GeneralConfig.RescheduleStatus status = GeneralConfig.RescheduleStatus.PENDING;

    @Column(nullable = false)
    private Integer originalTenureMonths;

    @Column(nullable = false)
    private Integer newTenureMonths;

    @Column(precision = 15, scale = 2)
    private BigDecimal originalMonthlyPayment;

    @Column(precision = 15, scale = 2)
    private BigDecimal newMonthlyPayment;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String approvalComments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDate effectiveDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")  // use _id for clarity
    private User reviewedBy;

    // Enhanced fields
    private Integer gracePeriodDays = 0;
    private Boolean interestRecalculation = false;
    private BigDecimal reschedulingFee = BigDecimal.ZERO;

    public boolean isApproved() {
        return status == GeneralConfig.RescheduleStatus.APPROVED;
    }

    public LocalDate OriginalMaturityDate;
    public LocalDate newMaturityDate;
    private String additionalNotes;
    private Integer extensionMonths;


    @Size(max = 1000, message = "Comments cannot exceed 1000 characters")
    private String approvalNotes;


    @Size(max = 1000, message = "Comments cannot exceed 1000 characters")
    private String rejectionReason;

    private String approvalRole;

    private Integer tenureMonths;

    private Integer originalTermMonths;

    private Integer newTermMonths;

    private String approvalReference;


   private String reviewComments;

    private LocalDate resumeDate;

    private BigDecimal proposedInterestRate;

    // Documents relationship
    @OneToMany(mappedBy = "loanReschedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReschedulingDocument> documents = new ArrayList<>();
}

