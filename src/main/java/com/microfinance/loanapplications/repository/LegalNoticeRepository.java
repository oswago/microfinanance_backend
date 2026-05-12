// repository/LegalNoticeRepository.java
package com.microfinance.loanapplications.repository;

import com.microfinance.loanapplications.entity.LegalNotice;
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
public interface LegalNoticeRepository extends JpaRepository<LegalNotice, Long> {
    
    List<LegalNotice> findByLoan(Loan loan);
    
    List<LegalNotice> findByRecoveryCase(RecoveryCase recoveryCase);
    
    Page<LegalNotice> findByLoanId(Long loanId, Pageable pageable);
    
    Page<LegalNotice> findByRecoveryCaseId(Long recoveryCaseId, Pageable pageable);
    
    Page<LegalNotice> findByAssignedOfficerId(Long officerId, Pageable pageable);
    
    Page<LegalNotice> findByStatus(String status, Pageable pageable);
    
    @Query("SELECT n FROM LegalNotice n WHERE " +
           "(:loanId IS NULL OR n.loan.id = :loanId) AND " +
           "(:recoveryCaseId IS NULL OR n.recoveryCase.id = :recoveryCaseId) AND " +
           "(:assignedOfficerId IS NULL OR n.assignedOfficer.id = :assignedOfficerId) AND " +
           "(:noticeType IS NULL OR n.noticeType = :noticeType) AND " +
           "(:status IS NULL OR n.status = :status) AND " +
           "(:fromDate IS NULL OR n.noticeDate >= :fromDate) AND " +
           "(:toDate IS NULL OR n.noticeDate <= :toDate)")
    Page<LegalNotice> findAllWithFilters(@Param("loanId") Long loanId,
                                         @Param("recoveryCaseId") Long recoveryCaseId,
                                         @Param("assignedOfficerId") Long assignedOfficerId,
                                         @Param("noticeType") String noticeType,
                                         @Param("status") String status,
                                         @Param("fromDate") LocalDate fromDate,
                                         @Param("toDate") LocalDate toDate,
                                         Pageable pageable);
    
    @Query("SELECT n FROM LegalNotice n WHERE n.complianceDate <= :date AND n.status = 'SENT'")
    List<LegalNotice> findNoticesDueForCompliance(@Param("date") LocalDate date);
    
    @Query("SELECT COUNT(n) FROM LegalNotice n WHERE n.status = :status")
    long countByStatus(@Param("status") String status);
    
    @Query("SELECT n.noticeType, COUNT(n) FROM LegalNotice n GROUP BY n.noticeType")
    List<Object[]> countByNoticeType();
    
    boolean existsByNoticeNumber(String noticeNumber);
}