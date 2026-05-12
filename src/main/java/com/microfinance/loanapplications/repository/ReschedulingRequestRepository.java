// ReschedulingRequestRepository.java
package com.microfinance.loanapplications.repository;

import com.microfinance.loanapplications.entity.ReschedulingRequest;
import com.microfinance.loanapplications.entity.ReschedulingRequest.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReschedulingRequestRepository extends JpaRepository<ReschedulingRequest, Long> {
    
    Optional<ReschedulingRequest> findByRequestNumber(String requestNumber);
    
    List<ReschedulingRequest> findByLoanId(Long loanId);
    
    List<ReschedulingRequest> findByBorrowerId(Long borrowerId);
    
    Page<ReschedulingRequest> findByStatus(RequestStatus status, Pageable pageable);
    
    @Query("SELECT r FROM ReschedulingRequest r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:branchId IS NULL OR r.loan.branch.id = :branchId) AND " +
           "(:startDate IS NULL OR DATE(r.requestDate) >= :startDate) AND " +
           "(:endDate IS NULL OR DATE(r.requestDate) <= :endDate)")
    Page<ReschedulingRequest> findWithFilters(
            @Param("status") RequestStatus status,
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);
    
    @Query("SELECT COUNT(r) FROM ReschedulingRequest r WHERE r.status = :status")
    long countByStatus(@Param("status") RequestStatus status);
    
    @Query("SELECT COUNT(r) FROM ReschedulingRequest r WHERE " +
           "r.requestDate BETWEEN :startDate AND :endDate")
    long countByDateRange(@Param("startDate") LocalDateTime startDate, 
                          @Param("endDate") LocalDateTime endDate);


    @Query(value = "SELECT AVG(DATEDIFF('SECOND', request_date, review_date) / 86400.0) " +
            "FROM rescheduling_requests " +
            "WHERE review_date IS NOT NULL " +
            "AND request_date BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    Double getAverageProcessingTime(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);


    
    @Query("SELECT r FROM ReschedulingRequest r WHERE r.loan.id = :loanId " +
           "ORDER BY r.requestDate DESC")
    List<ReschedulingRequest> findHistoryByLoanId(@Param("loanId") Long loanId, Pageable pageable);

    // Count pending reschedule requests
    default int countPendingRescheduleRequests() {
        return (int) countByStatus(RequestStatus.PENDING);
    }


    // Count by status and branch
    @Query("SELECT COUNT(r) FROM ReschedulingRequest r WHERE r.status = :status AND r.loan.branch.id = :branchId")
    int countByStatusAndBranch(@Param("status") ReschedulingRequest.RequestStatus status,
                               @Param("branchId") Long branchId);


    default int countPendingRescheduleRequestsByBranch(Long branchId) {
        return countByStatusAndBranch(ReschedulingRequest.RequestStatus.PENDING, branchId);
    }

    // Count approved this month
    @Query("SELECT COUNT(r) FROM ReschedulingRequest r WHERE r.status = 'APPROVED' AND r.approvedDate BETWEEN :start AND :end")
    int countApprovedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(r) FROM ReschedulingRequest r WHERE r.status = 'APPROVED' AND r.approvedDate BETWEEN :start AND :end AND r.loan.branch.id = :branchId")
    int countApprovedBetweenAndBranch(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("branchId") Long branchId);

    // Get recent rescheduling activities
    @Query("SELECT r FROM ReschedulingRequest r ORDER BY r.createdAt DESC")
    List<ReschedulingRequest> findTopByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT r FROM ReschedulingRequest r WHERE r.loan.branch.id = :branchId ORDER BY r.createdAt DESC")
    List<ReschedulingRequest> findTopByBranchIdOrderByCreatedAtDesc(@Param("branchId") Long branchId, Pageable pageable);

}



