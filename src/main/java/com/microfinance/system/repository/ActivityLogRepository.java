// src/main/java/com/microfinance/system/repository/ActivityLogRepository.java
package com.microfinance.system.repository;

import com.microfinance.system.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findByBorrowerId(Long borrowerId, Pageable pageable);
    
    List<ActivityLog> findByBorrowerIdOrderByActivityDateDesc(Long borrowerId);
    
    Page<ActivityLog> findByPerformedBy(Long performedBy, Pageable pageable);
    
    Page<ActivityLog> findByActivityType(String activityType, Pageable pageable);
    
    @Query("SELECT al FROM ActivityLog al WHERE al.activityDate BETWEEN :startDate AND :endDate")
    Page<ActivityLog> findByActivityDateBetween(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate, 
                                              Pageable pageable);
    
    @Query("SELECT al FROM ActivityLog al WHERE al.borrowerId = :borrowerId AND al.activityType = :activityType")
    List<ActivityLog> findByBorrowerIdAndActivityType(@Param("borrowerId") Long borrowerId, 
                                                     @Param("activityType") String activityType);
    
    @Query("SELECT COUNT(al) FROM ActivityLog al WHERE al.borrowerId = :borrowerId")
    Long countByBorrowerId(@Param("borrowerId") Long borrowerId);
    
    @Query("SELECT al FROM ActivityLog al WHERE al.groupId = :groupId ORDER BY al.activityDate DESC")
    Page<ActivityLog> findByGroupId(@Param("groupId") Long groupId, Pageable pageable);
}