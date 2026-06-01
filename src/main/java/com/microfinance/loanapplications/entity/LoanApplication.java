package com.microfinance.loanapplications.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microfinance.base.entity.BaseEntity;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.borrower.entity.BorrowerDocument;
import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanproducts.entity.LoanProduct;
import com.microfinance.base.entity.User;
import com.microfinance.system.entity.Branch;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import javax.net.ssl.SSLSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loan_applications")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)  // Add public no-args constructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)  // Add protected all-args constructor
@ToString(exclude = {"borrower", "branch", "loanProduct", "approvals", "loan", "createdByUser", "updatedByUser"})
@EqualsAndHashCode(callSuper = true, exclude = {"borrower", "branch", "loanProduct", "approvals", "loan", "createdByUser", "updatedByUser"})
public class LoanApplication extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal appliedAmount;
    
    @Column(nullable = false)
    private Integer tenureMonths;

    @Column(columnDefinition = "TEXT")
    private String tenureUnit;
    
    @Column(nullable = false, length = 500)
    private String purpose;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private GeneralConfig.LoanApplicationStatus status = GeneralConfig.LoanApplicationStatus.DRAFT;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private GeneralConfig.ApplicationStage stage = GeneralConfig.ApplicationStage.APPLICATION;
    
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String submittedBy;

    @Column(columnDefinition = "TEXT")
    private String additionalNotes;

    @Column(columnDefinition = "TEXT")
    private String officerComments;
    
    private LocalDateTime submittedDate;
    private LocalDateTime approvedDate;
    private LocalDateTime rejectedDate;
    private LocalDateTime disbursedDate;

    private LocalDateTime returnedDate;
    
    @OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ApplicationApproval> approvals = new ArrayList<>();
    
    @OneToOne(mappedBy = "loanApplication", cascade = CascadeType.ALL)
    @JsonIgnore
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user")
    private User createdByUser; // Rename the field

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user")
    private User updatedByUser; // Rename the field
    
    @Column(unique = true)
    private String applicationNumber;
    
    // Risk assessment fields
    private Integer creditScore;
    private String riskLevel; // LOW, MEDIUM, HIGH
    private Boolean recommendedForApproval;

    private Boolean termsAccepted;

    private LocalDateTime cancelledAt;

    @Column(columnDefinition = "TEXT")
    private String    cancelledBy;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(columnDefinition = "TEXT")
    private String rejectedBy;

    @Column(columnDefinition = "TEXT")
    private String revisionNotes;

    @Column(columnDefinition = "TEXT")
    private String returnedBy;

    @Column(columnDefinition = "TEXT")
    private String currentApprovalLevel;

    private LocalDateTime lastApprovalDate;

    @Column(columnDefinition = "TEXT")
    private String approvedBy;

    @Column(columnDefinition = "TEXT")
    private String purposeCategory;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal processingFee;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal insuranceFee;

    private String nextApprovalRole;


    private Integer riskScore;


    public boolean canBeSubmitted() {
        return status == GeneralConfig.LoanApplicationStatus.DRAFT;
    }

    public boolean canBeApproved() {
        return status == GeneralConfig.LoanApplicationStatus.SUBMITTED ||
                status == GeneralConfig.LoanApplicationStatus.UNDER_REVIEW ||
                status == GeneralConfig.LoanApplicationStatus.PENDING_APPROVAL ||
                status == GeneralConfig.LoanApplicationStatus.PENDING_FINAL_APPROVAL;  // Add this line
    }

    
    public boolean canBeDisbursed() {
        return status == GeneralConfig.LoanApplicationStatus.APPROVED && loan != null
               && loan.getStatus() == GeneralConfig.LoanStatus.PENDING_DISBURSEMENT;
    }


    // If you need to get the Long ID from base class, use:
    public Long getCreatedById() {
        return super.getCreatedBy(); // This calls the base class method
    }





}

