package com.microfinance.loanapplications.repository;

import com.microfinance.loanapplications.entity.ApprovalEscalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApprovalEscalationRepository extends JpaRepository<ApprovalEscalation, Long> {
    
    List<ApprovalEscalation> findByLoanApplicationIdOrderByEscalatedAtDesc(Long applicationId);
    
    List<ApprovalEscalation> findByStatus(ApprovalEscalation.EscalationStatus status);
    
    @Query("SELECT e FROM ApprovalEscalation e WHERE e.escalatedBy = :userId ORDER BY e.escalatedAt DESC")
    List<ApprovalEscalation> findByEscalatedBy(@Param("userId") Long userId);
    
    @Query("SELECT e FROM ApprovalEscalation e WHERE e.status = 'PENDING' AND e.escalatedAt <= :cutoff")
    List<ApprovalEscalation> findStalePendingEscalations(@Param("cutoff") LocalDateTime cutoff);
}