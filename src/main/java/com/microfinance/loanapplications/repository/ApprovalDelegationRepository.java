package com.microfinance.loanapplications.repository;

import com.microfinance.loanapplications.entity.ApprovalDelegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalDelegationRepository extends JpaRepository<ApprovalDelegation, Long> {
    
    List<ApprovalDelegation> findByDelegatorIdAndStatus(Long delegatorId, ApprovalDelegation.DelegationStatus status);
    
    List<ApprovalDelegation> findByDelegateIdAndStatus(Long delegateId, ApprovalDelegation.DelegationStatus status);
    
    Optional<ApprovalDelegation> findByLoanApplicationIdAndDelegatorIdAndStatus(
            Long applicationId, Long delegatorId, ApprovalDelegation.DelegationStatus status);
    
    @Query("SELECT d FROM ApprovalDelegation d WHERE d.delegate.id = :delegateId " +
           "AND d.status = 'ACTIVE' AND (d.expiresAt IS NULL OR d.expiresAt > :now)")
    List<ApprovalDelegation> findActiveDelegationsByDelegate(@Param("delegateId") Long delegateId,
                                                              @Param("now") LocalDateTime now);
    
    @Query("SELECT d FROM ApprovalDelegation d WHERE d.expiresAt < :now AND d.status = 'ACTIVE'")
    List<ApprovalDelegation> findExpiredActiveDelegations(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(d) FROM ApprovalDelegation d WHERE d.delegate.id = :delegateId " +
           "AND d.status = 'ACTIVE' AND d.loanApplication.id = :applicationId")
    Long countActiveDelegationsForApplication(@Param("delegateId") Long delegateId,
                                               @Param("applicationId") Long applicationId);
}