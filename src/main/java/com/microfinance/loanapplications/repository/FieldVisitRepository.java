// repository/FieldVisitRepository.java
package com.microfinance.loanapplications.repository;

import com.microfinance.base.entity.User;
import com.microfinance.loanapplications.entity.FieldVisit;
import com.microfinance.loanapplications.entity.Loan;
import com.microfinance.loanapplications.entity.RecoveryCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FieldVisitRepository extends JpaRepository<FieldVisit, Long> {
    
    List<FieldVisit> findByLoan(Loan loan);
    
    List<FieldVisit> findByRecoveryCase(RecoveryCase recoveryCase);
    
    List<FieldVisit> findByAssignedOfficerAndStatus(User officer, String status);
    
    Page<FieldVisit> findByLoanId(Long loanId, Pageable pageable);
    
    Page<FieldVisit> findByRecoveryCaseId(Long recoveryCaseId, Pageable pageable);
    
    Page<FieldVisit> findByAssignedOfficerId(Long officerId, Pageable pageable);
    
    Page<FieldVisit> findByStatus(String status, Pageable pageable);
    
    @Query("SELECT v FROM FieldVisit v WHERE " +
           "(:loanId IS NULL OR v.loan.id = :loanId) AND " +
           "(:recoveryCaseId IS NULL OR v.recoveryCase.id = :recoveryCaseId) AND " +
           "(:assignedOfficerId IS NULL OR v.assignedOfficer.id = :assignedOfficerId) AND " +
           "(:status IS NULL OR v.status = :status) AND " +
           "(:fromDate IS NULL OR v.visitDate >= :fromDate) AND " +
           "(:toDate IS NULL OR v.visitDate <= :toDate)")
    Page<FieldVisit> findAllWithFilters(@Param("loanId") Long loanId,
                                        @Param("recoveryCaseId") Long recoveryCaseId,
                                        @Param("assignedOfficerId") Long assignedOfficerId,
                                        @Param("status") String status,
                                        @Param("fromDate") LocalDate fromDate,
                                        @Param("toDate") LocalDate toDate,
                                        Pageable pageable);
    
    @Query("SELECT v FROM FieldVisit v WHERE v.assignedOfficer.id = :officerId AND v.visitDate >= :today AND v.status = 'SCHEDULED' ORDER BY v.visitDate ASC")
    List<FieldVisit> findUpcomingVisitsByOfficer(@Param("officerId") Long officerId, @Param("today") LocalDate today);
    
    boolean existsByVisitNumber(String visitNumber);
}