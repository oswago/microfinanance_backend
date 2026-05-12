// repository/FinancialPeriodRepository.java
package com.microfinance.financials.generalledger.repository;

import com.microfinance.financials.generalledger.entity.FinancialPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialPeriodRepository extends JpaRepository<FinancialPeriod, Long> {
    
    /**
     * Find financial period by year and month
     * @param year The year (e.g., 2024)
     * @param month The month (1-12)
     * @return Optional containing the financial period if found
     */
    Optional<FinancialPeriod> findByYearAndMonth(Integer year, Integer month);
    
    /**
     * Find financial period by period name
     * @param periodName The period name (e.g., "January 2024")
     * @return Optional containing the financial period if found
     */
    Optional<FinancialPeriod> findByPeriodName(String periodName);
    
    /**
     * Check if a financial period exists for the given year and month
     * @param year The year
     * @param month The month
     * @return true if exists, false otherwise
     */
    boolean existsByYearAndMonth(Integer year, Integer month);
    
    /**
     * Find all financial periods for a specific year, ordered by month
     * @param year The year
     * @return List of financial periods for that year
     */
    List<FinancialPeriod> findByYearOrderByMonthAsc(Integer year);
    
    /**
     * Find all financial periods with a specific status
     * @param status The status (OPEN, CLOSED, LOCKED)
     * @return List of financial periods with the given status
     */
    List<FinancialPeriod> findByStatus(String status);
    
    /**
     * Find all financial periods ordered by year descending and month descending
     * @return List of all financial periods
     */
    List<FinancialPeriod> findAllByOrderByYearDescMonthDesc();
    
    /**
     * Find the current/open financial period based on current date
     * @param currentDate The current date
     * @return Optional containing the current open financial period
     */
    @Query("SELECT fp FROM FinancialPeriod fp WHERE :currentDate BETWEEN fp.startDate AND fp.endDate AND fp.status = 'OPEN'")
    Optional<FinancialPeriod> findCurrentOpenPeriod(@Param("currentDate") LocalDate currentDate);
    
    /**
     * Find the most recent closed financial period
     * @return Optional containing the most recent closed period
     */
    @Query("SELECT fp FROM FinancialPeriod fp WHERE fp.status = 'CLOSED' ORDER BY fp.year DESC, fp.month DESC")
    Optional<FinancialPeriod> findMostRecentClosedPeriod();
    
    /**
     * Find financial periods within a date range
     * @param startDate Start date
     * @param endDate End date
     * @return List of financial periods covering the date range
     */
    @Query("SELECT fp FROM FinancialPeriod fp WHERE fp.startDate <= :endDate AND fp.endDate >= :startDate ORDER BY fp.year, fp.month")
    List<FinancialPeriod> findPeriodsInDateRange(@Param("startDate") LocalDate startDate, 
                                                  @Param("endDate") LocalDate endDate);
    
    /**
     * Find all open financial periods
     * @return List of open financial periods
     */
    @Query("SELECT fp FROM FinancialPeriod fp WHERE fp.status = 'OPEN' ORDER BY fp.year, fp.month")
    List<FinancialPeriod> findOpenPeriods();
    
    /**
     * Find all closed financial periods
     * @return List of closed financial periods
     */
    @Query("SELECT fp FROM FinancialPeriod fp WHERE fp.status = 'CLOSED' ORDER BY fp.year DESC, fp.month DESC")
    List<FinancialPeriod> findClosedPeriods();
    
    /**
     * Get the next financial period after the given one
     * @param year The year
     * @param month The month
     * @return Optional containing the next period
     */
    @Query("SELECT fp FROM FinancialPeriod fp WHERE (fp.year = :year AND fp.month > :month) OR fp.year > :year ORDER BY fp.year, fp.month")
    Optional<FinancialPeriod> findNextPeriod(@Param("year") Integer year, @Param("month") Integer month);
    
    /**
     * Get the previous financial period before the given one
     * @param year The year
     * @param month The month
     * @return Optional containing the previous period
     */
    @Query("SELECT fp FROM FinancialPeriod fp WHERE (fp.year = :year AND fp.month < :month) OR fp.year < :year ORDER BY fp.year DESC, fp.month DESC")
    Optional<FinancialPeriod> findPreviousPeriod(@Param("year") Integer year, @Param("month") Integer month);
    
    /**
     * Count financial periods by status
     * @param status The status
     * @return Count of periods with the given status
     */
    @Query("SELECT COUNT(fp) FROM FinancialPeriod fp WHERE fp.status = :status")
    long countByStatus(@Param("status") String status);

    
    /**
     * Find periods that are overdue for closing (end date has passed but still open)
     * @param currentDate The current date
     * @return List of overdue open periods
     */
    @Query("SELECT fp FROM FinancialPeriod fp WHERE fp.endDate < :currentDate AND fp.status = 'OPEN'")
    List<FinancialPeriod> findOverdueOpenPeriods(@Param("currentDate") LocalDate currentDate);
    
    /**
     * Find the financial period for a specific date
     * @param date The date
     * @return Optional containing the financial period that contains the date
     */
    @Query("SELECT fp FROM FinancialPeriod fp WHERE :date BETWEEN fp.startDate AND fp.endDate")
    Optional<FinancialPeriod> findByDate(@Param("date") LocalDate date);

    List<FinancialPeriod> findByYear(Integer year);



    @Query("SELECT fp FROM FinancialPeriod fp WHERE fp.status = 'LOCKED' ORDER BY fp.year DESC, fp.month DESC")
    List<FinancialPeriod> findLockedPeriods();


    @Query("SELECT fp.year, COUNT(fp), " +
            "SUM(CASE WHEN fp.status = 'CLOSED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN fp.status = 'OPEN' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN fp.status = 'LOCKED' THEN 1 ELSE 0 END) " +
            "FROM FinancialPeriod fp " +
            "GROUP BY fp.year " +
            "ORDER BY fp.year DESC")
    List<Object[]> getPeriodSummaryByYear();

    @Query("SELECT fp FROM FinancialPeriod fp WHERE fp.year = :year AND fp.month = :month")
    Optional<FinancialPeriod> findByYearAndMonthNative(@Param("year") Integer year, @Param("month") Integer month);

}