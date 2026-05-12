// ReschedulingHistory.java
package com.microfinance.loanapplications.entity;

import com.microfinance.base.entity.BaseEntity;
import com.microfinance.base.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rescheduling_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReschedulingHistory extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescheduling_request_id")
    private ReschedulingRequest reschedulingRequest;
    
    @Column(name = "action", nullable = false, length = 50)
    private String action;
    
    @Column(name = "previous_terms", length = 1000)
    private String previousTerms;
    
    @Column(name = "new_terms", length = 1000)
    private String newTerms;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;
    
    @Column(name = "performed_at")
    private LocalDateTime performedAt;
    
    @Column(name = "comments", length = 500)
    private String comments;
}