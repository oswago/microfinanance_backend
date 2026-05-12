// repository/ForecastRepository.java
package com.microfinance.financials.budget.repository;

import com.microfinance.financials.budget.entity.Forecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ForecastRepository extends JpaRepository<Forecast, Long> {
    
    Optional<Forecast> findByForecastCode(String forecastCode);
    
    List<Forecast> findByCategoryOrderByForecastDateDesc(String category);
    
    List<Forecast> findByForecastDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT f FROM Forecast f WHERE f.forecastPeriodEnd >= :date ORDER BY f.forecastDate DESC")
    List<Forecast> findActiveForecasts(@Param("date") LocalDate date);
    
    @Query("SELECT AVG(f.forecastAccuracy) FROM Forecast f WHERE f.category = :category")
    Double getAverageAccuracyByCategory(@Param("category") String category);
}