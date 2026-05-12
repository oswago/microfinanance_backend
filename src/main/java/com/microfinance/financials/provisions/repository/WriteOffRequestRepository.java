// repository/WriteOffRequestRepository.java
package com.microfinance.financials.provisions.repository;

import com.microfinance.financials.provisions.entity.WriteOffRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WriteOffRequestRepository extends JpaRepository<WriteOffRequest, Long> {
    
    Optional<WriteOffRequest> findByRequestNumber(String requestNumber);
    
    Page<WriteOffRequest> findByStatus(String status, Pageable pageable);
    
    List<WriteOffRequest> findByLoanId(Long loanId);
    
    @Query("SELECT wo FROM WriteOffRequest wo WHERE wo.status = 'PENDING' AND wo.requestDate <= :date")
    List<WriteOffRequest> findPendingRequestsOlderThan(@Param("date") LocalDate date);
    
    @Query("SELECT wo FROM WriteOffRequest wo WHERE wo.status IN ('APPROVED', 'COMPLETED') AND wo.writeOffDate BETWEEN :startDate AND :endDate")
    List<WriteOffRequest> findCompletedWriteOffsBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT wo FROM WriteOffRequest wo WHERE wo.status = 'COMPLETED' ORDER BY wo.writeOffDate DESC")
    List<WriteOffRequest> findCompletedWriteOffs();
}