// ReschedulingRequest.java
package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.base.entity.User;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.common.config.GeneralConfig;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rescheduling_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReschedulingRequest extends BaseEntity {
    
    @Column(name = "request_number", unique = true, nullable = false, length = 50)
    private String requestNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    private RequestType requestType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequestStatus status;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    // Current terms
    @Column(name = "current_monthly_payment", precision = 15, scale = 2)
    private BigDecimal currentMonthlyPayment;
    
    @Column(name = "current_installments")
    private Integer currentInstallments;
    
    @Column(name = "current_interest_rate", precision = 5, scale = 2)
    private BigDecimal currentInterestRate;
    
    @Column(name = "current_total_interest", precision = 15, scale = 2)
    private BigDecimal currentTotalInterest;
    
    // Proposed terms
    @Column(name = "proposed_monthly_payment", precision = 15, scale = 2)
    private BigDecimal proposedMonthlyPayment;
    
    @Column(name = "proposed_installments")
    private Integer proposedInstallments;
    
    @Column(name = "proposed_interest_rate", precision = 5, scale = 2)
    private BigDecimal proposedInterestRate;
    
    @Column(name = "proposed_total_interest", precision = 15, scale = 2)
    private BigDecimal proposedTotalInterest;
    
    // Request details
    @Column(name = "additional_months")
    private Integer additionalMonths;
    
    @Column(name = "reduced_payment", precision = 15, scale = 2)
    private BigDecimal reducedPayment;
    
    @Column(name = "holiday_months")
    private Integer holidayMonths;
    
    @Column(name = "resume_date")
    private LocalDate resumeDate;
    
    @Column(name = "additional_notes", length = 1000)
    private String additionalNotes;
    
    // Review details
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    
    @Column(name = "review_date")
    private LocalDateTime reviewDate;
    
    @Column(name = "review_comments", length = 500)
    private String reviewComments;
    
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    
    // Requested by
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;
    
    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;
    
    // Audit fields
    @Column(name = "approved_date")
    private LocalDateTime approvedDate;
    
    @Column(name = "rejected_date")
    private LocalDateTime rejectedDate;
    
    // Enums
    public enum RequestType {
        TENURE_EXTENSION,
        PAYMENT_REDUCTION,
        PAYMENT_HOLIDAY,
        INTEREST_RATE_ADJUSTMENT,
        LOAN_RESTRUCTURING
    }
    
    public enum RequestStatus {
        PENDING,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        CANCELLED
    }
}