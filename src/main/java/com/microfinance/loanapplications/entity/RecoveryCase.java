package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.base.entity.User;
import com.microfinance.borrower.entity.Borrower;
import com.microfinance.system.entity.Branch;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recovery_cases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryCase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String caseNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal originalLoanAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal recoveredAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal remainingAmount;

    private Integer daysOverdue;

    @Column(length = 50)
    private String currentStage;

    @Column(length = 20)
    private String status;

    @Column(length = 20)
    private String priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private User assignedAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private User closedBy;

    private LocalDate closedDate;

    @ElementCollection
    @CollectionTable(name = "recovery_case_completed_stages",
            joinColumns = @JoinColumn(name = "recovery_case_id"))
    @Column(name = "stage")
    private List<String> completedStages = new ArrayList<>();

    @OneToMany(mappedBy = "recoveryCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StageDate> stageDates = new ArrayList<>();

    @OneToMany(mappedBy = "recoveryCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CaseNote> caseNotes = new ArrayList<>();  // Renamed to caseNotes

    @Column(name = "notes_text", columnDefinition = "TEXT")  // New field for text notes
    private String notesText;

    @Column(name = "recovery_rate")
    private Integer recoveryRate;

    public LocalDate lastPaymentDate;



}