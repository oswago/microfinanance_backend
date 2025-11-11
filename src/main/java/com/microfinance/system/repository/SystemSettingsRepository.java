package com.microfinance.system.repository;

import com.microfinance.system.entity.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {
    
    /**
     * Find system settings by ID (typically only one record exists)
     */
    Optional<SystemSettings> findById(Long id);
    
    /**
     * Get the first system settings record (since there's typically only one)
     */
    @Query("SELECT s FROM SystemSettings s ORDER BY s.id ASC LIMIT 1")
    Optional<SystemSettings> findFirst();
    
    /**
     * Check if system settings exist
     */
    boolean existsById(Long id);
}