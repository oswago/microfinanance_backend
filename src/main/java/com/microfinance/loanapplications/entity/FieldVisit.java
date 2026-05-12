// entity/FieldVisit.java
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
import java.time.LocalTime;

@Entity
@Table(name = "field_visits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FieldVisit extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String visitNumber;
    
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
    private LocalDate visitDate;
    
    private LocalTime visitTime;
    
    @Column(length = 500)
    private String visitAddress;
    
    @Column(nullable = false)
    private String purpose;
    
    @Column(nullable = false)
    private String status; // SCHEDULED, COMPLETED, CANCELLED, RESCHEDULED
    
    @Column(length = 1000)
    private String notes;
    
    private String outcome; // SUCCESSFUL, UNSUCCESSFUL, PARTIAL, POSTPONED
    
    @Column(length = 1000)
    private String completionNotes;
    
    private LocalDate completedDate;
    
    private Boolean notifyBorrower;
    
    private Boolean sendReminder;
    
    private Boolean reminderSent;
    
    private Boolean notificationSent;

    
    @CreatedDate
    private LocalDateTime createdDate;
    
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}