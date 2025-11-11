package com.microfinance.system.repository;

import com.microfinance.system.entity.HolidayCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendar, Long> {
    
    /**
     * Find holidays by date
     */
    List<HolidayCalendar> findByHolidayDate(LocalDate holidayDate);
    
    /**
     * Find active holidays by date
     */
    List<HolidayCalendar> findByHolidayDateAndActiveTrue(LocalDate holidayDate);
    
    /**
     * Check if holiday exists for a specific date
     */
    boolean existsByHolidayDate(LocalDate holidayDate);
    
    /**
     * Check if active holiday exists for a specific date
     */
    boolean existsByHolidayDateAndActiveTrue(LocalDate holidayDate);
    
    /**
     * Find holidays within a date range
     */
    List<HolidayCalendar> findByHolidayDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find active holidays within a date range
     */
    List<HolidayCalendar> findByHolidayDateBetweenAndActiveTrue(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find recurring holidays
     */
    List<HolidayCalendar> findByRecurringTrue();
    
    /**
     * Find active recurring holidays
     */
    List<HolidayCalendar> findByRecurringTrueAndActiveTrue();
    
    /**
     * Find holidays by country code
     */
    List<HolidayCalendar> findByCountryCode(String countryCode);
    
    /**
     * Find active holidays by country code
     */
    List<HolidayCalendar> findByCountryCodeAndActiveTrue(String countryCode);
    
    /**
     * Find holidays by name containing (for search)
     */
    List<HolidayCalendar> findByNameContainingIgnoreCase(String name);
    
    /**
     * Find all active holidays
     */
    List<HolidayCalendar> findByActiveTrue();
    
    /**
     * Find holidays by year
     */
    @Query("SELECT h FROM HolidayCalendar h WHERE YEAR(h.holidayDate) = :year")
    List<HolidayCalendar> findByYear(@Param("year") int year);
    
    /**
     * Find active holidays by year
     */
    @Query("SELECT h FROM HolidayCalendar h WHERE YEAR(h.holidayDate) = :year AND h.active = true")
    List<HolidayCalendar> findByYearAndActiveTrue(@Param("year") int year);
    
    /**
     * Find holidays by month and year
     */
    @Query("SELECT h FROM HolidayCalendar h WHERE YEAR(h.holidayDate) = :year AND MONTH(h.holidayDate) = :month")
    List<HolidayCalendar> findByMonthAndYear(@Param("month") int month, @Param("year") int year);
    
    /**
     * Check if date is a holiday
     */
    default boolean isHoliday(LocalDate date) {
        return existsByHolidayDateAndActiveTrue(date);
    }
    
    /**
     * Get next holiday after a specific date
     */
    @Query("SELECT h FROM HolidayCalendar h WHERE h.holidayDate > :date AND h.active = true ORDER BY h.holidayDate ASC LIMIT 1")
    Optional<HolidayCalendar> findNextHoliday(@Param("date") LocalDate date);
}