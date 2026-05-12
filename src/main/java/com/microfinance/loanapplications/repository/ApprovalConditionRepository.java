package com.microfinance.loanapplications.repository;

import com.microfinance.common.config.GeneralConfig;
import com.microfinance.loanapplications.entity.ApprovalCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApprovalConditionRepository extends JpaRepository<ApprovalCondition, Long> {
    
    List<ApprovalCondition> findByLoanApplicationId(Long applicationId);
    Optional<ApprovalCondition> findByLoanApplicationIdAndConditionType(Long applicationId, String conditionType);
    List<ApprovalCondition> findByLoanApplicationIdAndStatus(Long applicationId, GeneralConfig.ConditionStatus status);
    
    @Query("SELECT COUNT(ac) FROM ApprovalCondition ac WHERE ac.loanApplication.id = :applicationId AND ac.mandatory = true AND ac.status != 'COMPLETED'")
    Long countPendingMandatoryConditions(@Param("applicationId") Long applicationId);
}