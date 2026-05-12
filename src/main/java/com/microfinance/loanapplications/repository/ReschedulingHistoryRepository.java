// ReschedulingHistoryRepository.java
package com.microfinance.loanapplications.repository;

import com.microfinance.loanapplications.entity.ReschedulingHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReschedulingHistoryRepository extends JpaRepository<ReschedulingHistory, Long> {
    
    List<ReschedulingHistory> findByLoanIdOrderByPerformedAtDesc(Long loanId);
    
    @Query("SELECT h FROM ReschedulingHistory h WHERE h.loan.id = :loanId " +
           "ORDER BY h.performedAt DESC")
    List<ReschedulingHistory> findRecentHistory(@Param("loanId") Long loanId, Pageable pageable);
}