// entity/LegalNotice.java
package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.base.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "legal_notices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LegalNotice extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String noticeNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private Loan loan;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovery_case_id")
    private RecoveryCase recoveryCase;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_officer_id")
    private User assignedOfficer;
    
    @Column(nullable = false)
    private String noticeType;
    
    @Column(nullable = false)
    private LocalDate noticeDate;
    
    @Column(nullable = false)
    private LocalDate complianceDate;
    
    @Column(nullable = false)
    private String status; // PENDING, SENT, ACKNOWLEDGED, COMPLIED, DEFAULTED, CANCELLED
    
    @Column(nullable = false, length = 1000)
    private String reason;
    
    private String legalGrounds;
    
    @Column(length = 1000)
    private String additionalNotes;
    
    @Column(nullable = false)
    private String deliveryMethod;
    
    private String documentPath;
    
    private LocalDate sentDate;
    
    private LocalDate acknowledgedDate;
    
    private String acknowledgedBy;
    
    @Column(length = 500)
    private String acknowledgementNotes;
    
    private Boolean generateDocument;
    
    private Boolean notifyLegalTeam;
    
    private Boolean attachToCase;
    
    @CreatedDate
    private LocalDateTime createdDate;
    
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}