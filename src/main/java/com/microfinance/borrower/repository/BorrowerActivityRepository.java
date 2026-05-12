package com.microfinance.borrower.repository;

import com.microfinance.borrower.entity.BorrowerActivity;
import com.microfinance.common.config.GeneralConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BorrowerActivityRepository extends JpaRepository<BorrowerActivity, Long> {

    Page<BorrowerActivity> findByBorrowerId(Long borrowerId, Pageable pageable);
    
    List<BorrowerActivity> findByBorrowerIdOrderByActivityDateDesc(Long borrowerId);
    
    List<BorrowerActivity> findByBorrowerIdAndActivityDateAfterOrderByActivityDateDesc(
            Long borrowerId, LocalDateTime activityDate);
    
    Page<BorrowerActivity> findByBorrowerIdAndActivityType(
            Long borrowerId, GeneralConfig. BorrowerActivityType activityType, Pageable pageable);
    
    @Query("SELECT ba FROM BorrowerActivity ba WHERE ba.borrower.id = :borrowerId AND " +
           "ba.activityDate BETWEEN :startDate AND :endDate ORDER BY ba.activityDate DESC")
    List<BorrowerActivity> findByBorrowerIdAndActivityDateBetween(
            @Param("borrowerId") Long borrowerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT ba.activityType, COUNT(ba) FROM BorrowerActivity ba " +
           "WHERE ba.borrower.id = :borrowerId AND ba.activityDate BETWEEN :startDate AND :endDate " +
           "GROUP BY ba.activityType")
    List<Object[]> getActivityStatistics(
            @Param("borrowerId") Long borrowerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT ba FROM BorrowerActivity ba WHERE ba.borrower.id = :borrowerId " +
           "AND ba.activityType IN :activityTypes ORDER BY ba.activityDate DESC")
    List<BorrowerActivity> findByBorrowerIdAndActivityTypeIn(
            @Param("borrowerId") Long borrowerId,
            @Param("activityTypes") List<GeneralConfig.BorrowerActivityType> activityTypes);
    
    Long countByBorrowerId(Long borrowerId);
    
    @Query("SELECT MAX(ba.activityDate) FROM BorrowerActivity ba WHERE ba.borrower.id = :borrowerId")
    LocalDateTime findLastActivityDateByBorrowerId(@Param("borrowerId") Long borrowerId);

    @Query("SELECT ba FROM BorrowerActivity ba WHERE ba.borrower.id = :borrowerId ORDER BY ba.activityDate DESC")
    List<BorrowerActivity> findRecentByBorrowerId(@Param("borrowerId") Long borrowerId, Pageable pageable);


    @Query("SELECT ba FROM BorrowerActivity ba WHERE ba.borrower.id = :borrowerId AND ba.activityDate BETWEEN :start AND :end")
    Page<BorrowerActivity> findByBorrowerIdAndActivityDateBetween(
            @Param("borrowerId") Long borrowerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @Query("SELECT ba.activityType, COUNT(ba) FROM BorrowerActivity ba WHERE ba.borrower.id = :borrowerId AND ba.activityDate BETWEEN :start AND :end GROUP BY ba.activityType")
    List<Object[]> countByBorrowerIdAndActivityTypeBetween(
            @Param("borrowerId") Long borrowerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(ba) FROM BorrowerActivity ba WHERE ba.borrower.id = :borrowerId AND ba.activityDate BETWEEN :start AND :end")
    long countByBorrowerIdAndActivityDateBetween(
            @Param("borrowerId") Long borrowerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT ba FROM BorrowerActivity ba WHERE ba.borrower.id = :borrowerId AND ba.activityDate BETWEEN :start AND :end ORDER BY ba.activityDate DESC")
    List<BorrowerActivity> findByBorrowerIdAndActivityDateBetweenOrderByActivityDateDesc(
            @Param("borrowerId") Long borrowerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    // In BorrowerActivityRepository.java

    @Query("SELECT a FROM BorrowerActivity a WHERE a.borrower.id = :borrowerId " +
            "AND a.activityType IN :activityTypes " +
            "AND a.activityDate BETWEEN :start AND :end " +
            "ORDER BY a.activityDate DESC")
    List<BorrowerActivity> findByBorrowerIdAndActivityTypeInAndActivityDateBetween(
            @Param("borrowerId") Long borrowerId,
            @Param("activityTypes") List<String> activityTypes,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);


}